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
    YkPaymentResponseDTO createPayment(@RequestBody YkPaymentCreateRequestDTO ykPaymentCreateRequestDTO);

    @PostMapping("/payments/{payment_id}/cancel")
    YkPaymentCancelResponseDTO cancelPayment(@PathVariable("payment_id") String paymentId);

    @PostMapping("/refunds")
    YkPaymentRefundResponseDTO refund(@RequestBody YkPaymentRefundRequestDTO ykPaymentRefundRequestDTO);

    @GetMapping("/refunds/{refund_id}")
    YkPaymentRefundResponseDTO refundStatus(@PathVariable("refund_id") String refundId);

    @GetMapping("/payments/{payment_id}")
    YkPaymentResponseDTO paymentStatus(@PathVariable("payment_id") String paymentId);

    @PostMapping("payments/{payment_id}/capture")
    YkPaymentResponseDTO capturePayment(@PathVariable("payment_id") String paymentId);

}
