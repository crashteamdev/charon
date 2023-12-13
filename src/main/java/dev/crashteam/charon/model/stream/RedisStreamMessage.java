package dev.crashteam.charon.model.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedisStreamMessage extends Message<byte[]> {
    private long maxLen;
    private String messageKey;
    private Long waitPending;

    public RedisStreamMessage(String streamKey, byte[] message, long maxLen, String messageKey, Long waitPending) {
        super(streamKey, message);
        this.maxLen = maxLen;
        this.waitPending = waitPending;
        this.messageKey = messageKey;
    }
}
