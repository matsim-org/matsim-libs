package org.matsim.core.serialization;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;
import org.apache.fory.memory.MemoryBuffer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.custom.AttributesSerializer;
import org.matsim.core.serialization.custom.IdSerializer;
import org.matsim.core.serialization.custom.IntArrayListSerializer;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.nio.ByteBuffer;

/**
 * Provides serialization and deserialization of messages.
 * <p>
 * The message type catalog (which classes exist and what their ids are) lives in {@link MessageTypeRegistry}; this class
 * only deals with turning those messages into bytes and back. It is only needed for runs that span multiple compute
 * nodes.
 */
public class SerializationProvider {

	private static final SerializationProvider INSTANCE = new SerializationProvider();

	public static SerializationProvider getInstance() {
		return INSTANCE;
	}

	private final MessageTypeRegistry registry = MessageTypeRegistry.getInstance();

	private final ThreadSafeFory fory;

	private SerializationProvider() {

		// Fory uses its own verbose logging. Disable this manually here, so users don't see the internals of how Fory compiles classes into
		// wire formats.
		org.apache.fory.logging.LoggerFactory.disableLogging();

		// multiple serializations of different objects.
		fory = Fory.builder().withLanguage(Language.JAVA)
			.withRefTracking(false)
			.withCodegen(true)
			.withMetaShare(false)
			.requireClassRegistration(false)
			.buildThreadSafeFory();

		// Manually register some allowed types
		fory.register(Coord.class);

		Class<?> idImpl;
		try {
			idImpl = SerializationProvider.class.getClassLoader().loadClass("org.matsim.api.core.v01.Id$IdImpl");
		} catch (ClassNotFoundException ignored) {
			throw new IllegalStateException("Id$IdImpl not found");
		}

		fory.registerSerializer(idImpl, IdSerializer.class);
		//fory.register(idImpl);

		fory.registerSerializer(IntArrayList.class, IntArrayListSerializer.class);
		fory.registerSerializer(AttributesImpl.class, AttributesSerializer.class);

		// Register all classes that are likely to be used as messages
		for (Class<? extends Message> msgClass : registry.messageClasses()) {
			fory.register(msgClass);
		}
	}

	/**
	 * Serialize message object and return byte array.
	 */
	public <T extends Message> byte[] toBytes(T msg) {
		return fory.serialize(msg);
	}

	/**
	 * Deserialize a message that was serialized using {@link #toBytes(Message)}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Message> T deserialize(ByteBuffer buf) {
		return (T) fory.deserialize(buf);
	}

	@SuppressWarnings("unchecked")
	public Message deserialize(MemoryBuffer in, int type) {
		var msgClass = registry.getType(type);
		if (msgClass == null) {
			throw new IllegalArgumentException("Type " + type + " was not registered for serialization. Messages that should be serialized must be at least package private to be detected.");
		}

		return deserialize(in, (Class<? extends Message>) msgClass);
	}

	public <T extends Message> T deserialize(MemoryBuffer in, Class<T> clazz) {
		return fory.deserialize(in, clazz);
	}

	public <T extends Message> byte[] serialize(T message) {
		if (!registry.isRegistered(message.getClass())) {
			throw new IllegalArgumentException("Class " + message.getClass() + " was not registered for serialization. Messages that should be serialized must be at least package private to be detected.");
		}
		try {
			return fory.serialize(message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "SerializationProvider{registry=" + registry + '}';
	}

	/**
	 * Return whether the given type is supported.
	 */
	public boolean hasType(int type) {
		return registry.hasType(type);
	}

	/**
	 * @see MessageTypeRegistry#getAssignableTypes(Class)
	 */
	public int[] getAssignableTypes(Class<?> clazz) {
		return registry.getAssignableTypes(clazz);
	}

	public int getType(Class<?> msgType) {
		return registry.getType(msgType);
	}

	public Class<?> getType(int type) {
		return registry.getType(type);
	}
}
