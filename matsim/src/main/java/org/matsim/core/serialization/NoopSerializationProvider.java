package org.matsim.core.serialization;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;

import java.nio.ByteBuffer;

/**
 * {@link SerializationProvider} bound for single-node runs. Every method throws: reaching this code means a single-node
 * run attempted cross-node serialization, or the {@code distributed-simulation} contrib is missing from the classpath.
 */
public final class NoopSerializationProvider implements SerializationProvider {

	private static final String MESSAGE = "Cross-node message serialization requires the distributed-simulation contrib on the classpath.";

	@Inject
	public NoopSerializationProvider() {
	}

	@Override
	public <T extends Message> byte[] toBytes(T msg) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public <T extends Message> byte[] serialize(T message) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public <T extends Message> T deserialize(ByteBuffer buf) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Message deserialize(ByteBuffer buf, int type) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public <T extends Message> T deserialize(ByteBuffer buf, Class<T> clazz) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public String toString() {
		return "NoopSerializationProvider";
	}
}
