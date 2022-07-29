package dev.crashteam.charon.service;

import dev.crashteam.charon.mapper.YookassaPaymentMapper;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.yookassa.PaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.PaymentResponseDTO;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.specification.PaymentSpecification;
import dev.crashteam.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final YookassaPaymentMapper paymentMapper;

    public UserPayment getUserPaymentByPaymentId(PaymentQuery request) {
        Payment payment = getPaymentByPaymentId(request);
        return paymentMapper.getUserPayment(payment);
    }

    public List<Payment> getPaymentByStatus(String status) {
        return paymentRepository.findAllByStatus(status);
    }

    public Payment saveFromRefundResponse(PaymentRefundResponseDTO refundResponse, String userId, String id) {

        Payment payment = paymentRepository.findByPaymentId(id).orElseThrow(EntityNotFoundException::new);
        payment.setPaymentId(id);
        payment.setExternalId(refundResponse.getId());
        payment.setStatus(refundResponse.getStatus());
        payment.setCurrency(refundResponse.getAmount().getCurrency());
        payment.setValue(Double.valueOf(refundResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(refundResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return savePayment(payment);
    }

    public Payment saveFromPaymentResponse(PaymentResponseDTO paymentResponse, String userId, String id) {
        Payment payment = new Payment();
        payment.setPaymentId(id);
        payment.setExternalId(paymentResponse.getId());
        payment.setStatus(paymentResponse.getStatus());
        payment.setCurrency(paymentResponse.getAmount().getCurrency());
        payment.setValue(Double.valueOf(paymentResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(paymentResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return savePayment(payment);
    }

    public Payment saveFromRecurrentPaymentResponse(PaymentResponseDTO paymentResponse, String userId, String id) {
        Payment payment = new Payment();
        payment.setPaymentId(id);
        payment.setExternalId(paymentResponse.getId());
        payment.setStatus(paymentResponse.getStatus());
        payment.setCurrency(paymentResponse.getAmount().getCurrency());
        payment.setValue(Double.valueOf(paymentResponse.getAmount().getValue()).longValue());
        payment.setUserId(userId);
        payment.setCreated(paymentResponse.getCreatedAt());
        payment.setUpdated(LocalDateTime.now());
        return savePayment(payment);
    }

    private Payment savePayment(Payment history) {
        return paymentRepository.save(history);
    }

    public Page<Payment> getPayments(PaymentsQuery paymentsQuery) {
        LimitOffsetPagination pagination = paymentsQuery.getPagination();
        Pageable pageable = PageRequest.of((int) (pagination.getOffset()
                / pagination.getLimit()), (int) pagination.getLimit());
        return paymentRepository.findAll(new PaymentSpecification(paymentsQuery), pageable);
    }

    private Payment getPaymentByPaymentId(PaymentQuery request) {
        return paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(EntityNotFoundException::new);
    }
}
