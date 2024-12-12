package org.matsim.core.communication;


import org.matsim.core.mobsim.dsim.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

@FunctionalInterface
public interface MessageParser<T extends Message> {

	T parse(ByteBuffer in) throws IOException;

}
