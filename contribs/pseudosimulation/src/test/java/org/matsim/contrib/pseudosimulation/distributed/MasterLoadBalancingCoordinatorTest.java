package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class MasterLoadBalancingCoordinatorTest {

	@Test
	void admitsReadySessionsBeforeSingleSessionEarlyReturn() {
		Fixture fixture = new Fixture();
		fixture.sessions.put(1, participant(1, true));

		fixture.coordinator().balance();

		assertEquals(List.of("wait", "admit"), fixture.operations.calls);
	}

	@Test
	void preservesCycleOrderTargetsValidSessionsAndTerminatesInvalidSessions() {
		Fixture fixture = new Fixture();
		Participant valid = participant(1, true);
		valid.numberOfPlans = 4;
		valid.usedMemory = 500;
		valid.persons.add(null);
		Participant invalid = participant(2, false);
		invalid.numberOfPlans = 6;
		invalid.usedMemory = 700;
		invalid.persons.add(null);
		fixture.sessions.put(1, valid);
		fixture.sessions.put(2, invalid);
		fixture.allocatedTargets = new int[] { 7 };

		MasterLoadBalancingCoordinator coordinator = fixture.coordinator();
		coordinator.balance();

		assertEquals(List.of("wait", "admit", "start:TRANSMIT_PERFORMANCE", "wait",
				"start:POOL_PERSONS", "wait", "terminate:2", "start:DISTRIBUTE_PERSONS"),
				fixture.operations.calls);
		assertEquals(7, valid.target);
		assertEquals(0, invalid.target);
		assertEquals(100, coordinator.bytesPerPlan());
		assertEquals(10, coordinator.bytesPerPerson());
		assertEquals(2, coordinator.takePersons(-2).size());
	}

	@Test
	void retainsInitialMemoryEstimatesWhenNoPlansAreReported() {
		Fixture fixture = new Fixture();
		fixture.sessions.put(1, participant(1, true));
		fixture.sessions.put(2, participant(2, true));
		fixture.allocatedTargets = new int[] { 4, 6 };

		MasterLoadBalancingCoordinator coordinator = fixture.coordinator();
		coordinator.balance();

		assertEquals(30, coordinator.bytesPerPlan());
		assertEquals(40, coordinator.bytesPerPerson());
		assertEquals(List.of(), coordinator.takePersons(2));
	}

	private Participant participant(int number, boolean ready) {
		return new Participant(number, ready);
	}

	private static final class Fixture {
		private final Map<Integer, Participant> sessions = new LinkedHashMap<>();
		private final Operations operations = new Operations(sessions);
		private int[] allocatedTargets = new int[0];

		private MasterLoadBalancingCoordinator coordinator() {
			return new MasterLoadBalancingCoordinator(new Sessions(sessions), operations,
					(times, sizes, max, used, bytesPlan, bytesPerson, dampening, population) -> allocatedTargets,
					LogManager.getLogger(MasterLoadBalancingCoordinatorTest.class), 100, 30, 40, 0.4);
		}
	}

	private static final class Operations implements MasterLoadBalancingCoordinator.Operations {
		private final List<String> calls = new ArrayList<>();
		private final Map<Integer, Participant> sessions;

		private Operations(Map<Integer, Participant> sessions) { this.sessions = sessions; }
		public void waitForSlaves() { calls.add("wait"); }
		public void admitReadySessions() { calls.add("admit"); }
		public void start(CommunicationsMode mode) { calls.add("start:" + mode); }
		public int populationSize() { return 100; }
		public void terminate(int number) { calls.add("terminate:" + number); sessions.remove(number); }
	}

	private record Sessions(Map<Integer, Participant> participants)
			implements MasterLoadBalancingCoordinator.Sessions {
		public int size() { return participants.size(); }
		public Set<Integer> ids() { return participants.keySet(); }
		public Collection<? extends MasterLoadBalancingCoordinator.Participant> all() {
			return participants.values();
		}
		public MasterLoadBalancingCoordinator.Participant get(int number) { return participants.get(number); }
	}

	private static final class Participant implements MasterLoadBalancingCoordinator.Participant {
		private final int number;
		private final boolean ready;
		private final List<PersonSerializable> persons = new ArrayList<>();
		private int numberOfPlans;
		private long usedMemory = 100;
		private int target = -1;

		private Participant(int number, boolean ready) { this.number = number; this.ready = ready; }
		public int number() { return number; }
		public boolean ready() { return ready; }
		public int numberOfPlans() { return numberOfPlans; }
		public double totalIterationTime() { return number * 2.0; }
		public int currentPopulationSize() { return number * 10; }
		public long usedMemory() { return usedMemory; }
		public long maximumMemory() { return 1000; }
		public Collection<? extends PersonSerializable> persons() { return persons; }
		public void targetPopulationSize(int value) { target = value; }
	}
}
