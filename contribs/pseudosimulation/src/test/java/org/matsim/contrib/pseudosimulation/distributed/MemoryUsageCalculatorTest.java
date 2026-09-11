package org.matsim.contrib.pseudosimulation.distributed;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryUsageCalculatorTest {

	@Test
	void preservesCollectionSequenceAroundMemorySamples() {
		List<String> calls = new ArrayList<>();
		RecordingRuntime runtime = new RecordingRuntime(calls, 1_000, 250);
		MemoryUsageCalculator calculator = new MemoryUsageCalculator(runtime,
				milliseconds -> calls.add("sleep:" + milliseconds));

		long usedMemory = calculator.getMemoryUse();

		assertEquals(750, usedMemory);
		assertEquals(List.of(
				"gc", "sleep:100", "finalize", "sleep:100",
				"gc", "sleep:100", "finalize", "sleep:100",
				"total",
				"gc", "sleep:100", "finalize", "sleep:100",
				"gc", "sleep:100", "finalize", "sleep:100",
				"free"), calls);
	}

	@Test
	void allowsNegativeUsedMemoryWithoutValidation() {
		MemoryUsageCalculator calculator = new MemoryUsageCalculator(
				new RecordingRuntime(new ArrayList<>(), 10, 20), milliseconds -> { });

		assertEquals(-10, calculator.getMemoryUse());
	}

	@Test
	void interruptedSleepPrintsTheExceptionAndContinuesRemainingCycles() {
		List<String> calls = new ArrayList<>();
		RecordingRuntime runtime = new RecordingRuntime(calls, 100, 40);
		int[] sleeps = {0};
		ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
		PrintStream originalError = System.err;
		try {
			System.setErr(new PrintStream(errorBytes));
			MemoryUsageCalculator calculator = new MemoryUsageCalculator(runtime, milliseconds -> {
				sleeps[0]++;
				calls.add("sleep:" + milliseconds);
				if (sleeps[0] == 1) {
					throw new InterruptedException("characterized interruption");
				}
			});

			assertEquals(60, calculator.getMemoryUse());
		} finally {
			System.setErr(originalError);
		}

		assertTrue(errorBytes.toString().contains("InterruptedException: characterized interruption"));
		assertEquals(4, runtime.garbageCollections);
		assertEquals(3, runtime.finalizations);
		assertEquals(7, sleeps[0]);
		assertFalse(Thread.currentThread().isInterrupted());
	}

	private static final class RecordingRuntime implements MemoryUsageCalculator.MemoryRuntime {
		private final List<String> calls;
		private final long totalMemory;
		private final long freeMemory;
		private int garbageCollections;
		private int finalizations;

		private RecordingRuntime(List<String> calls, long totalMemory, long freeMemory) {
			this.calls = calls;
			this.totalMemory = totalMemory;
			this.freeMemory = freeMemory;
		}

		@Override
		public long totalMemory() {
			calls.add("total");
			return totalMemory;
		}

		@Override
		public long freeMemory() {
			calls.add("free");
			return freeMemory;
		}

		@Override
		public void collectGarbage() {
			garbageCollections++;
			calls.add("gc");
		}

		@Override
		public void runFinalization() {
			finalizations++;
			calls.add("finalize");
		}
	}
}
