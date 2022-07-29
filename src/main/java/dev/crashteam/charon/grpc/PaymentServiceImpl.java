package dev.crashteam.charon.grpc;

import dev.crashteam.charon.mapper.YookassaPaymentMapper;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.YookassaService;
import dev.crashteam.payment.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class PaymentServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final YookassaService paymentService;
    private final PaymentService historyService;
    private final YookassaPaymentMapper paymentMapper;

    @Override
    public void createPayment(PaymentCreateRequest request, StreamObserver<PaymentCreateResponse> responseObserver) {
        var payment = paymentService.createPayment(request);
        responseObserver.onNext(payment);
        responseObserver.onCompleted();
    }

    @Override
    public void getPayments(PaymentsQuery request, StreamObserver<PaymentsResponse> responseObserver) {
        var payments = historyService.getPayments(request);
        List<UserPayment> userPayments = paymentMapper.getUserPaymentResponse(payments.getContent());
        LimitOffsetPaginationResult paginationResult = LimitOffsetPaginationResult.newBuilder()
                .setLimit(request.getPagination().getLimit())
                .setOffset(request.getPagination().getOffset())
                .setTotalOffset(payments.getTotalElements())
                .build();

        PaymentsResponse paymentsResponse = PaymentsResponse.newBuilder()
                .setPagination(paginationResult)
                .addAllUserPayment(userPayments)
                .build();

        responseObserver.onNext(paymentsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void getPayment(PaymentQuery request, StreamObserver<PaymentResponse> responseObserver) {
        var userPayment = historyService.getUserPaymentByPaymentId(request);
        PaymentResponse paymentResponse = PaymentResponse.newBuilder()
                .setUserPayment(userPayment)
                .build();
        responseObserver.onNext(paymentResponse);
        responseObserver.onCompleted();
    }


    @Override
    public void createRecurrentPayment(RecurrentPaymentCreateRequest request, StreamObserver<PaymentRecurrentResponse> responseObserver) {
        var payment = paymentService.createRecurrentPayment(request);
        responseObserver.onNext(payment);
        responseObserver.onCompleted();
    }

    @Override
    public void refundPayment(PaymentRefundRequest request, StreamObserver<PaymentRefundResponse> responseObserver) {
        var payment = paymentService.refundPayment(request);
        responseObserver.onNext(payment);
        responseObserver.onCompleted();
    }
}
