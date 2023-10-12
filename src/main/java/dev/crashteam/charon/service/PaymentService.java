package dev.crashteam.charon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.mapper.integration.PaymentMapper;
import dev.crashteam.charon.model.PaymentData;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentResponseDTO;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.specification.PaymentSpecification;
import dev.crashteam.charon.service.resolver.PaymentResolver;
import dev.crashteam.charon.util.PaymentProtoUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaidServiceService paidServiceService;
    private final PromoCodeService promoCodeService;
    private final UserService userService;
    private final ProtoMapper protoMapper;
    private final List<PaymentResolver> paymentResolvers;

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

    @Deprecated
    @Transactional
    public Payment refundPayment(YkPaymentRefundResponseDTO refundResponse, String userId, String id) {
        Payment payment = paymentRepository.findByPaymentId(id).orElseThrow(EntityNotFoundException::new);
        payment.setPaymentId(id);
        payment.setExternalId(refundResponse.getId());
        payment.setStatus(refundResponse.getStatus());
        payment.setCurrency(refundResponse.getAmount().getCurrency());
        payment.setValue(Double.valueOf(refundResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(refundResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> createBalanceDepositPayment(request);
            case PAYMENT_PURCHASE_SERVICE -> null;
            case PAYMENT_NOT_SET -> null;
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

        PaidServiceContext context = purchaseService.getPaidService().getContext();
        //TODO: Написать для PaidService все виды подписок и их планы(default и тд), написать фабрику где будет матчить по типу объекта

        PaymentData response = paymentResolver.createPayment(request);

        User user = getUser(purchaseService.getUserId());

        ObjectMapper objectMapper = new ObjectMapper();
        Payment payment = new Payment();
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setExternalId(response.getId());
        payment.setStatus(RequestPaymentStatus.PENDING.getTitle());
        payment.setCurrency(balanceRequest.getAmount().getCurrency());
        payment.setValue(balanceRequest.getAmount().getValue());
        payment.setUser(userService.saveUser(user));
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setUser(user);
        payment.setPromoCode(promoCode);
        payment.setMonthPaid(context.getMultiply());
        payment.setPaymentSystem(purchaseService.getPaymentSystem().toString());
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
        PaymentData response = paymentResolver
                .createPayment(request, balanceRequest.getAmount().getCurrency(), String.valueOf(balanceRequest.getAmount().getValue()));

        User user = getUser(balanceRequest.getUserId());

        ObjectMapper objectMapper = new ObjectMapper();
        Payment payment = new Payment();
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setExternalId(response.getId());
        payment.setStatus(RequestPaymentStatus.PENDING.getTitle());
        payment.setCurrency(balanceRequest.getAmount().getCurrency());
        payment.setValue(balanceRequest.getAmount().getValue());
        payment.setUser(userService.saveUser(user));
        payment.setCreated(response.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        payment.setUser(user);
        payment.setPaymentSystem(balanceRequest.getPaymentSystem().toString());
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

    @Transactional
    public Payment createPayment(YkPaymentResponseDTO paymentResponse,
                                 String userId, String id, Map<String, String> metaData) {
        Payment payment = new Payment();
        payment.setPaymentId(id);
        payment.setExternalId(paymentResponse.getId());
        payment.setStatus(paymentResponse.getStatus());
        payment.setCurrency(paymentResponse.getAmount().getCurrency());
        payment.setValue(Double.valueOf(paymentResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(paymentResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOperationId(String operationId) {
        return paymentRepository.findByOperationId(operationId).orElseThrow(EntityNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(PaymentsQuery paymentsQuery) {
        LimitOffsetPagination pagination = paymentsQuery.getPagination();
        Pageable pageable = PageRequest.of((int) (pagination.getOffset()
                / pagination.getLimit()), (int) pagination.getLimit());
        return paymentRepository.findAll(new PaymentSpecification(paymentsQuery), pageable);
    }
}
