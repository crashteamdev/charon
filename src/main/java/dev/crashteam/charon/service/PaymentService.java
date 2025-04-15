package dev.crashteam.charon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.charon.config.PromoCodeConfig;
import dev.crashteam.charon.exception.DuplicateTransactionException;
import dev.crashteam.charon.exception.NoSuchPaymentTypeException;
import dev.crashteam.charon.exception.NoSuchSubscriptionTypeException;
import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.Currency;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.*;
import dev.crashteam.charon.model.domain.PaidService;
import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.publisher.handler.StreamPublisherHandler;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.specification.PaymentSpecification;
import dev.crashteam.charon.resolver.PaymentResolver;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.charon.util.PromoCodeGenerator;
import dev.crashteam.payment.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaidServiceService paidServiceService;
    private final PromoCodeService promoCodeService;
    private final UserService userService;
    private final OperationTypeService operationTypeService;
    private final ProtoMapper protoMapper;
    private final CurrencyService currencyService;
    private final StreamPublisherHandler publisherHandler;
    private final List<PaymentResolver> paymentResolvers;
    private final UserSavedPaymentService savedPaymentService;

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        try {
            return switch (request.getPaymentCase()) {
                case PAYMENT_DEPOSIT_USER_BALANCE -> createBalanceDepositPayment(request);
                case PAYMENT_PURCHASE_SERVICE -> createPurchaseServicePayment(request);
                case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
            };
        } catch (Exception e) {
            log.error("Exception while creating payment ", e);
            return PaymentCreateResponse.newBuilder()
                    .setStatus(PaymentStatus.PAYMENT_STATUS_FAILED)
                    .setDescription(e.getMessage())
                    .build();
        }
    }

    public RecurrentPaymentCancelResponse cancelRecurrentPayment(RecurrentPaymentCancelRequest request) {
        savedPaymentService.cancelRecurrentPayment(request.getUserId());
        return RecurrentPaymentCancelResponse.newBuilder()
                .setUserId(request.getUserId())
                .build();
    }

    public GetExchangeRateResponse getExchangeRate(GetExchangeRateRequest request) {
        String currency = request.getCurrency();
        BigDecimal exchangeRate = currencyService.getExchangeRate(currency);
        return GetExchangeRateResponse.newBuilder()
                .setPair("USD_" + currency)
                .setExchangeRate(String.valueOf(exchangeRate))
                .build();
    }

    @Transactional
    public GetBalanceResponse getBalanceResponse(GetBalanceRequest request) {
        User user = userService.getUser(request.getUserId());
        return protoMapper.getBalanceResponse(user);
    }

    public CheckPromoCodeResponse checkPromoCode(CheckPromoCodeRequest request) {
        PromoCode promoCode = promoCodeService.getPromoCode(request.getPromoCode());
        return protoMapper.getCheckPromoCodeResponse(promoCode);
    }

    @Transactional
    public PurchaseServiceResponse purchaseService(PurchaseServiceRequest request) {
        log.info("Trying to purchase service from balance by user - {}", request.getUserId());
        try {
            if (paymentRepository.findByOperationId(request.getOperationId()).isPresent())
                throw new DuplicateTransactionException("Transaction with operation id %s already exists"
                        .formatted(request.getOperationId()));

            User user = userService.getUser(request.getUserId());

            long amount;
            long multiply;

            Payment payment = new Payment();
            if (request.hasPaidService()) {
                PaidServiceContext context = request.getPaidService().getContext();
                PaidService paidService = getPaidServiceFromContext(context);
                log.info("Purchasing service - {} by user - {}", paidService.getName(), user.getId());
                multiply = context.getMultiply() == 0 ? 1 : context.getMultiply();
                long multipliedAmount = paidService.getAmount() * multiply;
                amount = PaymentProtoUtils.multiplyDiscount(multipliedAmount, multiply, paidService.getSubscriptionType());
                payment.setPaidService(paidService);
            } else {
                List<PaidService> paidServices = getPaidServiceFromContext(request.getPaidServicesList());
                log.info("Purchasing service - {} by user - {}", paidServices.stream().map(PaidService::getName)
                        .collect(Collectors.joining(", ")), user.getId());

                multiply = request.getMultiply() == 0 ? 1 : request.getMultiply();

                amount = paidServices.stream().map(it -> {
                    long multipliedAmount = it.getAmount() * multiply;
                    return PaymentProtoUtils.multiplyDiscount(multipliedAmount, multiply, it.getSubscriptionType());
                }).mapToLong(Long::longValue).sum();
                payment.setPaidServices(new HashSet<>(paidServices));
            }

            long balanceAfterPurchase = user.getBalance() - amount;
            if (balanceAfterPurchase < 0) {
                return protoMapper.getErrorPurchaseServiceResponse(user.getBalance());
            }

            String paymentId = UUID.randomUUID().toString();
            payment.setPaymentId(paymentId);
            payment.setOperationId(request.getOperationId());
            payment.setCurrency(Currency.RUB.getTitle());
            payment.setAmount(amount);
            payment.setCreated(LocalDateTime.now());
            payment.setUpdated(LocalDateTime.now());
            payment.setMonthPaid(multiply);
            payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
            payment.setStatus(RequestPaymentStatus.SUCCESS);
            user.setBalance(balanceAfterPurchase);

            if (user.getSubscriptionValidUntil() == null) {
                user.setSubscriptionValidUntil(LocalDateTime.now().plusMonths(payment.getMonthPaid()));
            } else {
                if (user.getSubscriptionValidUntil().isBefore(LocalDateTime.now())) {
                    user.setSubscriptionValidUntil(LocalDateTime.now().plusMonths(payment.getMonthPaid()));
                } else {
                    LocalDateTime plusMonths = user.getSubscriptionValidUntil().plusMonths(payment.getMonthPaid());
                    user.setSubscriptionValidUntil(plusMonths);
                }
            }

            User saveUser = userService.saveUser(user);
            payment.setUser(saveUser);

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Saving payment with paymentId - {}", paymentId);

            publisherHandler.publishPaymentCreatedMessage(savedPayment); // Status Event отправлять не требуется, платеж уже в статусе SUCCESS

            return protoMapper.getPurchaseServiceResponse(savedPayment, user.getBalance());
        } catch (Exception e) {
            log.error("Exception while trying to purchase service from balance", e);
            PurchaseServiceResponse.ErrorResponse errorResponse = PurchaseServiceResponse.ErrorResponse.newBuilder()
                    .setErrorCode(PurchaseServiceResponse.ErrorResponse.ErrorCode.ERROR_CODE_UNKNOWN)
                    .setDescription(e.getMessage())
                    .build();
            return PurchaseServiceResponse.newBuilder().setErrorResponse(errorResponse).build();
        }
    }

    public CreatePromoCodeResponse createPromoCode(CreatePromoCodeRequest request) {
        PromoCodeConfig codeConfig = new PromoCodeConfig(request.getSerializedSize(), null, request.getPrefix(), null);
        String promoCodeValue = PromoCodeGenerator.getPromoCode(codeConfig);
        PromoCode promoCode = new PromoCode();
        promoCode.setCode(promoCodeValue);
        promoCode.setDescription(request.getDescription());
        promoCode.setUsageLimit((long) request.getUsageLimit());
        LocalDateTime validUntil = LocalDateTime
                .ofEpochSecond(request.getValidUntil().getSeconds(), request.getValidUntil().getNanos(), ZoneOffset.UTC);
        promoCode.setValidUntil(validUntil);
        //TODO: Добавить поддержку разных контекстов
        promoCode.setDiscountPercentage(request.getPromoCodeContext().getDiscountPromocodeContext().getDiscountPercentage());
        promoCodeService.save(promoCode);
        return protoMapper.getPromoCodeResponse(promoCode);
    }

    @Transactional(readOnly = true)
    public UserPayment getUserPaymentByPaymentId(PaymentQuery request) {
        Payment payment = paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(EntityNotFoundException::new);
        return protoMapper.getUserPayment(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentByPendingStatusAndOperationType(String operationType) {
        return paymentRepository.findAllByPendingStatusAndOperationType(operationType);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentByPendingStatusAndOperationTypeBetweenTimeRange(String operationType) {
        return paymentRepository.findAllByPendingStatusAndOperationTypeAndCreatedBetween(operationType,
                LocalDateTime.now().minusDays(1), LocalDateTime.now());
    }

    @Deprecated
    @Transactional
    public Payment refundPayment(YkPaymentRefundResponseDTO refundResponse, String userId, String id) {
        Payment payment = paymentRepository.findByPaymentId(id).orElseThrow(EntityNotFoundException::new);
        payment.setPaymentId(id);
        payment.setExternalId(refundResponse.getId());
        //payment.setStatus(refundResponse.getStatus());
        payment.setCurrency(refundResponse.getAmount().getCurrency());
        payment.setAmount(Double.valueOf(refundResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(refundResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentCreateResponse createPurchaseServicePayment(PaymentCreateRequest request) throws JsonProcessingException {
        PaymentCreateRequest.PaymentPurchaseService purchaseService = request.getPaymentPurchaseService();
        log.info("Processing service purchase request for user - {}. Promo code - {}", purchaseService.getUserId(),
                StringUtils.hasText(purchaseService.getPromoCode()) ? purchaseService.getPromoCode() : "");
        PaymentResolver paymentResolver = paymentResolvers.stream().filter(it -> it.getPaymentSystem()
                        .equals(purchaseService.getPaymentSystem()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);

        PromoCode promoCode = StringUtils.hasText(purchaseService.getPromoCode())
                ? promoCodeService.getPromoCode(purchaseService.getPromoCode()) : null;

        User user = userService.getUser(purchaseService.getUserId());

        long amount;
        long multiply;

        Payment payment = new Payment();

        if (purchaseService.hasPaidService()) {
            PaidServiceContext context = purchaseService.getPaidService().getContext();
            PaidService paidService = getPaidServiceFromContext(context);
            log.info("Purchasing service - {} by user - {}", paidService.getName(), user.getId());
            multiply = context.getMultiply() == 0 ? 1 : context.getMultiply();
            long multipliedAmount = paidService.getAmount() * multiply;
            amount = PaymentProtoUtils.multiplyDiscount(multipliedAmount, multiply, paidService.getSubscriptionType());
            payment.setPaidService(paidService);
        } else {
            List<PaidService> paidServices = getPaidServiceFromContext(purchaseService.getPaidServicesList());
            log.info("Purchasing service - {} by user - {}", paidServices.stream().map(PaidService::getName)
                    .collect(Collectors.joining(", ")), purchaseService.getUserId());
            multiply = purchaseService.getMultiply() == 0 ? 1 : purchaseService.getMultiply();
            amount = paidServices.stream().map(it -> {
                long multipliedAmount = it.getAmount() * multiply;
                return PaymentProtoUtils.multiplyDiscount(multipliedAmount, multiply, it.getSubscriptionType());
            }).mapToLong(Long::longValue).sum();
            payment.setPaidServices(new HashSet<>(paidServices));
        }

        if (paymentResolver.getPaymentSystem().equals(PaymentSystem.PAYMENT_SYSTEM_CLICK)) {
            long increaseAmount = (amount * 10) / 100;
            amount += increaseAmount;
        }

        if (promoCodeValidAndUnusedByUser(promoCode, user.getId())) {
            long discount = (long) (amount * ((double) promoCode.getDiscountPercentage() / 100));
            amount = amount - discount;
            log.info("Using promocode={} by user={}, new amount={}", promoCode.getCode(), user.getId(), amount);
            payment.setPromoCode(promoCode);
        }
        BigDecimal moneyAmount = PaymentProtoUtils.getMajorMoneyAmount(amount);
        PaymentData response = paymentResolver.createPayment(request, String.valueOf(moneyAmount));

        ObjectMapper objectMapper = new ObjectMapper();
        payment.setPaymentId(response.getPaymentId());
        payment.setExternalId(response.getProviderId());
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency(Currency.RUB.getTitle());
        payment.setAmount(amount);
        payment.setProviderAmount(Long.valueOf(response.getProviderAmount()));
        payment.setProviderCurrency(response.getProviderCurrency());
        payment.setUser(user);
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
        payment.setMonthPaid(multiply);
        payment.setEmail(response.getEmail());
        payment.setPhone(response.getPhone());
        payment.setPaymentSystem(protoMapper.getPaymentSystemType(purchaseService.getPaymentSystem()).getTitle());
        payment.setMetadata(objectMapper.writeValueAsString(request.getMetadataMap()));
        payment.setExchangeRate(response.getExchangeRate());
        paymentRepository.save(payment);
        log.info("Saving payment with paymentId - {}", response.getPaymentId());

        publisherHandler.publishPaymentCreatedMessage(payment);
        return protoMapper.getPaymentResponse(response, payment, amount);

    }

    @Transactional
    public PaymentCreateResponse createBalanceDepositPayment(PaymentCreateRequest request) throws JsonProcessingException {
        PaymentCreateRequest.PaymentDepositUserBalance balanceRequest = request.getPaymentDepositUserBalance();
        PaymentResolver paymentResolver = paymentResolvers.stream().filter(it -> it.getPaymentSystem()
                        .equals(balanceRequest.getPaymentSystem()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        String paymentSystemTitle = protoMapper.getPaymentSystemType(balanceRequest.getPaymentSystem()).getTitle();
        log.info("Processing balance deposit request for user - {} with payment system - {}", balanceRequest.getUserId(), paymentSystemTitle);
        long balanceRequestAmount = balanceRequest.getAmount();
        if (paymentResolver.getPaymentSystem().equals(PaymentSystem.PAYMENT_SYSTEM_CLICK)) {
            long increaseAmount = (balanceRequestAmount * 10) / 100;
            balanceRequestAmount += increaseAmount;
        }
        BigDecimal actualAmount = PaymentProtoUtils.getMajorMoneyAmount(balanceRequestAmount);
        PaymentData response = paymentResolver.createPayment(request, String.valueOf(actualAmount));

        User user = userService.getUser(balanceRequest.getUserId());

        ObjectMapper objectMapper = new ObjectMapper();
        Payment payment = new Payment();
        payment.setPaymentId(response.getPaymentId());
        payment.setExternalId(response.getProviderId());
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency(Currency.RUB.getTitle());
        payment.setAmount(balanceRequest.getAmount());
        payment.setProviderAmount(Long.valueOf(response.getProviderAmount()));
        payment.setProviderCurrency(response.getProviderCurrency());
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setUser(user);
        payment.setEmail(response.getEmail());
        payment.setPhone(response.getPhone());
        payment.setOperationType(operationTypeService.getOperationType(Operation.DEPOSIT_BALANCE.getTitle()));
        payment.setPaymentSystem(paymentSystemTitle);
        payment.setMetadata(objectMapper.writeValueAsString(request.getMetadataMap()));
        payment.setExchangeRate(response.getExchangeRate());
        paymentRepository.save(payment);
        log.info("Saving payment with paymentId - {}", response.getPaymentId());

        publisherHandler.publishPaymentCreatedMessage(payment);

        return protoMapper.getPaymentResponse(response, payment);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(PaymentsQuery paymentsQuery) {
        LimitOffsetPagination pagination = paymentsQuery.getPagination();
        Pageable pageable = PageRequest.of((int) (pagination.getOffset()
                / pagination.getLimit()), (int) pagination.getLimit());
        return paymentRepository.findAll(new PaymentSpecification(paymentsQuery), pageable);
    }

    public List<PaidService> getPaidServiceFromContext(List<dev.crashteam.payment.PaidService> paidServicesList) {
        return paidServicesList.stream().map(dev.crashteam.payment.PaidService::getContext)
                .map(this::getPaidServiceFromContext)
                .collect(Collectors.toList());
    }

    public PaidService getPaidServiceFromContext(PaidServiceContext paidServiceContext) {
        var paidServiceContextType = paidServiceContext.getContextCase().getNumber();

        var subscriptionType = switch (paidServiceContext.getContextCase()) {
            case UZUM_ANALYTICS_CONTEXT ->
                    paidServiceContext.getUzumAnalyticsContext().getPlan().getPlanCase().getNumber();
            case KE_ANALYTICS_CONTEXT -> paidServiceContext.getKeAnalyticsContext().getPlan().getPlanCase().getNumber();
            case UZUM_REPRICER_CONTEXT ->
                    paidServiceContext.getUzumRepricerContext().getPlan().getPlanCase().getNumber();
            case KE_REPRICER_CONTEXT -> paidServiceContext.getKeRepricerContext().getPlan().getPlanCase().getNumber();
            case CONTEXT_NOT_SET -> throw new NoSuchSubscriptionTypeException();
        };
        return paidServiceService.getPaidServiceByTypeAndPlan((long) paidServiceContextType, (long) subscriptionType);
    }

    @Transactional(readOnly = true)
    public Payment findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId).orElse(null);
    }

    @Transactional
    public void save(Payment payment) {
        paymentRepository.save(payment);
    }


    @Transactional
    public void savePromoCodeRestrictions(PromoCode promoCode, User user) {
        if (promoCode == null) return;
        promoCode.setUsageLimit(promoCode.getUsageLimit() - 1);
        if (CollectionUtils.isEmpty(promoCode.getUsers())) {
            Set<User> users = new HashSet<>();
            users.add(user);
            promoCode.setUsers(users);
        } else {
            promoCode.getUsers().add(user);
        }
        promoCodeService.save(promoCode);
    }

    @Transactional(readOnly = true)
    public Long getOperationIdSeq() {
        return paymentRepository.operationIdSeq();
    }

    public Payment findByOperationId(String operationId) {
        return paymentRepository.findByOperationId(operationId).orElse(null);
    }

    private boolean promoCodeValidAndUnusedByUser(PromoCode promoCode, String userId) {
        return promoCode != null
                && !promoCodeService.existsByCodeAndUserId(promoCode.getCode(), userId)
                && LocalDateTime.now().isBefore(promoCode.getValidUntil())
                && promoCode.getUsageLimit() > 0;
    }
}
