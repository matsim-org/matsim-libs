package org.matsim.dsim.simulation.net;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.vehicles.Vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PassingQueueTest {

	@Test
	void popsInExitTimeOrder() {
		PassingQueue queue = new PassingQueue();
		queue.addLast(vehicle("late", 30));
		queue.addLast(vehicle("early", 10));
		queue.addLast(vehicle("middle", 20));

		assertThat(poll(queue)).containsExactly("early", "middle", "late");
	}

	/**
	 * Vehicles that may leave at the same time have to come out in the same order whatever sequence they went in, because that sequence is
	 * not the same in every run - see the comparator in {@link PassingQueue}.
	 */
	@Test
	void breaksTiesOnTheVehicleIdWhateverTheInsertionOrder() {
		PassingQueue oneWayRound = new PassingQueue();
		oneWayRound.addLast(vehicle("a", 10));
		oneWayRound.addLast(vehicle("b", 10));
		oneWayRound.addLast(vehicle("c", 10));

		PassingQueue theOther = new PassingQueue();
		theOther.addLast(vehicle("c", 10));
		theOther.addLast(vehicle("b", 10));
		theOther.addLast(vehicle("a", 10));

		assertThat(poll(oneWayRound)).containsExactly("a", "b", "c");
		assertThat(poll(theOther)).containsExactly("a", "b", "c");
	}

	@Test
	void sortsByTimeFirstAndIdSecond() {
		PassingQueue queue = new PassingQueue();
		queue.addLast(vehicle("b", 10));
		queue.addLast(vehicle("a", 20));
		queue.addLast(vehicle("a", 10));
		queue.addLast(vehicle("b", 20));

		assertThat(poll(queue)).containsExactly("a", "b", "a", "b");
	}

	private static DistributedMobsimVehicle vehicle(String id, double earliestLinkExitTime) {
		DistributedMobsimVehicle vehicle = mock(DistributedMobsimVehicle.class);
		when(vehicle.getId()).thenReturn(Id.create(id, Vehicle.class));
		when(vehicle.getEarliestLinkExitTime()).thenReturn(earliestLinkExitTime);
		return vehicle;
	}

	private static java.util.List<String> poll(PassingQueue queue) {
		java.util.List<String> ids = new java.util.ArrayList<>();
		while (!queue.isEmpty()) {
			ids.add(queue.poll().getId().toString());
		}
		return ids;
	}
}
