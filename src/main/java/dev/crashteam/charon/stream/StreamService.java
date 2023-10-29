package dev.crashteam.charon.stream;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.stream.AwsStreamMessage;
import dev.crashteam.charon.publisher.AwsStreamMessagePublisher;
import dev.crashteam.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamService {

    private final AwsStreamMessagePublisher awsStreamMessagePublisher;
    private final ProtoMapper protoMapper;
    private final RetryTemplate retryTemplate;

    @Value("${app.aws-stream.uzum-stream.name}")
    private String awsStreamName;

    public PutRecordsResult publishPaymentStatusChangeAwsMessage(Payment payment) {
        try {
            retryTemplate.execute((RetryCallback<PutRecordsResult, Exception>) retryContext -> {
                PutRecordsRequestEntry awsMessagePaymentCreatedEntry = getAwsMessagePaymentStatusChangeEntry(payment);
                if (awsMessagePaymentCreatedEntry != null) {
                    log.info("Publishing status change payment - {}", payment.getPaymentId());
                    return awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName,
                            Collections.singletonList(awsMessagePaymentCreatedEntry)));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId());
        }
        return null;
    }

    public PutRecordsResult publishPaymentCreatedAwsMessage(Payment payment) {
        try {
            retryTemplate.execute((RetryCallback<PutRecordsResult, Exception>) retryContext -> {
                PutRecordsRequestEntry awsMessagePaymentCreatedEntry = getAwsMessagePaymentCreatedEntry(payment);
                if (awsMessagePaymentCreatedEntry != null) {
                    log.info("Publishing created payment - {}", payment.getPaymentId());
                    return awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName,
                            Collections.singletonList(awsMessagePaymentCreatedEntry)));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId());
        }
        return null;
    }

    private PutRecordsRequestEntry getAwsMessagePaymentCreatedEntry(Payment payment) {
        try {
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
