package org.matsim.contrib.perceivedsafety;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.perceivedsafety.PerceivedSafetyUtils.E_BIKE;
import static org.matsim.contrib.perceivedsafety.PerceivedSafetyUtils.E_SCOOTER;

public class PerceivedSafetyScoringTest {

    @RegisterExtension
    MatsimTestUtils utils = new MatsimTestUtils();

    private static final Id<Person> PERSON_ID = Id.createPersonId("testPerson");

    @Test
    public void testPerceivedSafetyScoring() {
//		expected cores come from the old approach, which is located at
//		https://github.com/panosgjuras/Psafe
        Map<String, Double> expectedLegScores = new HashMap<>();
        expectedLegScores.put(E_BIKE, -3.2736575082122163);
        expectedLegScores.put(E_SCOOTER, -2.5198001162493413);
        expectedLegScores.put(TransportMode.car, -2.005143189283833);

        Map<String, Double> actualLegScores = new HashMap<>();

        for (Map.Entry<String, Double> e : expectedLegScores.entrySet()) {
            String mode = e.getKey();
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
            PerceivedSafetyUtils.fillConfigWithBicyclePerceivedSafetyDefaultValues(perceivedSafetyConfigGroup);

            MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
            createAndAddTestPopulation(scenario, mode);

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
            controler.run();

//            read experienced plans and get score of plan
            String experiencedPlansPath = ApplicationUtils.globFile(outputPath, "*output_experienced_plans.xml.gz").toString();
            Population experiencedPlans = PopulationUtils.readPopulation(experiencedPlansPath);

//            score of plan should only consist of leg scores as all act types are set to "dont score"
            actualLegScores.put(e.getKey(), experiencedPlans.getPersons().get(PERSON_ID).getSelectedPlan().getScore());
        }

        for (Map.Entry<String, Double> e : actualLegScores.entrySet()) {
            Assertions.assertEquals(expectedLegScores.get(e.getKey()), e.getValue());
        }
    }

    private void createAndAddTestPopulation(MutableScenario scenario, String mode) {
        Population pop = PopulationUtils.createPopulation(scenario.getConfig());
        PopulationFactory fac = pop.getFactory();

        Activity home = fac.createActivityFromLinkId("h", Id.createLinkId("20"));
        home.setEndTime(8 * 3600.);
        Leg leg = fac.createLeg(mode);
        Activity work = fac.createActivityFromLinkId("w", Id.createLinkId("21"));
        work.setEndTime(9 * 3600.);

        Plan plan = fac.createPlan();
        plan.addActivity(home);
        plan.addLeg(leg);
        plan.addActivity(work);

        Person person = fac.createPerson(PERSON_ID);
        person.addPlan(plan);
        person.setSelectedPlan(plan);

        pop.addPerson(person);
        scenario.setPopulation(pop);
    }
}
