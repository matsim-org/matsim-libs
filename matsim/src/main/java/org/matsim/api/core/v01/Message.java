package org.matsim.api.core.v01;

/**
 * Interface for arbitrarily serializable and exchangeable messages.
 * <p>
 * For distributed simulations, information exchange requires serializing Java-Objects. Classes that implement this interface are the object-representation
 * of the wireformat used during the transfer. Currently, the serialization and de-serializatino are realized by {@link org.matsim.core.serialization.SerializationProvider}.
 * On initialization it parses the object graph and creates a wire format for each {@link Message} it finds. To be picked up by this mechanism,
 * {@link Message}s must be at least package private. Private inner classes or records are NOT serializable for {@link org.matsim.core.serialization.SerializationProvider}.
 */
public interface Message {

	/**
	 * Reserved type that indicates any arbitrary event.
	 */
	int ANY_TYPE = Integer.MIN_VALUE;

	default int getType() {
		return getClass().getName().hashCode();
	}
}
