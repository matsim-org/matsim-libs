package org.matsim.core.communication;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Message;
import org.matsim.core.serialization.NoopSerializationProvider;
import org.matsim.core.serialization.SerializationProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A single-node run uses {@link NullCommunicator} together with {@link NoopSerializationProvider}. None of the collective
 * operations may reach into the serialization provider.
 */
class NullCommunicatorTest {

	private final NullCommunicator comm = new NullCommunicator();
	private final SerializationProvider failing = new NoopSerializationProvider();

	private record Msg(String data) implements Message {
	}

	@Test
	void allGatherReturnsOwnMessageWithoutSerializing() {
		var msg = new Msg("a");
		List<Msg> result = assertDoesNotThrow(() -> comm.allGather(msg, 0, failing));
		assertEquals(List.of(msg), result);
		// result must be mutable, callers sort it
		result.sort((x, y) -> 0);
	}

	@Test
	void collectiveOpsDoNotSerialize() {
		assertDoesNotThrow(() -> comm.send(0, new Msg("a"), failing));
		assertDoesNotThrow(() -> comm.allToAll(Map.of(0, new Msg("a")), failing));
		assertDoesNotThrow(() -> comm.gatherTo(0, new Msg("a"), failing));
		assertDoesNotThrow(() -> comm.gatherFromAll(Msg.class, failing));
	}
}
