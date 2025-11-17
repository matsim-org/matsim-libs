package org.matsim.core.serialization;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;
import org.apache.fury.reflect.ReflectionUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.serialization.custom.AttributesSerializer;
import org.matsim.core.serialization.custom.IdSerializer;
import org.matsim.core.serialization.custom.IntArrayListSerializer;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.nio.ByteBuffer;

/**
 * Provides serialization and deserialization of messages.
 */
public class SerializationProvider {

	private final Int2ObjectMap<Class<? extends Message>> classes = new Int2ObjectOpenHashMap<>(128);
	private final Object2IntMap<Class<? extends Message>> types = new Object2IntOpenHashMap<>();

	private final ThreadSafeFury fury;

	public SerializationProvider() {

		// multiple serializations of different objects.
		fury = Fury.builder().withLanguage(Language.JAVA)
			.withRefTracking(false)
			.withCodegen(true)
			.withMetaShare(false)
			.requireClassRegistration(false)
			.buildThreadSafeFury();

		// Manually register some allowed types
		fury.register(Coord.class, true);

		Class<?> idImpl;
		try {
			idImpl = SerializationProvider.class.getClassLoader().loadClass("org.matsim.api.core.v01.Id$IdImpl");
		} catch (ClassNotFoundException ignored) {
			throw new IllegalStateException("Id$IdImpl not found");
		}

		fury.registerSerializer(idImpl, IdSerializer.class);
		fury.register(idImpl, true);

		fury.registerSerializer(IntArrayList.class, IntArrayListSerializer.class);
		fury.registerSerializer(AttributesImpl.class, AttributesSerializer.class);

		try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {

			// Register classes that are likely to be used as messages

			for (ClassInfo info : scanResult.getClassesImplementing(Message.class)
				.union(scanResult.getSubclasses(Event.class))
				.union(scanResult.getSubclasses(Leg.class))
				.union(scanResult.getSubclasses(Activity.class))
			) {

				Class<? extends Message> msgType = (Class<? extends Message>) info.loadClass();

				if (msgType.isInterface() || ReflectionUtils.isAbstract(msgType))
					continue;

				int messageType = msgType.getName().hashCode();

				// Protobuf message
				fury.register(msgType, true);

				if (classes.containsKey(messageType)) {
					throw new IllegalArgumentException("Duplicate provider for type %s. %s already registered.".formatted(msgType,
						classes.get(messageType)));
				}

//                System.out.println("Registering " + msgType.getSimpleName() + " with type " + messageType);
				classes.put(messageType, msgType);
				types.put(msgType, messageType);

			}
		}
	}

	public static void main(String[] args) throws ClassNotFoundException {
		System.out.println(new SerializationProvider());
	}

	/**
	 * Return whether the given type is an event.
	 */
	public boolean isEvent(int type) {
		return classes.containsKey(type) && Event.class.isAssignableFrom(classes.get(type));
	}

	/**
	 * Serialize message object and return byte array.
	 */
	public <T extends Message> byte[] toBytes(T msg) {
		return fury.serialize(msg);
	}

	/**
	 * Deserialize a message that was serialized using {@link #toBytes(Message)}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Message> T parse(ByteBuffer buf) {
		return (T) fury.deserialize(buf);
	}

	public ByteMessageParser getParser(int type) {
		if (!classes.containsKey(type)) {
			throw new IllegalArgumentException("No provider for type " + type);
		}

		Class<? extends Message> msgType = classes.get(type);
		return (in) -> fury.deserializeJavaObject(in, msgType);
	}

	public FuryBufferParser getFuryParser(int type) {
		if (!classes.containsKey(type)) {
			throw new IllegalArgumentException("No provider for type " + type);
		}

		Class<? extends Message> msgType = classes.get(type);
		return (in) -> fury.deserializeJavaObject(in, msgType);
	}

	@Override
	public String toString() {
		return "SerializationProvider{" +
			"classes=" + classes +
			'}';
	}

	/**
	 * Return whether the given type is supported.
	 */
	public boolean hasType(int type) {
		return type == Event.ANY_TYPE || classes.containsKey(type);
	}

	public int getType(Class<?> msgType) {
		if (msgType == Event.class) {
			return Event.ANY_TYPE;
		}

		if (!types.containsKey(msgType)) {
			throw new IllegalArgumentException("No type for class " + msgType);
		}

		return types.getInt(msgType);
	}

	public Class<?> getType(int type) {
		return type == Event.ANY_TYPE ? Event.class : classes.get(type);
	}

	public ThreadSafeFury getFury() {
		return fury;
	}
}
