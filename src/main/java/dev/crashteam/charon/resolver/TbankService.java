package dev.crashteam.charon.resolver;

import dev.crashteam.charon.mapper.integration.TbankPaymentMapper;
import dev.crashteam.charon.model.Currency;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.domain.UserSavedPayment;
import dev.crashteam.charon.model.dto.UserSavedPaymentResolverDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.dto.tbank.*;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.service.OperationTypeService;
import dev.crashteam.charon.service.UserSavedPaymentService;
import dev.crashteam.charon.service.UserService;
import dev.crashteam.charon.service.feign.TBankClient;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TbankService implements PaymentResolver {

    private final TBankClient tBankClient;
    private final TbankPaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final OperationTypeService operationTypeService;
    private final UserSavedPaymentService savedPaymentService;

    @Value("${app.integration.tinkoff.secretKey}")
    private String secretKey;
    @Value("${app.integration.tinkoff.shopId}")
    private String shopId;

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_TBANK;
    }

    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(amount);
        String paymentId = UUID.randomUUID().toString();
        InitRequestDTO requestDTO = paymentMapper.getPaymentRequestDTO(request,
                shopId,
                secretKey,
                paymentId,
                moneyAmount.longValue());
        InitResponseDTO responseDTO = tBankClient.init(requestDTO);

        String phone = PaymentProtoUtils.getPhoneFromRequest(request);
        String email = PaymentProtoUtils.getEmailFromRequest(request);

        PaymentData paymentData = new PaymentData();
        paymentData.setPhone(phone);
        paymentData.setEmail(email);
        paymentData.setPaymentId(paymentId);
        paymentData.setProviderId(responseDTO.paymentId());
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        paymentData.setProviderAmount(String.valueOf(responseDTO.amount()));
        paymentData.setConfirmationUrl(responseDTO.paymentURL());
        return paymentData;
    }

    @Override
    public PaymentData recurrentPayment(UserSavedPaymentResolverDto savedPaymentDto) {
        long amount = PaymentProtoUtils.getMinorMoneyAmount(savedPaymentDto.getAmount()).longValue();
        InitRequestDTO request = paymentMapper
                .getPaymentRecurrentRequestDTO(shopId, secretKey, savedPaymentDto.getPaymentId(), amount);

        InitResponseDTO response = tBankClient.init(request);

        User user = userService.getUser(savedPaymentDto.getUserId());

        String paymentId = UUID.randomUUID().toString();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setExternalId(response.paymentId());
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency(Currency.RUB.getTitle());
        payment.setAmount(amount);
        payment.setProviderAmount(Long.valueOf(response.amount()));
        payment.setProviderCurrency(Currency.RUB.getTitle());
        payment.setUser(user);
        payment.setCreated(LocalDateTime.now());
        payment.setUpdated(LocalDateTime.now());
        payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
        payment.setMonthPaid(savedPaymentDto.getMonthPaid());
        payment.setPaymentSystem(PaymentSystemType.PAYMENT_SYSTEM_TBANK.getTitle());
        paymentRepository.save(payment);

        ChargeRequestDTO chargeRequestDTO = paymentMapper.getChargeRequestDTO(shopId,
                secretKey,
                savedPaymentDto.getPaymentId(),
                response.paymentId());
        ChargeResponseDTO responseDTO = tBankClient.charge(chargeRequestDTO);

        PaymentData paymentData = new PaymentData();
        paymentData.setPaymentId(paymentId);
        paymentData.setProviderId(responseDTO.paymentId());
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(response.amount());
        paymentData.setProviderAmount(String.valueOf(moneyAmount));

        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {

        GetStateRequestDTO requestDTO = GetStateRequestDTO.builder()
                .paymentId(paymentId)
                .token(secretKey)
                .terminalKey(shopId)
                .build();
        GetStateResponseDTO state = tBankClient.getState(requestDTO);

        RequestPaymentStatus paymentStatus = paymentMapper.getPaymentStatus(state.status());
        if (StringUtils.hasText(state.rebillId()) && paymentStatus.equals(RequestPaymentStatus.SUCCESS)) {
            Optional<Payment> optionalPayment = paymentRepository.findByExternalId(paymentId);
            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                if (payment.getOperationType().getType().equals(Operation.PURCHASE_SERVICE.getTitle())) {
                    String userId = payment.getUser().getId();
                    UserSavedPayment userSavedPayment = savedPaymentService.findByUserId(userId);
                    if (userSavedPayment != null) {
                        userSavedPayment.setPaymentId(state.rebillId());
                        userSavedPayment.setPaidService(payment.getPaidService());
                        userSavedPayment.setMonthPaid(payment.getMonthPaid());
                        userSavedPayment.setPaymentSystem(PaymentSystemType.PAYMENT_SYSTEM_TBANK.getTitle());
                        savedPaymentService.save(userSavedPayment);
                    } else {
                        UserSavedPayment savedPayment = new UserSavedPayment();
                        savedPayment.setUserId(userId);
                        savedPayment.setPaymentId(state.rebillId());
                        savedPayment.setPaymentSystem(PaymentSystemType.PAYMENT_SYSTEM_TBANK.getTitle());
                        savedPayment.setPaidService(payment.getPaidService());
                        savedPayment.setMonthPaid(payment.getMonthPaid());
                        savedPaymentService.save(savedPayment);
                    }
                }
            }
        }
        return paymentStatus;
    }
}
