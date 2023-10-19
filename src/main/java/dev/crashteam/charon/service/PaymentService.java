package dev.crashteam.charon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.charon.config.PromoCodeConfig;
import dev.crashteam.charon.exception.DuplicateTransactionException;
import dev.crashteam.charon.exception.NoSuchPaymentTypeException;
import dev.crashteam.charon.exception.NoSuchSubscriptionTypeException;
import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.dto.ninja.ExchangeRateDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.PaidService;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.specification.PaymentSpecification;
import dev.crashteam.charon.service.resolver.PaymentResolver;
import dev.crashteam.charon.util.PromoCodeGenerator;
import dev.crashteam.payment.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaidServiceService paidService;
    private final PromoCodeService promoCodeService;
    private final UserService userService;
    private final OperationTypeService operationTypeService;
    private final ProtoMapper protoMapper;
    private final CurrencyService currencyService;
    private final List<PaymentResolver> paymentResolvers;

    public GetExchangeRateResponse getExchangeRate(GetExchangeRateRequest request) {
        ExchangeRateDto exchangeRate = currencyService.getExchangeRate("USD_" + request.getCurrency());
        return GetExchangeRateResponse.newBuilder()
                .setPair(exchangeRate.getCurrencyPair())
                .setExchangeRate(exchangeRate.getExchangeRate())
                .build();
    }

    @Transactional(readOnly = true)
    public GetBalanceResponse getBalanceResponse(GetBalanceRequest request) {
        User user = userService.getUser(request.getUserId());
        return protoMapper.getBalanceResponse(user);
    }

    @Transactional
    public PurchaseServiceResponse purchaseService(PurchaseServiceRequest request) {
        if (paymentRepository.findByOperationId(request.getOperationId()).isPresent())
            throw new DuplicateTransactionException("Transaction with operation id %s already exists"
                    .formatted(request.getOperationId()));

        User user = userService.getUser(request.getUserId());
        PaidServiceContext context = request.getPaidService().getContext();
        PaidService paidService = getPaidServiceFromContext(context);
        long balanceAfterPurchase = user.getBalance() - paidService.getAmount();
        Payment payment = new Payment();
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setOperationId(request.getOperationId());
        payment.setCurrency("USD");
        payment.setAmount(paidService.getAmount());
        payment.setCreated(LocalDateTime.now());
        payment.setUpdated(LocalDateTime.now());
        payment.setMonthPaid(context.getMultiply());
        payment.setPaymentSystem("LK");
        if (balanceAfterPurchase < 0) {
            payment.setStatus(RequestPaymentStatus.FAILED.getTitle());
            payment.setUser(user);
            return protoMapper.getPurchaseServiceResponse(paymentRepository.save(payment), user.getBalance());
        }
        payment.setStatus(RequestPaymentStatus.SUCCESS.getTitle());
        user.setBalance(balanceAfterPurchase);
        payment.setUser(userService.saveUser(user));
        return protoMapper.getPurchaseServiceResponse(paymentRepository.save(payment), user.getBalance());
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
    public List<Payment> getPaymentByStatus(String status) {
        return paymentRepository.findAllByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Payment> getBPaymentByStatusAndOperationType(String status, String operationType) {
        return paymentRepository.findAllByStatusAndOperationType(status, operationType);
    }

    @Deprecated
    @Transactional
    public Payment refundPayment(YkPaymentRefundResponseDTO refundResponse, String userId, String id) {
        Payment payment = paymentRepository.findByPaymentId(id).orElseThrow(EntityNotFoundException::new);
        payment.setPaymentId(id);
        payment.setExternalId(refundResponse.getId());
        payment.setStatus(refundResponse.getStatus());
        payment.setCurrency(refundResponse.getAmount().getCurrency());
        payment.setAmount(Double.valueOf(refundResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(refundResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> createBalanceDepositPayment(request);
            case PAYMENT_PURCHASE_SERVICE -> createPurchaseServicePayment(request);
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException();
        };
    }

    @Transactional
    @SneakyThrows
    public PaymentCreateResponse createPurchaseServicePayment(PaymentCreateRequest request) {
        PaymentCreateRequest.PaymentPurchaseService purchaseService = request.getPaymentPurchaseService();
        PaymentResolver paymentResolver = paymentResolvers.stream().filter(it -> it.getPaymentSystem()
                        .equals(purchaseService.getPaymentSystem()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);

        PromoCode promoCode = StringUtils.hasText(purchaseService.getPromoCode())
                ? promoCodeService.getPromoCode(purchaseService.getPromoCode()) : null;

        PaidServiceContext paidServiceContext = purchaseService.getPaidService().getContext();
        PaidService servicePlan = getPaidServiceFromContext(paidServiceContext);

        PaymentData response = paymentResolver
                .createPayment(request, String.valueOf(servicePlan.getAmount()));

        User user = getUser(purchaseService.getUserId());

        ObjectMapper objectMapper = new ObjectMapper();
        Payment payment = new Payment();
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setExternalId(response.getId());
        payment.setStatus(RequestPaymentStatus.PENDING.getTitle());
        payment.setCurrency("USD");
        payment.setAmount(servicePlan.getAmount());
        payment.setUser(userService.saveUser(user));
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setUser(user);
        payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
        payment.setPromoCode(promoCode);
        payment.setMonthPaid(paidServiceContext.getMultiply());
        payment.setPaymentSystem(protoMapper.getPaymentSystemType(purchaseService.getPaymentSystem()).getTitle());
        payment.setMetadata(objectMapper.writeValueAsString(request.getMetadataMap()));

        paymentRepository.save(payment);
        return protoMapper.getPaymentResponse(response);

    }

    @Transactional
    @SneakyThrows
    public PaymentCreateResponse createBalanceDepositPayment(PaymentCreateRequest request) {
        PaymentCreateRequest.PaymentDepositUserBalance balanceRequest = request.getPaymentDepositUserBalance();
        PaymentResolver paymentResolver = paymentResolvers.stream().filter(it -> it.getPaymentSystem()
                        .equals(balanceRequest.getPaymentSystem()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        PaymentData response = paymentResolver.createPayment(request, String.valueOf(balanceRequest.getAmount()));

        User user = getUser(balanceRequest.getUserId());

        ObjectMapper objectMapper = new ObjectMapper();
        Payment payment = new Payment();
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setExternalId(response.getId());
        payment.setStatus(RequestPaymentStatus.PENDING.getTitle());
        payment.setCurrency("USD");
        payment.setAmount(balanceRequest.getAmount());
        payment.setUser(userService.saveUser(user));
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setUser(user);
        payment.setOperationType(operationTypeService.getOperationType(Operation.DEPOSIT_BALANCE.getTitle()));
        payment.setPaymentSystem(protoMapper.getPaymentSystemType(balanceRequest.getPaymentSystem()).getTitle());
        payment.setMetadata(objectMapper.writeValueAsString(request.getMetadataMap()));

        paymentRepository.save(payment);
        return protoMapper.getPaymentResponse(response);
    }

    private User getUser(String id) {
        if (userService.userExists(id)) {
            return userService.getUser(id);
        }
        log.info("Creating new user with id - {}", id);
        User user = new User();
        user.setId(id);
        user.setBalance(0L);
        user.setCurrency("USD");
        return user;
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(PaymentsQuery paymentsQuery) {
        LimitOffsetPagination pagination = paymentsQuery.getPagination();
        Pageable pageable = PageRequest.of((int) (pagination.getOffset()
                / pagination.getLimit()), (int) pagination.getLimit());
        return paymentRepository.findAll(new PaymentSpecification(paymentsQuery), pageable);
    }

    public PaidService getPaidServiceFromContext(PaidServiceContext paidServiceContext) {
        var paidServiceContextType = paidServiceContext.getContextCase().getNumber();

        var subscriptionType = switch (paidServiceContext.getContextCase()) {
            case UZUM_ANALYTICS_CONTEXT ->
                    paidServiceContext.getUzumAnalyticsContext().getPlan().getPlanCase().getNumber();
            case KE_ANALYTICS_CONTEXT -> paidServiceContext.getKeAnalyticsContext().getPlan().getPlanCase().getNumber();
            case UZUM_REPRICER_CONTEXT ->
                    paidServiceContext.getUzumRepricerContext().getPlan().getPlanCase().getNumber();
            case CONTEXT_NOT_SET -> throw new NoSuchSubscriptionTypeException();
        };
        return paidService
                .getPaidServiceByTypeAndPlan(String.valueOf(paidServiceContextType), String.valueOf(subscriptionType));
    }

    @Transactional
    public void save(Payment payment) {
        paymentRepository.save(payment);
    }
}
