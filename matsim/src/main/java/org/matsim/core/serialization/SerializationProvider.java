package org.matsim.core.serialization;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.reflect.ReflectionUtils;
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

	private static final SerializationProvider INSTANCE = new SerializationProvider();

	public static SerializationProvider getInstance() {
		return INSTANCE;
	}

	private final Int2ObjectMap<Class<? extends Message>> type2Class = new Int2ObjectOpenHashMap<>(128);
	private final Object2IntMap<Class<? extends Message>> class2Type = new Object2IntOpenHashMap<>();

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

		try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {

			// Register classes that are likely to be used as messages

			for (ClassInfo info : scanResult.getClassesImplementing(Message.class)
				.union(scanResult.getSubclasses(Event.class))
				.union(scanResult.getSubclasses(Leg.class))
				.union(scanResult.getSubclasses(Activity.class))
			) {

				@SuppressWarnings("unchecked")  // we filter for Subclasses of Message above, so this cast is safe.
				Class<? extends Message> msgClass = (Class<? extends Message>) info.loadClass();

				if (msgClass.isInterface() || ReflectionUtils.isAbstract(msgClass))
					continue;

				int msgType = msgClass.getName().hashCode();

				// Protobuf message
				fory.register(msgClass);

				if (type2Class.containsKey(msgType)) {
					throw new IllegalArgumentException("Duplicate provider for type %s. %s already registered.".formatted(msgClass,
						type2Class.get(msgType)));
				}

//                System.out.println("Registering " + msgType.getSimpleName() + " with type " + msgType);
				type2Class.put(msgType, msgClass);
				class2Type.put(msgClass, msgType);

			}
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

	public Message deserialize(MemoryBuffer in, int type) {
		var msgClass = type2Class.get(type);
		if (msgClass == null) {
			throw new IllegalArgumentException("Type " + type + " was not registered for serialization. Messages that should be serialized must be at least package private to be detected.");
		}

		return deserialize(in, msgClass);
	}

	public <T extends Message> T deserialize(MemoryBuffer in, Class<T> clazz) {
		return fory.deserialize(in, clazz);
	}

	public <T extends Message> byte[] serialize(T message) {
		if (!class2Type.containsKey(message.getClass())) {
			throw new IllegalArgumentException("Class " + message.getClass() + " was not registered for serialization. Messages that should be serialized must be at least package private to be detected.");
		}
		return fory.serialize(message);
	}

	@Override
	public String toString() {
		return "SerializationProvider{" +
			"classes=" + type2Class +
			'}';
	}

	/**
	 * Return whether the given type is supported.
	 */
	public boolean hasType(int type) {
		return type == Event.ANY_TYPE || type2Class.containsKey(type);
	}

	/**
	 * Returns all types for a given class. This is useful for event handlers which listen to a baseclass
	 * of events. For example, an event handler that listens for ActivityEvents also needs to handle SpecializedActivityEvents if
	 * those extend ActivityEvent. This method will return a list of message types for the given class and all
	 * its subclasses found in the object graph.
	 *
	 * @param clazz the class to find assignable types for
	 * @return an array of types for all known subclasses of clazz including the type for clazz itself.
	 */
	public int[] getAssignableTypes(Class<?> clazz) {
		return class2Type.keySet().stream()
			.filter(clazz::isAssignableFrom)
			.mapToInt(this::getType)
			.toArray();
	}

	public int getType(Class<?> msgType) {
		if (msgType == Event.class) {
			return Event.ANY_TYPE;
		}

		if (!class2Type.containsKey(msgType)) {
			throw new IllegalArgumentException("No type for class " + msgType);
		}

		return class2Type.getInt(msgType);
	}

	public Class<?> getType(int type) {
		return type == Event.ANY_TYPE ? Event.class : type2Class.get(type);
	}
}
