package org.matsim.core.serialization;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * Catalog of every concrete {@link Message} type on the classpath, each mapped to a stable integer id derived from its
 * fully qualified class name. The scan (via {@link ClassGraph}) also picks up subclasses of {@link Event}, {@link Leg}
 * and {@link Activity}. The ids identify messages and events as they are routed through the simulation, whether or not
 * the run is distributed.
 * <p>
 * The classpath scan is expensive, so this is a lazily initialized singleton. Wire-format serialization is a separate
 * concern, handled by {@link SerializationProvider}.
 */
public final class MessageTypeRegistry {

	private static volatile MessageTypeRegistry instance;

	/**
	 * Returns the singleton instance, performing the classpath scan on first access.
	 */
	public static MessageTypeRegistry getInstance() {
		MessageTypeRegistry result = instance;
		if (result == null) {
			synchronized (MessageTypeRegistry.class) {
				result = instance;
				if (result == null) {
					result = new MessageTypeRegistry();
					instance = result;
				}
			}
		}
		return result;
	}

	private final Int2ObjectMap<Class<? extends Message>> type2Class = new Int2ObjectOpenHashMap<>(128);
	private final Object2IntMap<Class<? extends Message>> class2Type = new Object2IntOpenHashMap<>();

	private MessageTypeRegistry() {

		try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {

			for (ClassInfo info : scanResult.getClassesImplementing(Message.class)
				.union(scanResult.getSubclasses(Event.class))
				.union(scanResult.getSubclasses(Leg.class))
				.union(scanResult.getSubclasses(Activity.class))
			) {

				@SuppressWarnings("unchecked")  // we filter for subclasses of Message above, so this cast is safe.
				Class<? extends Message> msgClass = (Class<? extends Message>) info.loadClass();

				if (msgClass.isInterface() || Modifier.isAbstract(msgClass.getModifiers()))
					continue;

				int msgType = msgClass.getName().hashCode();

				if (type2Class.containsKey(msgType)) {
					throw new IllegalArgumentException("Duplicate provider for type %s. %s already registered.".formatted(msgClass,
						type2Class.get(msgType)));
				}

				type2Class.put(msgType, msgClass);
				class2Type.put(msgClass, msgType);
			}
		}
	}

	/**
	 * Returns all known concrete message classes. Useful for eagerly registering wire formats.
	 */
	public Collection<Class<? extends Message>> messageClasses() {
		return type2Class.values();
	}

	/**
	 * Return whether the given type is known to the registry.
	 */
	public boolean hasType(int type) {
		return type == Message.ANY_TYPE || type2Class.containsKey(type);
	}

	/**
	 * Return whether the given class was picked up by the classpath scan.
	 */
	public boolean isRegistered(Class<?> clazz) {
		return class2Type.containsKey(clazz);
	}

	/**
	 * Returns the type ids of {@code clazz} and every known subclass of it. A handler subscribed to a base event type
	 * (e.g. {@code ActivityEvent}) uses this to also receive its more specific subtypes.
	 *
	 * @param clazz the class to find assignable types for
	 * @return type ids for {@code clazz} and all its known subclasses
	 */
	public int[] getAssignableTypes(Class<?> clazz) {
		return class2Type.keySet().stream()
			.filter(clazz::isAssignableFrom)
			.mapToInt(this::getType)
			.toArray();
	}

	public int getType(Class<?> msgType) {
		if (msgType == Event.class) {
			return Message.ANY_TYPE;
		}

		if (!class2Type.containsKey(msgType)) {
			throw new IllegalArgumentException("No type for class " + msgType);
		}

		return class2Type.getInt(msgType);
	}

	public Class<?> getType(int type) {
		return type == Message.ANY_TYPE ? Event.class : type2Class.get(type);
	}

	@Override
	public String toString() {
		return "MessageTypeRegistry{classes=" + type2Class + '}';
	}
}
