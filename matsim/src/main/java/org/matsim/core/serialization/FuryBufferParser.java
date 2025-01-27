package org.matsim.core.serialization;

import org.apache.fury.memory.MemoryBuffer;
import org.matsim.api.core.v01.Message;

import java.io.IOException;

@FunctionalInterface
public interface FuryBufferParser {

	Message parse(MemoryBuffer in) throws IOException;

}
