package org.matsim.core.serialization;


import org.matsim.core.mobsim.dsim.Message;

import java.io.IOException;

@FunctionalInterface
public interface ByteMessageParser {

	Message parse(byte[] data) throws IOException;

}
