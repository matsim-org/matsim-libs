package org.matsim.contrib.ev.strategic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.plan.ChargingPlansConverter;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder;
import org.matsim.contrib.ev.strategic.utils.TestScenarioBuilder.TestScenario;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class StrategicChargingTest {
    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testChargingAtActivity() {
        TestScenario scenario = new TestScenarioBuilder(utils) //
                .enableStrategicCharging(1) //
                .addWorkCharger(8, 8, 1, 1.0, "default") //
                .setElectricVehicleRange(10000.0) //
                .addPerson("person", 0.5) // SoC goes to zero after leaving from work
                .addActivity("home", 0, 0, 10.0 * 3600.0) //
                .addActivity("work", 8, 8, 18.0 * 3600.0) //
                .addActivity("home", 0, 0) //
                .build();

        StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
        config.scoreTrackingInterval = 1;
        config.scoring.zeroSoc = -1000.0; // incentivize agent to charge at work

        // motivate agent to charge at activity
        config.minimumEnrouteDriveTime = Double.POSITIVE_INFINITY;

        Controler controller = scenario.controller();
        controller.run();

        assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
        assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
        assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

        assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
        assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

        assertFalse(scenario.tracker().startChargingAttemptEvents.getFirst().isEnroute());

        assertEquals(1, scenario.tracker().chargingStartEvents.size());
        assertEquals(1, scenario.tracker().chargingEndEvents.size());

        // test output of charging plans
        Config outputConfig = ConfigUtils.createConfig();
        Scenario ouptutScenario = ScenarioUtils.createScenario(outputConfig);

        PopulationReader reader = new PopulationReader(ouptutScenario);
        reader.putAttributeConverter(ChargingPlans.class, new ChargingPlansConverter());
        reader.readFile(utils.getOutputDirectory() + "/output_plans.xml.gz");

        Object value = ouptutScenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan()
                .getAttributes().getAttribute(ChargingPlans.ATTRIBUTE);

        assertTrue(value != null);
        assertTrue(value instanceof ChargingPlans);
    }

    @Test
    public void testChargingEnroute() {
        TestScenario scenario = new TestScenarioBuilder(utils) //
                .enableStrategicCharging(3) //
                .addPublicCharger("charger", 6, 6, 1, 1.0, "default") //
                .setElectricVehicleRange(10000.0) //
                .addPerson("person", 0.5) // SoC goes to zero after leaving from work
                .addActivity("home", 0, 0, 10.0 * 3600.0) //
                .addActivity("work", 8, 8, 18.0 * 3600.0) //
                .addActivity("home", 0, 0) //
                .build();

        StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
        config.scoring.zeroSoc = -1000.0; // incentivize agent to charge

        // motivate agent to charge enroute
        config.maximumActivityChargingDuration = 0.0;
        config.minimumEnrouteDriveTime = 0;

        // define charging duration
        config.minimumEnrouteChargingDuration = 1800.0;
        config.maximumEnrouteChargingDuration = 1800.0;

        Controler controller = scenario.controller();
        controller.run();

        assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
        assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
        assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

        assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
        assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

        assertTrue(scenario.tracker().startChargingAttemptEvents.getFirst().isEnroute());

        assertEquals(1, scenario.tracker().chargingStartEvents.size());
        assertEquals(1, scenario.tracker().chargingEndEvents.size());
    }

    @Test
    public void testCriticalCharging() {
        TestScenario scenario = new TestScenarioBuilder(utils) //
                .enableStrategicCharging(0) //
                .addPublicCharger("charger", 2, 2, 1, 1.0, "default") //
                .setElectricVehicleRange(10000.0) //
                .addPerson("person", 0.5) // SoC goes to zero after leaving from work
                .addActivity("home", 0, 0, 10.0 * 3600.0) //
                .addActivity("work", 8, 8, 18.0 * 3600.0) //
                .addActivity("home", 0, 0) //
                .build();

        WithinDayEvConfigGroup wdConfig = WithinDayEvConfigGroup.get(scenario.config());
        wdConfig.allowSpoantaneousCharging = true;

        StrategicChargingConfigGroup config = StrategicChargingConfigGroup.get(scenario.config());
        config.scoring.zeroSoc = -1000.0; // incentivize agent to charge

        // disallow enroute and activity charging
        config.minimumEnrouteDriveTime = Double.POSITIVE_INFINITY;
        config.minimumActivityChargingDuration = Double.POSITIVE_INFINITY;

        // set critical soc
        scenario.scenario().getPopulation().getPersons().get(Id.createPersonId("person")).getAttributes()
                .putAttribute(CriticalAlternativeProvider.CRITICAL_SOC_PERSON_ATTRIBUTE, 0.1);

        Controler controller = scenario.controller();
        controller.run();

        assertEquals(1, scenario.tracker().startChargingProcessEvents.size());
        assertEquals(1, scenario.tracker().finishChargingProcessEvents.size());
        assertEquals(0, scenario.tracker().abortCharingProcessEvents.size());

        assertEquals(1, scenario.tracker().startChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().updateChargingAttemptEvents.size());
        assertEquals(1, scenario.tracker().finishChargingAttemptEvents.size());
        assertEquals(0, scenario.tracker().abortCharingAttemptEvents.size());

        assertTrue(scenario.tracker().startChargingAttemptEvents.getFirst().isEnroute());

        assertEquals(1, scenario.tracker().chargingStartEvents.size());
        assertEquals(1, scenario.tracker().chargingEndEvents.size());
    }
}