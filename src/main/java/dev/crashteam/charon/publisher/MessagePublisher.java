package dev.crashteam.charon.publisher;

import dev.crashteam.charon.model.stream.Message;

@FunctionalInterface
public interface MessagePublisher<T extends Message> {

    Object publish(T message);
}
