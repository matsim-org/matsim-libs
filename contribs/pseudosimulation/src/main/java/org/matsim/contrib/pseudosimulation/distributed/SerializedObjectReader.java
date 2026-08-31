package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.List;
import java.util.Map;

/**
 * Keeps the unavoidable unchecked casts at the boundary of the existing Java
 * serialization protocol. The protocol verifies the outer collection type but
 * carries no runtime information about its generic element types.
 */
final class SerializedObjectReader {

	private SerializedObjectReader() {
	}

	@SuppressWarnings("unchecked")
	static <K, V> Map<K, V> readMap(ObjectInput input) throws IOException, ClassNotFoundException {
		return (Map<K, V>) input.readObject();
	}

	@SuppressWarnings("unchecked")
	static <T> List<T> readList(ObjectInput input) throws IOException, ClassNotFoundException {
		return (List<T>) input.readObject();
	}
}
