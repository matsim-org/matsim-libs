package org.matsim.contrib.ev.withinday;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservability;
import org.matsim.contrib.ev.reservation.ChargerReservationModule;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder;
import org.matsim.contrib.ev.withinday.utils.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Takes the excellent test suite from {@link WithinDayEvTest} and applies it to the distributed version of the engine.
 */
public class DistributedWithinDayEvTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWithoutProviders() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();
		controller.run();

		assertEquals("work", scenario.tracker().activityStartEvents.get(0).getActType());
		assertEquals(39217.0, scenario.tracker().activityStartEvents.get(0).getTime());
		assertEquals("home", scenario.tracker().activityStartEvents.get(1).getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.get(1).getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtFirstTry() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 8, 8, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68575.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(64956.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtFirstTryWithLongerDistance() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 5, 5, 1, 1.0) // charger is further away from work
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68431.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(38011.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(66018.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtFirstActivitySameLocation() {

		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 0, 0, 1, 1.0) // located at home
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0)
			.setMobsim("dsim")
			.build();

		var controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at work
		assertEquals("work", scenario.tracker().activityStartEvents.get(1).getActType());
		assertEquals(39373.0, scenario.tracker().activityStartEvents.get(1).getTime());

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(36156.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtFirstActivityOtherLocation() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 1, 1, 1, 1.0) // located at home
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(36562.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testFailAtFirstActivitySameLocation() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 0, 0, 0, 1.0) // located at home
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at work
		assertEquals("work", scenario.tracker().activityStartEvents.get(1).getActType());
		assertEquals(39373.0, scenario.tracker().activityStartEvents.get(1).getTime());

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(0, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:ev:access interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testFailAtFirstActivityOtherLocation() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 1, 1, 0, 1.0) // located at home
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at work
		assertEquals("work", scenario.tracker().activityStartEvents.get(1).getActType());
		assertEquals(39377.0, scenario.tracker().activityStartEvents.get(1).getTime());

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(0, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:ev:access interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtLastActivity() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 0, 0, 1, 1.0) // at home
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(LastActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68586.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(68419.0, scenario.tracker().plugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeAtSecondTry() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 0, 1.0) // plug count zero
			.addCharger("charger2", 7, 7, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(OrderedAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68365.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(2, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger1", scenario.tracker().queuedAtChargerEvents.getFirst().getChargerId().toString());
		assertEquals("charger2", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(2, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(40323.0, scenario.tracker().plugActivityEvents.get(1).getTime());
		assertEquals(65148.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testFailAfterSecondTry() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 0, 1.0) // plug count zero
			.addCharger("charger2", 7, 7, 0, 1.0) // plug count zero
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(OrderedAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(2, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(2, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(0, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(2, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(2, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger1", scenario.tracker().queuedAtChargerEvents.getFirst().getChargerId().toString());
		assertEquals("charger2", scenario.tracker().queuedAtChargerEvents.get(1).getChargerId().toString());

		// check engine logic
		assertEquals(2, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(40323.0, scenario.tracker().plugActivityEvents.get(1).getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testFailAfterSecondTryAndStuck() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 0, 1.0) // plug count zero
			.addCharger("charger2", 7, 7, 0, 1.0) // plug count zero
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		WithinDayEvConfigGroup config = WithinDayEvConfigGroup.get(scenario.config());
		config.setAbortAgents(true);

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(OrderedAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals(40624.0, scenario.tracker().personStuckEvents.getFirst().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(2, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(2, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(0, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(2, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(2, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger1", scenario.tracker().queuedAtChargerEvents.getFirst().getChargerId().toString());
		assertEquals("charger2", scenario.tracker().queuedAtChargerEvents.get(1).getChargerId().toString());

		// check engine logic
		assertEquals(2, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(40323.0, scenario.tracker().plugActivityEvents.get(1).getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:car",
			"activity:ev:plug interaction")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChangeWhileApproaching() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 1, 1.0)
			.addCharger("charger2", 7, 7, 1, 1.0)
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		WithinDayEvConfigGroup config = WithinDayEvConfigGroup.get(scenario.config());
		config.setAbortAgents(true);

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(SwitchChargerAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68365.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger2", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(38815.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(65148.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testTwoAgentsCompetition() {
		/*
		 * Two agents want to use the same charger at work, but there is only one plug.
		 * The first agent therefore occupies the plug, so the second one that is
		 * leaving 30s later must fall back to the other charger using online search.
		 */
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 1, 1.0) // plug count zero
			.addCharger("charger2", 7, 7, 1, 1.0) //
			.addPerson("person1", 1.0) //
			/**/.addActivity("home", 0, 0, 10.0 * 3600.0) //
			/**/.addActivity("work", 8, 8, 18.0 * 3600.0) //
			/**/.addActivity("home", 0, 0) //
			.addPerson("person2", 1.0) //
			/**/.addActivity("home", 0, 0, 10.0 * 3600.0 + 30.0) // 30 seconds later
			/**/.addActivity("work", 8, 8, 18.0 * 3600.0) //
			/**/.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(OrderedAlternativeProvider.class);
			}
		});

		controller.run();

		// check charging process
		assertEquals(2, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(2, scenario.tracker().finishChargingProcessEvents.size());

		assertEquals(3, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(2, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(2, scenario.tracker().chargingStartEvents.size());
		assertEquals(2, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("person1", scenario.tracker().chargingStartEvents.getFirst().getVehicleId().toString());
		assertEquals("charger1", scenario.tracker().chargingStartEvents.get(0).getChargerId().toString());
		assertEquals(39224.0, scenario.tracker().chargingStartEvents.get(0).getTime());

		assertEquals("person2", scenario.tracker().chargingStartEvents.get(1).getVehicleId().toString());
		assertEquals("charger2", scenario.tracker().chargingStartEvents.get(1).getChargerId().toString());
		assertEquals(40364.0, scenario.tracker().chargingStartEvents.get(1).getTime());

		assertEquals("person2", scenario.tracker().queuedAtChargerEvents.getFirst().getVehicleId().toString());
		assertEquals(39254.0, scenario.tracker().queuedAtChargerEvents.getFirst().getTime());

		assertEquals("person2", scenario.tracker().quitQueueAtChargerEvents.getFirst().getVehicleId().toString());
		assertEquals(39548.0, scenario.tracker().quitQueueAtChargerEvents.getFirst().getTime());
	}

	//TODO this test fails, because reservations don't work accorss partitions
	@Test
	public void testTwoAgentsCompetitionWithReservation() {
		/*
		 * Two agents want to use the same charger at work. When departing, the second
		 * agent is allowed to reserve the charger. Hence, it is the first agent that
		 * will be queued at the charger and then falls back to the other one.
		 */
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 8, 8, 1, 1.0) // plug count zero
			.addCharger("charger2", 7, 7, 1, 1.0) //
			.addPerson("person1", 1.0) //
			/**/.addActivity("home", 0, 0, 10.0 * 3600.0) //
			/**/.addActivity("work", 8, 8, 18.0 * 3600.0) //
			/**/.addActivity("home", 0, 0) //
			.addPerson("person2", 1.0) //
			/**/.addActivity("home", 0, 0, 10.0 * 3600.0 + 30.0) // 30 seconds later
			/**/.addActivity("work", 8, 8, 18.0 * 3600.0) //
			/**/.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		ChargingInfrastructureSpecification infrastructure = (ChargingInfrastructureSpecification) scenario.scenario()
			.getScenarioElement("infrastructure");
		for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
			ChargerReservability.setReservable(charger, true);
		}

		Controler controller = scenario.controller();
		controller.addOverridingModule(new ChargerReservationModule());

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(ReservationAlternativeProvider.class);
			}
		});

		controller.run();

		// check charging process
		assertEquals(2, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(2, scenario.tracker().finishChargingProcessEvents.size());

		assertEquals(3, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(2, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(2, scenario.tracker().chargingStartEvents.size());
		assertEquals(2, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("person2", scenario.tracker().chargingStartEvents.get(0).getVehicleId().toString());
		assertEquals("charger1", scenario.tracker().chargingStartEvents.get(0).getChargerId().toString());
		assertEquals(39254.0, scenario.tracker().chargingStartEvents.get(0).getTime());

		assertEquals("person1", scenario.tracker().chargingStartEvents.get(1).getVehicleId().toString());
		assertEquals("charger2", scenario.tracker().chargingStartEvents.get(1).getChargerId().toString());
		assertEquals(40334.0, scenario.tracker().chargingStartEvents.get(1).getTime());

		assertEquals("person1", scenario.tracker().queuedAtChargerEvents.get(0).getVehicleId().toString());
		assertEquals(39224.0, scenario.tracker().queuedAtChargerEvents.get(0).getTime());

		assertEquals("person1", scenario.tracker().quitQueueAtChargerEvents.get(0).getVehicleId().toString());
		assertEquals(39518.0, scenario.tracker().quitQueueAtChargerEvents.get(0).getTime());
	}

	@Test
	public void testChargeOverMultipleActivities() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 8, 8, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 14.0 * 3600.0) //
			.addActivity("work", 8, 8, 16.0 * 3600.0, "walk") //
			.addActivity("work", 8, 8, 18.0 * 3600.0, "walk") //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68575.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(64956.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeUntilSecondActivity() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 8, 8, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0, "walk") // unplugging first at work
			.addActivity("work", 8, 8, 18.0 * 3600.0, "walk") // same as home
			.addActivity("home", 0, 0, "car") //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68575.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(64956.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeWholeDay() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 8, 8, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 0, 0, 18.0 * 3600.0, "walk") // same as home
			.addActivity("home", 0, 0, "walk") //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WholeDaySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		// assertEquals(68576.0,
		// scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(0, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(0, scenario.tracker().plugActivityEvents.size());
		assertEquals(0, scenario.tracker().unplugActivityEvents.size());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeOnRoute() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 4, 4, 1, 1.0) //
			.addPerson("person", 0.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstLegSlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals(37619.0, scenario.tracker().chargingStartEvents.getFirst().getTime());
		assertEquals(41220.0, scenario.tracker().chargingEndEvents.getFirst().getTime());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(37609.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(41220.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(41220.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"activity:ev:wait interaction",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeOnRouteWithChange() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 4, 4, 1, 1.0) //
			.addCharger("charger2", 5, 5, 1, 1.0) //
			.addPerson("person", 0.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstLegSlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(SwitchChargerAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals(38024.0, scenario.tracker().chargingStartEvents.getFirst().getTime());
		assertEquals(41625.0, scenario.tracker().chargingEndEvents.getFirst().getTime());

		assertEquals("charger2", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(38011.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(41625.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(41625.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"activity:ev:wait interaction",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChargeOnRouteWithTwoAttempts() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 4, 4, 0, 1.0) // blocked
			.addCharger("charger2", 5, 5, 1, 1.0) //
			.addPerson("person", 0.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstLegSlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(OrderedAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(2, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(1, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(1, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals(38324.0, scenario.tracker().chargingStartEvents.getFirst().getTime());
		assertEquals(41925.0, scenario.tracker().chargingEndEvents.getFirst().getTime());

		assertEquals("charger2", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(2, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(37609.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(38313.0, scenario.tracker().plugActivityEvents.getLast().getTime());
		assertEquals(41925.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(41925.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"activity:ev:wait interaction",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChangeActivityToOnRoute() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 4, 4, 1, 1.0) //
			.addCharger("charger2", 5, 5, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).toProvider(ActivityLegChangeProvider.createProvider(true));
				bind(ChargingAlternativeProvider.class).toProvider(ActivityLegChangeProvider.createProvider(true));
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger2", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(38011.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(41625.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(41625.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"activity:ev:wait interaction",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}

	@Test
	public void testChangeOnRouteToActivityBased() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger1", 4, 4, 1, 1.0) //
			.addCharger("charger2", 5, 5, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).toProvider(ActivityLegChangeProvider.createProvider(false));
				bind(ChargingAlternativeProvider.class).toProvider(ActivityLegChangeProvider.createProvider(false));
			}
		});

		try {
			controller.run();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().startsWith("Cannot switch from a leg-based"));
			return;
		}

		fail();
	}

	@Test
	public void testChangeOnrouteDuration() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 4, 4, 1, 1.0) //
			.addPerson("person", 0.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(FirstLegSlotProvider.class);
				bind(ChargingAlternativeProvider.class).to(ChangeDurationAlternativeProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals(37619.0, scenario.tracker().chargingStartEvents.getFirst().getTime());
		assertEquals(39420.0, scenario.tracker().chargingEndEvents.getFirst().getTime());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(37609.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(39420.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(39420.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());
	}

	@Test
	public void testSpontaneousCharging() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 4, 4, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		WithinDayEvConfigGroup config = WithinDayEvConfigGroup.get(scenario.config());
		config.setAllowSpoantaneousCharging(true);

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingAlternativeProvider.class).to(SpontaneousChargingProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68419.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(1, scenario.tracker().chargingStartEvents.size());
		assertEquals(1, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());

		// check engine logic
		assertEquals(1, scenario.tracker().plugActivityEvents.size());
		assertEquals(1, scenario.tracker().unplugActivityEvents.size());

		assertEquals(37609.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(41220.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());
		assertEquals(41220.0, scenario.tracker().unplugActivityEndEvents.getFirst().getTime());
	}

	@Test
	public void testTwoSlots() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addCharger("charger", 8, 8, 1, 1.0) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 14.0 * 3600.0) //
			.addActivity("home", 0, 0, 15.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
			.setMobsim("dsim")
			.setNumberOfThreads(2)
			.build();

		Controler controller = scenario.controller();

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingSlotProvider.class).to(WorkActivitySlotProvider.class);
			}
		});

		controller.run();

		// check arrival at home
		assertEquals("home", scenario.tracker().activityStartEvents.getLast().getActType());
		assertEquals(68575.0, scenario.tracker().activityStartEvents.getLast().getTime());

		// check charging process
		assertEquals(2, scenario.tracker().startChargingProcessEvents.size());
		assertEquals(2, scenario.tracker().finishChargingProcessEvents.size());
		assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

		assertEquals(2, scenario.tracker().startChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
		assertEquals(2, scenario.tracker().finishChargingAttemptEvents.size());
		assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

		// check charger interaction
		assertEquals(2, scenario.tracker().chargingStartEvents.size());
		assertEquals(2, scenario.tracker().chargingEndEvents.size());
		assertEquals(0, scenario.tracker().queuedAtChargerEvents.size());
		assertEquals(0, scenario.tracker().quitQueueAtChargerEvents.size());

		assertEquals("charger", scenario.tracker().chargingStartEvents.getFirst().getChargerId().toString());
		assertEquals("charger", scenario.tracker().chargingStartEvents.getLast().getChargerId().toString());

		// check engine logic
		assertEquals(2, scenario.tracker().plugActivityEvents.size());
		assertEquals(2, scenario.tracker().unplugActivityEvents.size());

		assertEquals(39217.0, scenario.tracker().plugActivityEvents.getFirst().getTime());
		assertEquals(50556.0, scenario.tracker().unplugActivityEvents.getFirst().getTime());

		assertEquals(57392.0, scenario.tracker().plugActivityEvents.getLast().getTime());
		assertEquals(64956.0, scenario.tracker().unplugActivityEvents.getLast().getTime());

		assertEquals(Arrays.asList(Arrays.array(
			// "activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home",
			"leg:walk",
			"activity:car interaction",
			"leg:car",
			"activity:ev:plug interaction",
			"leg:walk",
			"activity:work",
			"leg:walk",
			"activity:ev:unplug interaction",
			"leg:car",
			"activity:car interaction",
			"leg:walk",
			"activity:home")), scenario.tracker().sequences.get(Id.createPersonId("person")));
	}
}
