package org.matsim.core.communication;

import java.lang.foreign.MemorySegment;

/**
 * Envelope is a simple container used for sending to multiple recipients.
 */
public record Envelope(int receiver, MemorySegment data, long offset, long length) {

    public Envelope(int receiver, MemorySegment data) {
        this(receiver, data, 0, data.byteSize());
    }
}


