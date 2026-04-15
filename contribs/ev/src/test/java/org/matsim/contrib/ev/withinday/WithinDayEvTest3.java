package org.matsim.contrib.ev.withinday;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder;
import org.matsim.contrib.ev.withinday.utils.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WithinDayEvTest3 {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWithoutProviders() {
		TestScenarioBuilder.TestScenario scenario = new TestScenarioBuilder(utils) //
			.addPerson("person", 1.0) //
			.addActivity("home", 0, 0, 10.0 * 3600.0) //
			.addActivity("work", 8, 8, 18.0 * 3600.0) //
			.addActivity("home", 0, 0) //
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

		var builder = new TestScenarioBuilder(utils);

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
		assertEquals(68366.0, scenario.tracker().activityStartEvents.getLast().getTime());

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


}
