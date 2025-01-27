package org.matsim.core.serialization;


import org.matsim.api.core.v01.Message;

import java.io.IOException;

@FunctionalInterface
public interface ByteMessageParser {

	Message parse(byte[] data) throws IOException;

}
