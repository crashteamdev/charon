package dev.crashteam.charon.publisher.handler;

import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.stream.RedisStreamMessage;
import dev.crashteam.charon.publisher.RedisStreamMessagePublisher;
import dev.crashteam.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.stream.publisher", havingValue = "REDIS")
public class RedisStreamPublisherHandler implements StreamPublisherHandler<RecordId> {

    private final RedisStreamMessagePublisher messagePublisher;
    private final RetryTemplate retryTemplate;
    private final ProtoMapper protoMapper;

    @Value("${app.redis.stream.key}")
    public String streamKey;

    @Value("${app.redis.stream.maxlen}")
    public Long maxlen;

    @Value("${app.redis.stream.waitPending}")
    public Long waitPending;

    @Override
    public RecordId publishPaymentStatusChangeMessage(Payment payment) {
        try {
            retryTemplate.execute((RetryCallback<RecordId, Exception>) retryContext -> {
                PaymentEvent paymentEvent = getMessagePaymentStatusChangeEntry(payment);
                if (paymentEvent != null) {
                    log.info("Publishing status change payment - {}", payment.getPaymentId());
                    return messagePublisher
                            .publish(new RedisStreamMessage(streamKey, payment, maxlen, "payment", waitPending));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId(), e);
        }
        return null;
    }

    @Override
    public RecordId publishPaymentCreatedMessage(Payment payment) {
        try {
            retryTemplate.execute((RetryCallback<RecordId, Exception>) retryContext -> {
                PaymentEvent paymentEvent = getMessagePaymentCreatedEntry(payment);
                if (paymentEvent != null) {
                    log.info("Publishing create payment event - {}", payment.getPaymentId());
                    return messagePublisher
                            .publish(new RedisStreamMessage(streamKey, payment, maxlen, "payment", waitPending));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error while trying to publish payment - {}", payment.getPaymentId(), e);
        }
        return null;
    }

    private PaymentEvent getMessagePaymentCreatedEntry(Payment payment) {
        try {
            log.info("Creating REDIS message for created paymentId - {} and userId - {}", payment.getPaymentId(),
                    Optional.ofNullable(payment.getUser()).map(User::getId).orElse(null));
            return protoMapper.getCreatedPaymentEvent(payment);
        } catch (Exception ex) {
            log.error("Unexpected exception during publish REDIS stream message returning null entry for payment - {}",
                    payment.getPaymentId(), ex);
        }
        return null;
    }

    private PaymentEvent getMessagePaymentStatusChangeEntry(Payment payment) {
        try {
            log.info("Creating REDIS message for changed status of paymentId - {} and userId - {}", payment.getPaymentId(),
                    Optional.ofNullable(payment.getUser()).map(User::getId).orElse(null));
            return protoMapper.getPaymentStatusChangeEvent(payment);
        } catch (Exception ex) {
            log.error("Unexpected exception during publish REDIS stream message returning null entry for payment - {}",
                    payment.getPaymentId(), ex);
        }
        return null;
    }
}
