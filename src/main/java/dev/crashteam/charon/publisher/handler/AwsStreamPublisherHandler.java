package dev.crashteam.charon.publisher.handler;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.charon.exception.MessagePublishException;
import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.stream.AwsStreamMessage;
import dev.crashteam.charon.publisher.AwsStreamMessagePublisher;
import dev.crashteam.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.stream.publisher", havingValue = "AWS")
public class AwsStreamPublisherHandler implements StreamPublisherHandler<PutRecordsResult> {

    private final AwsStreamMessagePublisher awsStreamMessagePublisher;
    private final ProtoMapper protoMapper;
    private final RetryTemplate retryTemplate;

    @Value("${app.aws-stream.uzum-stream.name}")
    private String awsStreamName;

    public PutRecordsResult publishPaymentStatusChangeMessage(Payment payment) {
        try {
            log.info("Publishing status change payment - {}", payment.getPaymentId());
            retryTemplate.execute((RetryCallback<PutRecordsResult, Exception>) retryContext -> {
                PutRecordsRequestEntry awsMessagePaymentCreatedEntry = getAwsMessagePaymentStatusChangeEntry(payment);
                if (awsMessagePaymentCreatedEntry != null) {
                    return awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName,
                            Collections.singletonList(awsMessagePaymentCreatedEntry)));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId(), e);
            throw new MessagePublishException(Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()));
        }
        return null;
    }

    public PutRecordsResult publishPaymentCreatedMessage(Payment payment) {
        try {
            log.info("Publishing created payment - {}", payment.getPaymentId());
            retryTemplate.execute((RetryCallback<PutRecordsResult, Exception>) retryContext -> {
                PutRecordsRequestEntry awsMessagePaymentCreatedEntry = getAwsMessagePaymentCreatedEntry(payment);
                if (awsMessagePaymentCreatedEntry != null) {
                    return awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName,
                            Collections.singletonList(awsMessagePaymentCreatedEntry)));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId(), e);
            throw new MessagePublishException(Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()));
        }
        return null;
    }

    private PutRecordsRequestEntry getAwsMessagePaymentCreatedEntry(Payment payment) {
        try {
            log.info("Creating AWS message for created paymentId - {} and userId - {}", payment.getPaymentId(),
                    Optional.ofNullable(payment.getUser()).map(User::getId).orElse(null));
            PaymentEvent createdPaymentEvent = protoMapper.getCreatedPaymentEvent(payment);
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setPartitionKey(payment.getPaymentId());
            requestEntry.setData(ByteBuffer.wrap(createdPaymentEvent.toByteArray()));
            return requestEntry;
        } catch (Exception ex) {
            log.error("Unexpected exception during publish AWS stream message returning null entry for payment - {}",
                    payment.getPaymentId(), ex);
        }
        return null;
    }

    private PutRecordsRequestEntry getAwsMessagePaymentStatusChangeEntry(Payment payment) {
        try {
            log.info("Creating AWS message for changed status of paymentId - {} and userId - {}", payment.getPaymentId(),
                    Optional.ofNullable(payment.getUser()).map(User::getId).orElse(null));
            PaymentEvent createdPaymentEvent = protoMapper.getPaymentStatusChangeEvent(payment);
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setPartitionKey(payment.getPaymentId());
            requestEntry.setData(ByteBuffer.wrap(createdPaymentEvent.toByteArray()));
            return requestEntry;
        } catch (Exception ex) {
            log.error("Unexpected exception during publish AWS stream message returning null entry for payment - {}",
                    payment.getPaymentId(), ex);
        }
        return null;
    }

}
