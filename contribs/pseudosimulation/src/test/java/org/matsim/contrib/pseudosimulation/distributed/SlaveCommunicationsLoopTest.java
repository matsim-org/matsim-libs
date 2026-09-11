package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class SlaveCommunicationsLoopTest {

	@Test
	void dispatchesEveryNonTerminatingModeAndAcknowledgesEachCommand() {
		RecordingOperations operations = new RecordingOperations();
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		List<Long> sleeps = new ArrayList<>();
		SlaveCommunicationsLoop loop = loop(
				reader(CommunicationsMode.TRANSMIT_SCENARIO, CommunicationsMode.TRANSMIT_TRAVEL_TIMES,
						CommunicationsMode.POOL_PERSONS, CommunicationsMode.DISTRIBUTE_PERSONS,
						CommunicationsMode.TRANSMIT_PLANS_TO_MASTER, CommunicationsMode.TRANSMIT_SCORES,
						CommunicationsMode.TRANSMIT_PERFORMANCE, CommunicationsMode.WAIT,
						CommunicationsMode.CONTINUE),
				acknowledger, operations, sleeps::add, status -> {
				});

		assertTrue(loop.run());

		assertEquals(List.of("distribute", "travel-times", "pool", "distribute", "plans", "status",
				"scores", "performance"), operations.calls);
		assertEquals(List.of(10L), sleeps);
		assertEquals(9, acknowledger.acknowledgements);
		assertEquals(9, acknowledger.flushes);
		assertEquals(1, acknowledger.resets);
	}

	@Test
	void terminatesForDieBeforeAcknowledgingOrResetting() {
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		SlaveCommunicationsLoop loop = loop(reader(CommunicationsMode.DIE), acknowledger,
				new RecordingOperations(), milliseconds -> {
				}, status -> {
					throw new Termination(status);
				});

		Termination termination = assertThrows(Termination.class, loop::run);

		assertEquals(0, termination.status);
		assertEquals(0, acknowledger.acknowledgements);
		assertEquals(0, acknowledger.flushes);
		assertEquals(0, acknowledger.resets);
	}

	@Test
	void reportsReadFailuresWithoutAcknowledgingOrResetting() {
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		SlaveCommunicationsLoop loop = loop(() -> {
			throw new ClassNotFoundException("broken command");
		}, acknowledger, new RecordingOperations(), milliseconds -> {
		}, status -> {
		});

		assertFalse(loop.run());
		assertEquals(0, acknowledger.acknowledgements);
		assertEquals(0, acknowledger.resets);
	}

	@Test
	void reportsActionFailuresBeforeAcknowledging() {
		RecordingOperations operations = new RecordingOperations();
		operations.failure = new IOException("broken action");
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		SlaveCommunicationsLoop loop = loop(reader(CommunicationsMode.TRANSMIT_SCORES), acknowledger,
				operations, milliseconds -> {
				}, status -> {
				});

		assertFalse(loop.run());
		assertEquals(List.of("scores"), operations.calls);
		assertEquals(0, acknowledger.acknowledgements);
		assertEquals(0, acknowledger.resets);
	}

	@Test
	void reportsInterruptedWaitWithoutAcknowledging() {
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		SlaveCommunicationsLoop loop = loop(reader(CommunicationsMode.WAIT), acknowledger,
				new RecordingOperations(), milliseconds -> {
					throw new InterruptedException("interrupted wait");
				}, status -> {
				});

		assertFalse(loop.run());
		assertEquals(0, acknowledger.acknowledgements);
		assertEquals(0, acknowledger.resets);
	}

	@Test
	void reportsAcknowledgementFailuresWithoutResetting() {
		RecordingAcknowledger acknowledger = new RecordingAcknowledger();
		acknowledger.failure = new IOException("broken acknowledgement");
		SlaveCommunicationsLoop loop = loop(reader(CommunicationsMode.CONTINUE), acknowledger,
				new RecordingOperations(), milliseconds -> {
				}, status -> {
				});

		assertFalse(loop.run());
		assertEquals(1, acknowledger.acknowledgements);
		assertEquals(0, acknowledger.flushes);
		assertEquals(0, acknowledger.resets);
	}

	private SlaveCommunicationsLoop loop(SlaveCommunicationsLoop.CommandReader reader,
			SlaveCommunicationsLoop.Acknowledger acknowledger, SlaveCommunicationsLoop.Operations operations,
			SlaveCommunicationsLoop.Sleeper sleeper, SlaveCommunicationsLoop.ProcessTerminator terminator) {
		return new SlaveCommunicationsLoop(reader, acknowledger, operations, sleeper, terminator,
				LogManager.getLogger(SlaveCommunicationsLoopTest.class));
	}

	private SlaveCommunicationsLoop.CommandReader reader(CommunicationsMode... modes) {
		Deque<CommunicationsMode> commands = new ArrayDeque<>(Arrays.asList(modes));
		return commands::removeFirst;
	}

	private static final class RecordingAcknowledger implements SlaveCommunicationsLoop.Acknowledger {
		private int acknowledgements;
		private int flushes;
		private int resets;
		private IOException failure;

		@Override
		public void acknowledge() throws IOException {
			acknowledgements++;
			if (failure != null) {
				throw failure;
			}
			flushes++;
		}

		@Override
		public void reset() {
			resets++;
		}
	}

	private static final class RecordingOperations implements SlaveCommunicationsLoop.Operations {
		private final List<String> calls = new ArrayList<>();
		private IOException failure;

		@Override
		public void distributePersons() throws IOException {
			record("distribute");
		}

		@Override
		public void transmitTravelTimes() throws IOException {
			record("travel-times");
		}

		@Override
		public void poolPersons() throws IOException {
			record("pool");
		}

		@Override
		public void transmitPlans() throws IOException {
			record("plans");
		}

		@Override
		public void transmitSlaveStatus() throws IOException {
			record("status");
		}

		@Override
		public void transmitScores() throws IOException {
			record("scores");
		}

		@Override
		public void transmitPerformance() throws IOException {
			record("performance");
		}

		private void record(String call) throws IOException {
			calls.add(call);
			if (failure != null) {
				throw failure;
			}
		}
	}

	private static final class Termination extends RuntimeException {
		private final int status;

		private Termination(int status) {
			this.status = status;
		}
	}
}
