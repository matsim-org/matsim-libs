package org.matsim.contrib.perceivedsafety;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.matsim.contrib.perceivedsafety.PerceivedSafetyUtils.E_BIKE;
import static org.matsim.contrib.perceivedsafety.PerceivedSafetyUtils.E_SCOOTER;

public class PerceivedSafetyScoringTest {

    @RegisterExtension
    MatsimTestUtils utils = new MatsimTestUtils();

    private static final String PERSON_ID = "testPerson";
	private static final Map<String, List<Double>> ACTUAL_LEG_SCORES = new HashMap<>();

    @Test
    public void testPerceivedSafetyScoring() {
//		expected scores come from the old approach, which is located at
//		https://github.com/panosgjuras/Psafe
        Map<String, Double> expectedLegScores = new HashMap<>();
        expectedLegScores.put(PERSON_ID + "_" + E_BIKE, -3.2736575082122163);
        expectedLegScores.put(PERSON_ID + "_" + E_SCOOTER, -2.5198001162493413);
        expectedLegScores.put(PERSON_ID + "_" + TransportMode.car, -2.005143189283833);

        for (Map.Entry<String, Double> e : expectedLegScores.entrySet()) {
            String mode = e.getKey().split("_")[1];
            URL context = ExamplesUtils.getTestScenarioURL("equil");
            Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

            Path outputPath = Paths.get(utils.getOutputDirectory()).resolve(mode);

//        general config settings
            config.controller().setOutputDirectory(outputPath.toString());
            config.controller().setLastIteration(0);
            config.controller().setRunId("test");

            Set<String> mainModes = new HashSet<>(Set.of(mode));
            mainModes.addAll(config.qsim().getMainModes());
            config.qsim().setMainModes(mainModes);

            config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

            Set<String> networkModes = new HashSet<>(Set.of(mode));
            networkModes.addAll(config.routing().getNetworkModes());
            config.routing().setNetworkModes(networkModes);

            //            set all scoring params to 0 such that we only see the score change caused through perceived safety
            ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(mode);
            modeParams.setMarginalUtilityOfTraveling(0.);
            config.scoring().addModeParams(modeParams);
            config.scoring().setWriteExperiencedPlans(true);
            config.scoring().getActivityParams()
                    .forEach(a -> a.setScoringThisActivityAtAll(false));

//      add perceivedSafetyCfgGroup and configure
            PerceivedSafetyConfigGroup perceivedSafetyConfigGroup = ConfigUtils.addOrGetModule(config, PerceivedSafetyConfigGroup.class);
            PerceivedSafetyUtils.fillConfigWithPerceivedSafetyDefaultValues(perceivedSafetyConfigGroup);

            MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
            createAndAddTestPopulation(scenario, mode, Id.createLinkId("20"), Id.createLinkId("21"));

//            add mode to network links
            scenario.getNetwork().getLinks().values()
                    .forEach(l -> {
//                        add mode as allowed mode
                        Set<String> allowedModes = new HashSet<>();
                        allowedModes.add(mode);
                        allowedModes.addAll(l.getAllowedModes());
                        l.setAllowedModes(allowedModes);

//                        add perceived safety link attr to link
                        l.getAttributes().putAttribute(mode + "PerceivedSafety", 1);
                    });

//          create veh type for given mode(s)
            scenario.getConfig().routing().getNetworkModes()
                    .forEach(m -> {
                        VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(m, VehicleType.class));
                        vehicleType.setNetworkMode(m);
                        scenario.getVehicles().addVehicleType(vehicleType);
                    });


            Controler controler = new Controler(scenario);
            controler.addOverridingModule(new PerceivedSafetyModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().to(PerceivedSafetyScoreEventHandler.class);
				}
			});
            controler.run();
        }

        for (Map.Entry<String, List<Double>> e : ACTUAL_LEG_SCORES.entrySet()) {
//			there should be 2 scores which are scored at LinkLeave link 20 and VehicleLeavesTraffic link 21
			Assertions.assertEquals(2, e.getValue().size());
//			we only want to compare the first value to the old approach.
//			the old approach did not score perceived safety correctly as it ignores the VehicleLeavesTrafficEvent at link 21, there is no LinkLeaveEvent when
//			the vehicle leaves traffic at the same link! -sm0426
            Assertions.assertEquals(expectedLegScores.get(e.getKey()), e.getValue().getFirst());
        }
    }

    private void createAndAddTestPopulation(MutableScenario scenario, String mode, Id<Link> startLinkId, Id<Link> endLinkId) {
        Population pop = PopulationUtils.createPopulation(scenario.getConfig());
        PopulationFactory fac = pop.getFactory();

        Activity home = fac.createActivityFromLinkId("h", startLinkId);
        home.setEndTime(8 * 3600.);
        Leg leg = fac.createLeg(mode);
        Activity work = fac.createActivityFromLinkId("w", endLinkId);
        work.setEndTime(9 * 3600.);

        Plan plan = fac.createPlan();
        plan.addActivity(home);
        plan.addLeg(leg);
        plan.addActivity(work);

        Person person = fac.createPerson(Id.createPersonId(PERSON_ID + "_" + mode));
        person.addPlan(plan);
        person.setSelectedPlan(plan);

        pop.addPerson(person);
        scenario.setPopulation(pop);
    }

	private static class PerceivedSafetyScoreEventHandler implements PersonScoreEventHandler {
		@Override
		public void handleEvent(PersonScoreEvent event) {
			ACTUAL_LEG_SCORES.putIfAbsent(event.getPersonId().toString(), new ArrayList<>());
			ACTUAL_LEG_SCORES.get(event.getPersonId().toString()).add(event.getAmount());
		}
	}
}
