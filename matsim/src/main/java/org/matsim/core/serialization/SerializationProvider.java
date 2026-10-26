package org.matsim.core.serialization;

import org.matsim.api.core.v01.Message;

import java.nio.ByteBuffer;

/**
 * Turns {@link Message} objects into bytes and back for transfer between compute nodes. Only runs that span multiple
 * compute nodes need this; the implementation ({@code ForySerializationProvider}) and its Fory dependency live in the
 * {@code distributed-simulation} contrib. Single-node runs bind a {@link NoopSerializationProvider}.
 * <p>
 * The message type catalog is a separate concern, see {@link MessageTypeRegistry}.
 */
public interface SerializationProvider {

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
