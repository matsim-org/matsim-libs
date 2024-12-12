package org.matsim.core.serialization;

import org.apache.fury.memory.MemoryBuffer;
import org.matsim.core.mobsim.dsim.Message;

import java.io.IOException;

@FunctionalInterface
public interface FuryBufferParser {

	Message parse(MemoryBuffer in) throws IOException;

}
