package org.matsim.core.communication;

import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.SerializationProvider;

import java.lang.foreign.MemorySegment;
import java.util.List;

public class NullCommunicator implements Communicator {

	@Override
	public int getRank() {
		return 0;
	}

	@Override
	public int getSize() {
		return 1;
	}

	@Override
	public void send(int receiver, MemorySegment data, long offset, long length) {
	}

	@Override
	public void recv(MessageReceiver expectsNext, MessageConsumer handleMsg) {
	}

	@Override
	public <T extends Message> List<T> allGather(T msg, int tag, SerializationProvider provider) {
		return Communicator.super.allGather(msg, tag, provider);
	}
}
