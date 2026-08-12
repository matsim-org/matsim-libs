package org.matsim.contrib.pseudosimulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup.TransitEmulation;

/**
 * {@code fullTransitPerformanceTransmission} is superseded by {@code transitEmulation}, but configs
 * written against it still exist. It said only whether to emulate transit at all, so it maps onto
 * the new setting as {@code false -> none} and {@code true -> fullTransitPerformance}.
 *
 * <p>
 * The case that matters is a config that sets only the old flag to {@code false}: without the
 * mapping it would silently pick up the new default and start emulating transit.
 */
class TransitEmulationCompatibilityTest {

	@Test
	void defaultsToFullTransitPerformanceWhenNeitherIsNamed() {
		assertEquals(TransitEmulation.fullTransitPerformance, new PSimConfigGroup().resolveTransitEmulation());
	}

	@Test
	void honoursTheSupersededFlagWhenItIsTheOnlyOneNamed() {
		PSimConfigGroup off = new PSimConfigGroup();
		off.setFullTransitPerformanceTransmission(false);
		assertEquals(TransitEmulation.none, off.resolveTransitEmulation(),
				"a config that switched transit emulation off must not silently switch it back on");

		PSimConfigGroup on = new PSimConfigGroup();
		on.setFullTransitPerformanceTransmission(true);
		assertEquals(TransitEmulation.fullTransitPerformance, on.resolveTransitEmulation());
	}

	@Test
	void takesTheNewSettingWhenItIsTheOnlyOneNamed() {
		PSimConfigGroup config = new PSimConfigGroup();
		config.setTransitEmulation(TransitEmulation.waitAndStopStopTimes);
		assertEquals(TransitEmulation.waitAndStopStopTimes, config.resolveTransitEmulation());
	}

	@Test
	void acceptsBothWhenTheyAgreeOnWhetherTransitIsEmulated() {
		PSimConfigGroup deterministic = new PSimConfigGroup();
		deterministic.setFullTransitPerformanceTransmission(true);
		deterministic.setTransitEmulation(TransitEmulation.waitAndStopStopTimes);
		assertEquals(TransitEmulation.waitAndStopStopTimes, deterministic.resolveTransitEmulation(),
				"the old flag cannot name an emulator, so the new setting decides which one");

		PSimConfigGroup neither = new PSimConfigGroup();
		neither.setFullTransitPerformanceTransmission(false);
		neither.setTransitEmulation(TransitEmulation.none);
		assertEquals(TransitEmulation.none, neither.resolveTransitEmulation());
	}

	@Test
	void rejectsBothWhenTheyContradictEachOther() {
		PSimConfigGroup offButEmulating = new PSimConfigGroup();
		offButEmulating.setFullTransitPerformanceTransmission(false);
		offButEmulating.setTransitEmulation(TransitEmulation.waitAndStopStopTimes);
		String message = assertThrows(IllegalStateException.class, offButEmulating::resolveTransitEmulation)
				.getMessage();
		assertTrue(message.contains(PSimConfigGroup.FULL_TRANSIT_PERFORMANCE_TRANSMISSION)
				&& message.contains(PSimConfigGroup.TRANSIT_EMULATION),
				"the error should name both settings so the contradiction can be found: " + message);

		PSimConfigGroup onButNotEmulating = new PSimConfigGroup();
		onButNotEmulating.setFullTransitPerformanceTransmission(true);
		onButNotEmulating.setTransitEmulation(TransitEmulation.none);
		assertThrows(IllegalStateException.class, onButNotEmulating::resolveTransitEmulation);
	}
}
