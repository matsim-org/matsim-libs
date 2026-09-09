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
 * Catalog of all {@link Message} types on the classpath and their stable integer type ids.
 * <p>
 * The registry scans the classpath once (using {@link ClassGraph}) to find every concrete class that implements
 * {@link Message} - including subclasses of {@link Event}, {@link Leg} and {@link Activity} - and assigns each a stable
 * id derived from its fully qualified class name. These ids are used to route messages and events through the distributed
 * simulation, regardless of whether the run is actually distributed.
 * <p>
 * The classpath scan is comparatively expensive, therefore this class is a lazily initialized singleton. It contains no
 * serialization logic and has no dependency on the wire format; see {@link SerializationProvider} for the actual
 * serialization, which is only required for runs that span multiple compute nodes.
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
