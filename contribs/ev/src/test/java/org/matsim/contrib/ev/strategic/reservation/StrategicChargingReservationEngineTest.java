package org.matsim.contrib.ev.strategic.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservability;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.replanning.innovator.RandomChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder.TestScenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.testcases.MatsimTestUtils;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StrategicChargingReservationEngine}.
 * <p>
 * All tests use mobsim "dsim" with two threads so that agents and chargers may
 * live on different partitions.
 * <p>
 * Note: {@link org.matsim.contrib.ev.strategic.StrategicChargingAlternativeProvider} is not fully
 * DSim-compatible when using {@code AlternativeSearchStrategy.ReservationBased} (it calls
 * {@code addLocalReservation} for potentially remote chargers). Tests here use the
 * default online search strategy and only verify advance-reservation behaviour.
 */
public class StrategicChargingReservationEngineTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	// ------------------------------------------------------------------
	// Shared event handler — must be a top-level or static inner class
	// so that DSim's LambdaUtils can access it for serialization checks.
	// ProcessingMode.DIRECT runs the handler on the main thread, avoiding
	// serialisation entirely.
	// ------------------------------------------------------------------

	@DistributedEventHandler(processing = ProcessingMode.DIRECT)
	public static class ReservationTracker implements AdvanceReservationEventHandler {
		public final List<AdvanceReservationEvent> events = new LinkedList<>();

		@Override
		public void handleEvent(AdvanceReservationEvent event) {
			events.add(event);
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/** Make the charger reservable and let the innovator create reserved activities. */
	private static void enableAdvanceReservationPlanning(TestScenario scenario) {
		// Mark all chargers as reservable so plan innovation produces isReserved=true activities
		ChargingInfrastructureSpecification infrastructure =
			(ChargingInfrastructureSpecification) scenario.scenario().getScenarioElement("infrastructure");
		for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
			ChargerReservability.setReservable(charger, true);
		}

		// Always include reservations during innovation
		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
		((RandomChargingPlanInnovator.Parameters) config.getInnovationParameters())
			.setReservationProbability(1.0);
		((RandomChargingPlanInnovator.Parameters) config.getInnovationParameters())
			.setActivityInclusionProbability(1.0);
	}

	// ------------------------------------------------------------------
	// 1. Basic: AdvanceReservationEvent fires when slack is set
	// ------------------------------------------------------------------

	/**
	 * Agent has reservation slack and the charger is reservable. After one
	 * iteration an advance reservation event must be fired and must be successful.
	 */
	@Test
	public void testAdvanceReservationFired() {
		TestScenario scenario = new TestScenarioBuilder(utils)
			.enableStrategicCharging(1)
			.addWorkCharger(8, 8, 1, 1.0, "default")
			.setElectricVehicleRange(10000.0)
			.addPerson("person", 0.5)
			.addActivity("home", 0, 0, 10.0 * 3600.0)
			.addActivity("work", 8, 8, 18.0 * 3600.0)
			.addActivity("home", 0, 0)
			//.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
		config.getScoringParameters().setZeroSoc(-1000.0);
		config.setMinimumEnrouteDriveTime(Double.POSITIVE_INFINITY);

		enableAdvanceReservationPlanning(scenario);

		Person person = scenario.scenario().getPopulation().getPersons().get(Id.createPersonId("person"));
		StrategicChargingReservationEngine.setReservationSlack(person, 7200.0);

		ReservationTracker tracker = new ReservationTracker();
		scenario.controller().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		scenario.controller().run();

		assertFalse(tracker.events.isEmpty(), "Expected at least one AdvanceReservationEvent");
		assertTrue(tracker.events.stream().allMatch(AdvanceReservationEvent::getSuccessful),
			"All advance reservations should be successful");
	}

	// ------------------------------------------------------------------
	// 2. No slack → no advance reservation event
	// ------------------------------------------------------------------

	/**
	 * When no reservation slack is set, no advance reservation events must fire.
	 */
	@Test
	public void testNoSlackNoAdvanceReservation() {
		TestScenario scenario = new TestScenarioBuilder(utils)
			.enableStrategicCharging(1)
			.addWorkCharger(8, 8, 1, 1.0, "default")
			.setElectricVehicleRange(10000.0)
			.addPerson("person", 0.5)
			.addActivity("home", 0, 0, 10.0 * 3600.0)
			.addActivity("work", 8, 8, 18.0 * 3600.0)
			.addActivity("home", 0, 0)
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
		config.getScoringParameters().setZeroSoc(-1000.0);
		config.setMinimumEnrouteDriveTime(Double.POSITIVE_INFINITY);

		// Deliberately do NOT call setReservationSlack → engine must stay silent

		ReservationTracker tracker = new ReservationTracker();
		scenario.controller().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		scenario.controller().run();

		assertTrue(tracker.events.isEmpty(),
			"No AdvanceReservationEvent expected when reservation slack is not set");
	}

	// ------------------------------------------------------------------
	// 3. Remote charger: advance reservation works via async addReservation
	// ------------------------------------------------------------------

	/**
	 * Agent's home activity is at (0,0) and work charger is at (8,8). With two
	 * DSim threads these typically reside on different partitions. The advance
	 * reservation must still succeed because {@link StrategicChargingReservationEngine}
	 * routes the request via the async {@code addReservation()} path.
	 */
	@Test
	public void testAdvanceReservationRemoteCharger() {
		TestScenario scenario = new TestScenarioBuilder(utils)
			.enableStrategicCharging(1)
			.addWorkCharger(8, 8, 1, 1.0, "default")
			.setElectricVehicleRange(10000.0)
			.addPerson("person", 0.5)
			.addActivity("home", 0, 0, 10.0 * 3600.0)
			.addActivity("work", 8, 8, 18.0 * 3600.0)
			.addActivity("home", 0, 0)
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
		config.getScoringParameters().setZeroSoc(-1000.0);
		config.setMinimumEnrouteDriveTime(Double.POSITIVE_INFINITY);

		enableAdvanceReservationPlanning(scenario);

		Person person = scenario.scenario().getPopulation().getPersons().get(Id.createPersonId("person"));
		StrategicChargingReservationEngine.setReservationSlack(person, 7200.0);

		ReservationTracker tracker = new ReservationTracker();
		scenario.controller().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		scenario.controller().run();

		assertFalse(tracker.events.isEmpty(),
			"AdvanceReservationEvent must fire even when charger may be on a remote partition");
		assertTrue(tracker.events.stream().allMatch(AdvanceReservationEvent::getSuccessful),
			"Reservation should succeed for remote charger via async path");
	}

	// ------------------------------------------------------------------
	// 4. Partition transfer: reservation follows the agent
	// ------------------------------------------------------------------

	/**
	 * With a small reservation slack the {@code AdvanceReservation} may be
	 * scheduled on the agent's starting partition but not yet fired when the
	 * agent crosses to the neighbouring partition. The engine must transfer the
	 * pending reservation via {@link org.matsim.core.mobsim.dsim.NotifyAgentPartitionTransfer}
	 * so that it is still processed on the receiving partition.
	 * <p>
	 * We verify exactly one event fires per reservation (no duplicates, no drops).
	 */
	@Test
	public void testAdvanceReservationAfterPartitionTransfer() {
		TestScenario scenario = new TestScenarioBuilder(utils)
			.enableStrategicCharging(1)
			.addWorkCharger(8, 8, 1, 1.0, "default")
			.setElectricVehicleRange(10000.0)
			.addPerson("person", 0.5)
			.addActivity("home", 0, 0, 10.0 * 3600.0)
			.addActivity("work", 8, 8, 18.0 * 3600.0)
			.addActivity("home", 0, 0)
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
		config.getScoringParameters().setZeroSoc(-1000.0);
		config.setMinimumEnrouteDriveTime(Double.POSITIVE_INFINITY);

		enableAdvanceReservationPlanning(scenario);

		Person person = scenario.scenario().getPopulation().getPersons().get(Id.createPersonId("person"));
		// Small slack: reservation fires while agent is en-route, possibly after partition crossing
		StrategicChargingReservationEngine.setReservationSlack(person, 600.0);

		ReservationTracker tracker = new ReservationTracker();
		scenario.controller().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		scenario.controller().run();

		assertFalse(tracker.events.isEmpty(),
			"AdvanceReservationEvent must fire even after partition transfer");
		assertEquals(1, tracker.events.size(),
			"Exactly one AdvanceReservationEvent per reservation (no duplicates after transfer)");
		assertTrue(tracker.events.getFirst().getSuccessful(),
			"Reservation should succeed after partition transfer");
	}
}
