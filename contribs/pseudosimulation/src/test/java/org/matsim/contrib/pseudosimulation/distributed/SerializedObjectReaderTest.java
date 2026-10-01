package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SerializedObjectReaderTest {

	@Test
	void readsMapWithoutChangingItsEntries() throws IOException, ClassNotFoundException {
		Map<String, Integer> expected = new LinkedHashMap<>();
		expected.put("first", 1);
		expected.put("second", 2);

		try (ObjectInputStream input = serializedInput(expected)) {
			Map<String, Integer> actual = SerializedObjectReader.readMap(input);

			assertEquals(expected, actual);
		}
	}

	@Test
	void readsListWithoutChangingItsElements() throws IOException, ClassNotFoundException {
		List<String> expected = new ArrayList<>(List.of("first", "second"));

		try (ObjectInputStream input = serializedInput(expected)) {
			List<String> actual = SerializedObjectReader.readList(input);

			assertEquals(expected, actual);
		}
	}

	@Test
	void rejectsWrongOuterCollectionType() throws IOException {
		try (ObjectInputStream input = serializedInput(List.of("not a map"))) {
			assertThrows(ClassCastException.class, () -> SerializedObjectReader.readMap(input));
		}
	}

	@Test
	void leavesGenericElementMismatchLatentUntilElementUse() throws IOException, ClassNotFoundException {
		Map<Integer, String> serialized = Map.of(1, "one");

		try (ObjectInputStream input = serializedInput(serialized)) {
			Map<String, Integer> actual = SerializedObjectReader.readMap(input);

			assertEquals(1, actual.size());
			assertThrows(ClassCastException.class, () -> actual.keySet().iterator().next().length());
		}
	}

	private ObjectInputStream serializedInput(Object value) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(value);
		}
		return new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
	}
}
