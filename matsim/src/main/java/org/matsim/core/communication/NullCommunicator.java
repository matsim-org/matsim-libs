package org.matsim.core.communication;

import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.SerializationProvider;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Communicator for a single-node run. Every collective operation resolves locally and never touches the
 * {@link SerializationProvider}, so a single-node run needs no wire codec on the classpath.
 */
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
	public <T extends Message> void send(int toRank, T msg, SerializationProvider provider) {
	}

	@Override
	public <T extends Message> List<T> allGather(T msg, int tag, SerializationProvider provider) {
		List<T> result = new ArrayList<>(1);
		result.add(msg);
		return result;
	}

	@Override
	public <T extends Message> List<T> allToAll(Map<Integer, T> msgsToRanks, SerializationProvider provider) {
		return new ArrayList<>();
	}

	@Override
	public <T extends Message> void gatherTo(int toRank, T msg, SerializationProvider provider) {
	}

	@Override
	public <T extends Message> List<T> gatherFromAll(Class<T> msgClass, SerializationProvider provider) {
		return new ArrayList<>();
	}
}
