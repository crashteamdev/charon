package dev.crashteam.charon.publisher;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.charon.model.stream.AwsStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.stream.publisher", havingValue = "AWS")
public class AwsStreamMessagePublisher implements MessagePublisher<AwsStreamMessage> {

    private final AmazonKinesis amazonKinesis;

    @Override
    public PutRecordsResult publish(AwsStreamMessage message) {
        return this.sendMessage(
                message.getTopic(),
                message.getMessage()
        );
    }

    private PutRecordsResult sendMessage(String streamName, List<PutRecordsRequestEntry> entries) {
        PutRecordsRequest createRecordsRequest = new PutRecordsRequest();
        createRecordsRequest.setStreamName(streamName);
        createRecordsRequest.setRecords(entries);
        return amazonKinesis.putRecords(createRecordsRequest);
    }
}
