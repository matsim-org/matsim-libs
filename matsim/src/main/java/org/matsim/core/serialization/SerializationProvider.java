package org.matsim.core.serialization;

import org.matsim.api.core.v01.Message;

import java.nio.ByteBuffer;

/**
 * Turns {@link Message} objects into bytes and back for transfer between compute nodes.
 * <p>
 * This is only needed for simulation runs that actually span multiple compute nodes. Single-node runs (including
 * multi-threaded ones) never serialize any message and get a {@link NoopSerializationProvider}. The wire format
 * implementation ({@code ForySerializationProvider}) and its dependency on Fory live in the {@code distributed-simulation}
 * contrib.
 * <p>
 * The message type catalog (which classes exist and what their ids are) is independent of the wire format and lives in
 * {@link MessageTypeRegistry}.
 */
public interface SerializationProvider {

	/**
	 * Transitional accessor. Will be removed once all call sites obtain the provider through Guice.
	 */
	static SerializationProvider getInstance() {
		return ForySerializationProvider.getInstance();
	}

	/**
	 * Serialize a message and return its byte representation.
	 */
	<T extends Message> byte[] toBytes(T msg);

	/**
	 * Serialize a message that is known to the {@link MessageTypeRegistry}, wrapping any failure in an unchecked
	 * exception.
	 */
	<T extends Message> byte[] serialize(T message);

	/**
	 * Deserialize a self-describing message, advancing {@code buf}'s position past the consumed bytes.
	 */
	<T extends Message> T deserialize(ByteBuffer buf);

	/**
	 * Deserialize a message of the given type id, advancing {@code buf}'s position past the consumed bytes.
	 */
	Message deserialize(ByteBuffer buf, int type);

	/**
	 * Deserialize a message of the given class, advancing {@code buf}'s position past the consumed bytes.
	 */
	<T extends Message> T deserialize(ByteBuffer buf, Class<T> clazz);
}
