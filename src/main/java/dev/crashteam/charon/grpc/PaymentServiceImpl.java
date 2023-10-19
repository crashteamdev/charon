package dev.crashteam.charon.grpc;

import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.payment.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class PaymentServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final PaymentService paymentService;
    private final ProtoMapper protoMapper;

    @Override
    public void getExchangeRate(GetExchangeRateRequest request, StreamObserver<GetExchangeRateResponse> responseObserver) {
        responseObserver.onNext(paymentService.getExchangeRate(request));
        responseObserver.onCompleted();
    }

    @Override
    public void createPromoCode(CreatePromoCodeRequest request, StreamObserver<CreatePromoCodeResponse> responseObserver) {
        responseObserver.onNext(paymentService.createPromoCode(request));
        responseObserver.onCompleted();
    }

    @Override
    public void checkPromoCode(CheckPromoCodeRequest request, StreamObserver<CheckPromoCodeResponse> responseObserver) {
        super.checkPromoCode(request, responseObserver);
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        responseObserver.onNext(paymentService.getBalanceResponse(request));
        responseObserver.onCompleted();
    }

    @Override
    public void purchaseService(PurchaseServiceRequest request, StreamObserver<PurchaseServiceResponse> responseObserver) {
       responseObserver.onNext(paymentService.purchaseService(request));
       responseObserver.onCompleted();
    }

    @Override
    public void createPayment(PaymentCreateRequest request, StreamObserver<PaymentCreateResponse> responseObserver) {
        var payment = paymentService.createPayment(request);
        responseObserver.onNext(payment);
        responseObserver.onCompleted();
    }

    @Override
    public void getPayments(PaymentsQuery request, StreamObserver<PaymentsResponse> responseObserver) {
        var payments = paymentService.getPayments(request);
        List<UserPayment> userPayments = protoMapper.getUserPaymentResponse(payments.getContent());
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
        var userPayment = paymentService.getUserPaymentByPaymentId(request);
        PaymentResponse paymentResponse = PaymentResponse.newBuilder()
                .setUserPayment(userPayment)
                .build();
        responseObserver.onNext(paymentResponse);
        responseObserver.onCompleted();
    }

}
