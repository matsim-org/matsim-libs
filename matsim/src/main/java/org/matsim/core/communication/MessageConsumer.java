package org.matsim.core.communication;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface MessageConsumer {

    /**
     * Consume the received data.
     */
    void consume(ByteBuffer data) throws IOException;

}
