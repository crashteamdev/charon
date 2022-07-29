package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.yookassa.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "yookassaClient", url = "${app.integration.yookassa}")
public interface YookassaClient {

    @PostMapping("/payments")
    PaymentResponseDTO createPayment(@RequestBody PaymentCreateRequestDTO paymentCreateRequestDTO);

    @PostMapping("/payments/{payment_id}/cancel")
    PaymentCancelResponseDTO cancelPayment(@PathVariable("payment_id") String paymentId);

    @PostMapping("/refunds")
    PaymentRefundResponseDTO refund(@RequestBody PaymentRefundRequestDTO paymentRefundRequestDTO);

    @GetMapping("/refunds/{refund_id}")
    PaymentRefundResponseDTO refundStatus(@PathVariable("refund_id") String refundId);

    @GetMapping("/payments/{payment_id}")
    PaymentResponseDTO paymentStatus(@PathVariable("payment_id") String paymentId);

    @PostMapping("payments/{payment_id}/capture")
    PaymentResponseDTO capturePayment(@PathVariable("payment_id") String paymentId);

}
