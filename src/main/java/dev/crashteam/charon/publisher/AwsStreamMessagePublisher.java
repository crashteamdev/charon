package dev.crashteam.charon.publisher;

import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.charon.stream.AwsStreamClient;
import dev.crashteam.charon.model.stream.AwsStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AwsStreamMessagePublisher implements MessagePublisher<AwsStreamMessage> {

    private final AwsStreamClient awsStreamClient;

    @SneakyThrows
    @Override
    public PutRecordsResult publish(AwsStreamMessage message) {
        return awsStreamClient.sendMessage(
                message.getTopic(),
                message.getMessage()
        );
    }
}
