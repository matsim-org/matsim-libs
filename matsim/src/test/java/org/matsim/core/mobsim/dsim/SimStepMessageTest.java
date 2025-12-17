package org.matsim.core.mobsim.dsim;

import org.junit.jupiter.api.Test;
import org.matsim.dsim.scoring.BackPack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimStepMessageTest {

	@Test
	void build_populates_fields_and_lists() {
		SimStepMessage.Builder b = SimStepMessage.builder()
			.setSimstep(123.0);

		// Minimal placeholder instances; contents are not used by the tests
		CapacityUpdate cu = new CapacityUpdate(null, 1.0, 2.0);
		Teleportation tp = new Teleportation(null, null, 42.0);
		VehicleContainer vc = new VehicleContainer(null, null, null, List.of());
		BackPack bp = new BackPack(null, null);

		b.addCapacityUpdate(cu)
			.addTeleportation(tp)
			.addVehicleContainer(vc)
			.addBackPack(bp);

		SimStepMessage msg = b.build();

		assertEquals(123.0, msg.simstep());

		assertEquals(1, msg.capUpdates().size());
		assertEquals(1, msg.teleportations().size());
		assertEquals(1, msg.vehicles().size());
		assertEquals(1, msg.backPacks().size());

		assertSame(cu, msg.capUpdates().getFirst());
		assertSame(tp, msg.teleportations().getFirst());
		assertSame(vc, msg.vehicles().getFirst());
		assertSame(bp, msg.backPacks().getFirst());
	}

	@Test
	void clear_resets_state_and_creates_fresh_lists() {
		SimStepMessage.Builder b = SimStepMessage.builder().setSimstep(10.0);

		// Seed with one element in each list so we can compare references later
		CapacityUpdate cu = new CapacityUpdate(null, 1.0, 2.0);
		Teleportation tp = new Teleportation(null, null, 7.0);
		VehicleContainer vc = new VehicleContainer(null, null, null, List.of());
		BackPack bp = new BackPack(null, null);

		b.addCapacityUpdate(cu)
			.addTeleportation(tp)
			.addVehicleContainer(vc)
			.addBackPack(bp);

		SimStepMessage msgBeforeClear = b.build();
		assertEquals(10.0, msgBeforeClear.simstep());
		assertFalse(msgBeforeClear.capUpdates().isEmpty());
		assertFalse(msgBeforeClear.teleportations().isEmpty());
		assertFalse(msgBeforeClear.vehicles().isEmpty());
		assertFalse(msgBeforeClear.backPacks().isEmpty());

		// Now clear the builder and build again
		b.clear();
		SimStepMessage msgAfterClear = b.build();

		// simstep reset
		assertEquals(0.0, msgAfterClear.simstep());

		// Lists are emptied
		assertTrue(msgAfterClear.capUpdates().isEmpty());
		assertTrue(msgAfterClear.teleportations().isEmpty());
		assertTrue(msgAfterClear.vehicles().isEmpty());
		assertTrue(msgAfterClear.backPacks().isEmpty());

		// And list instances are fresh (not the same as before clear)
		assertNotSame(msgBeforeClear.capUpdates(), msgAfterClear.capUpdates());
		assertNotSame(msgBeforeClear.teleportations(), msgAfterClear.teleportations());
		assertNotSame(msgBeforeClear.vehicles(), msgAfterClear.vehicles());
		assertNotSame(msgBeforeClear.backPacks(), msgAfterClear.backPacks());
	}
}
