package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MasterIterationOrchestratorTest {

	private final RecordingOperations operations = new RecordingOperations();
	private final MasterIterationOrchestrator orchestrator = new MasterIterationOrchestrator(operations);

	@Test
	void startsScenarioOnlyWhenInitialRoutingIsDisabled() {
		orchestrator.startup(false, true, () -> { throw new AssertionError("must remain lazy"); });

		assertEquals(List.of("start:TRANSMIT_SCENARIO"), operations.calls);
	}

	@Test
	void preservesParallelInitialRoutingAndLoadBalanceOrder() {
		orchestrator.startup(true, true, () -> true);

		assertEquals(List.of("start:TRANSMIT_SCENARIO", "wait", "start:TRANSMIT_PLANS_TO_MASTER",
				"wait", "merge", "apply-initial", "load-balance", "wait", "start:CONTINUE"),
				operations.calls);
	}

	@Test
	void waitsAtIterationStartAndReceivesPlansOnlyInParallelMode() {
		orchestrator.iterationStarts(3, 10, true);

		assertEquals(List.of("wait", "start:TRANSMIT_PLANS_TO_MASTER"), operations.calls);
	}

	@Test
	void onlyWaitsAtSerialIterationStart() {
		orchestrator.iterationStarts(3, 10, false);

		assertEquals(List.of("wait"), operations.calls);
	}

	@Test
	void skipsIterationWorkAfterInnovationEnds() {
		orchestrator.iterationStarts(11, 10, true);
		orchestrator.afterMobsim(11, 10, false,
				() -> { throw new AssertionError("must remain lazy"); });

		assertEquals(List.of(), operations.calls);
	}

	@Test
	void stopsSlavesAtTheTerminalInnovationIteration() {
		orchestrator.afterMobsim(10, 10, true, () -> true);

		assertEquals(List.of("start:DIE"), operations.calls);
	}

	@Test
	void preservesParallelAfterMobsimOrder() {
		orchestrator.afterMobsim(5, 10, true, () -> true);

		assertEquals(List.of("wait", "merge", "wait", "start:TRANSMIT_SCORES", "wait",
				"load-balance", "wait", "travel-times", "start:TRANSMIT_TRAVEL_TIMES"),
				operations.calls);
	}

	@Test
	void preservesSerialAfterMobsimOrder() {
		orchestrator.afterMobsim(5, 10, false, () -> false);

		assertEquals(List.of("wait", "travel-times", "start:TRANSMIT_TRAVEL_TIMES", "wait",
				"start:TRANSMIT_PLANS_TO_MASTER", "wait", "merge", "wait",
				"start:TRANSMIT_SCORES", "wait"), operations.calls);
	}

	@Test
	void stopsRegistryBeforeSlavesAtShutdown() {
		orchestrator.shutdown();

		assertEquals(List.of("stop-registry", "start:DIE"), operations.calls);
	}

	private static final class RecordingOperations implements MasterIterationOrchestrator.Operations {
		private final List<String> calls = new ArrayList<>();

		public void start(CommunicationsMode mode) { calls.add("start:" + mode); }
		public void waitForSlaves() { calls.add("wait"); }
		public void mergePlans() { calls.add("merge"); }
		public void applyInitialPlans() { calls.add("apply-initial"); }
		public void loadBalance() { calls.add("load-balance"); }
		public void updateTravelTimes() { calls.add("travel-times"); }
		public void stopRegistry() { calls.add("stop-registry"); }
	}
}
