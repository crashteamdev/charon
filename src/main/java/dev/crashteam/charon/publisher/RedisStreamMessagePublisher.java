package dev.crashteam.charon.publisher;

import dev.crashteam.charon.model.stream.RedisStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.stream.publisher", havingValue = "REDIS")
public class RedisStreamMessagePublisher implements MessagePublisher<RedisStreamMessage> {

    private final RedisStreamCommands streamCommands;

    @Override
    public RecordId publish(RedisStreamMessage message) {
        return streamCommands.xAdd(MapRecord.create(message.getTopic().getBytes(StandardCharsets.UTF_8),
                Collections.singletonMap(message.getMessageKey().getBytes(StandardCharsets.UTF_8),
                        message.getMessage())), RedisStreamCommands.XAddOptions.maxlen(message.getMaxLen()));

    }

}
