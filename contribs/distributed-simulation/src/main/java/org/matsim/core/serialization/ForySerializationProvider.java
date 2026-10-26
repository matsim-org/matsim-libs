package org.matsim.core.serialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
 * Fory-backed {@link SerializationProvider}, used for runs that span multiple compute nodes.
 */
@Singleton
public class ForySerializationProvider implements SerializationProvider {

	private final MessageTypeRegistry registry;

	private final ThreadSafeFory fory;

	@Inject
	public ForySerializationProvider(MessageTypeRegistry registry) {

		this.registry = registry;

		// Silence Fory's own logging; its class-compilation chatter is noise to MATSim users.
		org.apache.fory.logging.LoggerFactory.disableLogging();

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
			idImpl = ForySerializationProvider.class.getClassLoader().loadClass("org.matsim.api.core.v01.Id$IdImpl");
		} catch (ClassNotFoundException ignored) {
			throw new IllegalStateException("Id$IdImpl not found");
		}

		fory.registerSerializer(idImpl, IdSerializer.class);
		fory.registerSerializer(IntArrayList.class, IntArrayListSerializer.class);
		fory.registerSerializer(AttributesImpl.class, AttributesSerializer.class);

		// Register all classes that are likely to be used as messages
		for (Class<? extends Message> msgClass : registry.messageClasses()) {
			fory.register(msgClass);
		}
	}

	@Override
	public <T extends Message> byte[] toBytes(T msg) {
		return fory.serialize(msg);
	}

	@Override
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
	@SuppressWarnings("unchecked")
	public <T extends Message> T deserialize(ByteBuffer buf) {
		MemoryBuffer in = MemoryBuffer.fromByteBuffer(buf);
		Object msg = fory.deserialize(in);
		buf.position(buf.position() + in.readerIndex());
		return (T) msg;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Message deserialize(ByteBuffer buf, int type) {
		Class<?> msgClass = registry.getType(type);
		if (msgClass == null) {
			throw new IllegalArgumentException("Type " + type + " was not registered for serialization. Messages that should be serialized must be at least package private to be detected.");
		}
		return deserialize(buf, (Class<? extends Message>) msgClass);
	}

	@Override
	public <T extends Message> T deserialize(ByteBuffer buf, Class<T> clazz) {
		MemoryBuffer in = MemoryBuffer.fromByteBuffer(buf);
		T msg = fory.deserialize(in, clazz);
		buf.position(buf.position() + in.readerIndex());
		return msg;
	}

	@Override
	public String toString() {
		return "ForySerializationProvider{registry=" + registry + '}';
	}
}
