package org.matsim.contrib.perceivedsafety;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.ApplicationUtils;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
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

import static org.matsim.contrib.perceivedsafety.PerceivedSafetyScoringTest.createAndAddTestPopulation;

public class PerceivedSafetyAndBicycleContribTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	private static final Id<Person> PERSON_ID = Id.createPersonId("testPerson");
	private static final String BASIC = "matsimBasic";
	private static final String BICYCLE = "matsimBasicAndBicycleContrib";
	private static final String BICYCLE_AND_PSAFE = "matsimBicycleContribAndPerceivedSafetyContrib";

	@Test
	void testAndComparePerceivedSafetyScoringWithAndWithoutBicycleContrib() {
		Map<String, Double> actualLegScores = new HashMap<>();
		actualLegScores.put(BASIC, null);
		actualLegScores.put(BICYCLE, null);
		actualLegScores.put(BICYCLE_AND_PSAFE, null);

//		expected scores are calculated manually:
//		travel_dist: 2 links a 10km
//		travel_time: 6min
//		BIKE SCORE: infrastructureScore + comfortScore + gradientScore
//		infrastructureScore = marginalUtilityOfInfrastructure_m * (1. - infrastructureFactor) * distance_m;
//		= -0.0002 * (1-0.4) * 10000 = -1.2
//		comfortScore = marginalUtilityOfComfort_m * (1. - comfortFactor) * distance_m;
//		= -0.0002 * (1-0.4) * 10000 = -1.2
//		gradientScore = marginalUtilityOfGradient_pct_m * gradient_pct * distance_m;
//		= -0.0002 * 0 * 10000 = 0
//		= -2.4 * 2 = -4.8
//		PERCEIVED SAFETY SCORE: (betaPerceivedSafety + randomGaussian * sdPerceivedSafety) * distanceBasedPerceivedSafety;
//		distanceBasedPerceivedSafety = perceivedSafetyValueOnLink * distance / dMax
//		perceivedSafetyValueOnLink = varPerceivedSafety - threshold
//		= 1 - 4 = -3
//		= -3 * 10000 / 10000 = 10000; dMax = 0. (see above), so dMax will be set to linkLength
//		= (0.84 + 1.1419053154730547 * 0.22) * -3
//		= -3.2736575082122163; for first link
//		= -3.12680924632864; for second link (random double of 0.9194079489827879 is different to first link (see above) because the computeLinkBasedScore is called from different method (probably).
//		RESULTING IN A FINAL SCORE WITH BIKE CONTRIB + PERCEIVED SAFETY CONTRIB
//		= -11.200466754540857
		Map<String, Double> expectedLegScores = new HashMap<>();
		expectedLegScores.put(BASIC, 0.);
		expectedLegScores.put(BICYCLE, -4.8);
		expectedLegScores.put(BICYCLE_AND_PSAFE, -11.200466754540857);

		for (Map.Entry<String, Double> e : actualLegScores.entrySet()) {
			String setup = e.getKey();

			URL context = ExamplesUtils.getTestScenarioURL("equil");
			Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

			Path outputPath = Paths.get(utils.getOutputDirectory()).resolve(setup);

//########################################################################### prepare config #######################################################################
//			general config settings
			config.controller().setOutputDirectory(outputPath.toString());
			config.controller().setLastIteration(0);
			config.controller().setRunId("test");

			Set<String> mainModes = new HashSet<>(Set.of(TransportMode.bike));
			mainModes.addAll(config.qsim().getMainModes());
			config.qsim().setMainModes(mainModes);

			config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

			Set<String> networkModes = new HashSet<>(Set.of(TransportMode.bike));
			networkModes.addAll(config.routing().getNetworkModes());
			config.routing().setNetworkModes(networkModes);
			config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
			config.routing().removeTeleportedModeParams(TransportMode.bike);

			//            set all scoring params to 0 such that we only see the score change caused through perceived safety
			ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(TransportMode.bike);
			modeParams.setMarginalUtilityOfTraveling(0.);
			config.scoring().addModeParams(modeParams);
			config.scoring().setWriteExperiencedPlans(true);
			config.scoring().getActivityParams()
				.forEach(a -> a.setScoringThisActivityAtAll(false));

			switch (setup) {
				case BASIC -> {

				}
				case BICYCLE -> {
//					add bicycle contrib config group
					addAndConfigureBicycleConfigGroup(config);
				}
				case BICYCLE_AND_PSAFE -> {
//					add bicycle contrib config group
					addAndConfigureBicycleConfigGroup(config);
//					add perceivedSafetyCfgGroup and configure
					PerceivedSafetyConfigGroup perceivedSafetyConfigGroup = ConfigUtils.addOrGetModule(config, PerceivedSafetyConfigGroup.class);

//					values taken from E_BIKE in PerceivedSafetyUtils.fillConfigWithPerceivedSafetyDefaultValues
					PerceivedSafetyConfigGroup.PerceivedSafetyModeParams perceivedSafetyModeParams = perceivedSafetyConfigGroup.getOrCreatePerceivedSafetyModeParams(TransportMode.bike);
					perceivedSafetyModeParams.setMarginalUtilityOfPerceivedSafetyPerM(0.84);
					perceivedSafetyModeParams.setMarginalUtilityOfPerceivedSafetyPerMSd(0.22);
					perceivedSafetyModeParams.setDMaxPerM(0.);

					perceivedSafetyConfigGroup.addModeParams(perceivedSafetyModeParams);
					perceivedSafetyConfigGroup.setInputPerceivedSafetyThresholdPerM(4);
				}
				default -> throw new IllegalStateException("Unexpected value: " + setup);
			}

//########################################################################### prepare scenario #######################################################################
			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
			createAndAddTestPopulation(scenario, TransportMode.bike);

			switch (setup) {
				case BASIC -> {
					scenario.getNetwork().getLinks().values()
						.forEach(l -> {
//                        add mode as allowed mode
							Set<String> allowedModes = new HashSet<>();
							allowedModes.add(TransportMode.bike);
							allowedModes.addAll(l.getAllowedModes());
							l.setAllowedModes(allowedModes);
						});
				}
				case BICYCLE -> {
					scenario.getNetwork().getLinks().values()
						.forEach(l -> {
//                        add mode as allowed mode
							Set<String> allowedModes = new HashSet<>();
							allowedModes.add(TransportMode.bike);
							allowedModes.addAll(l.getAllowedModes());
							l.setAllowedModes(allowedModes);

//							add surface and speedfactor link attrs
							l.getAttributes().putAttribute(BicycleUtils.SURFACE, "cobblestone");
							BicycleUtils.setBicycleInfrastructureFactor(l, 1.0);
							NetworkUtils.setType(l, "tertiary");
						});
				}
				case BICYCLE_AND_PSAFE -> {
					scenario.getNetwork().getLinks().values()
						.forEach(l -> {
//                        add mode as allowed mode
							Set<String> allowedModes = new HashSet<>();
							allowedModes.add(TransportMode.bike);
							allowedModes.addAll(l.getAllowedModes());
							l.setAllowedModes(allowedModes);

//							add surface and speedfactor link attrs
							l.getAttributes().putAttribute(BicycleUtils.SURFACE, "cobblestone");
							BicycleUtils.setBicycleInfrastructureFactor(l, 1.0);
							NetworkUtils.setType(l, "tertiary");

//                        add perceived safety link attr to link
							l.getAttributes().putAttribute(TransportMode.bike + "PerceivedSafety", 1);
						});
				}
				default -> throw new IllegalStateException("Unexpected value: " + setup);
			}

//          create veh type for given mode(s)
			scenario.getConfig().routing().getNetworkModes()
				.forEach(m -> {
					VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create(m, VehicleType.class));
					vehicleType.setNetworkMode(m);
					scenario.getVehicles().addVehicleType(vehicleType);
				});

//########################################################################### prepare scenario #######################################################################
			Controler controler = new Controler(scenario);

			switch (setup) {
				case BASIC -> {

				}
				case BICYCLE -> {
					controler.addOverridingModule(new BicycleModule());
				}
				case BICYCLE_AND_PSAFE -> {
					controler.addOverridingModule(new BicycleModule());
					controler.addOverridingModule(new PerceivedSafetyModule());
				}
				default -> throw new IllegalStateException("Unexpected value: " + setup);
			}
			controler.run();

//            read experienced plans and get score of plan
			String experiencedPlansPath = ApplicationUtils.globFile(outputPath, "*output_experienced_plans.xml.gz").toString();
			Population experiencedPlans = PopulationUtils.readPopulation(experiencedPlansPath);

//            score of plan should only consist of leg scores as all act types are set to "dont score"
			actualLegScores.put(setup, experiencedPlans.getPersons().get(PERSON_ID).getSelectedPlan().getScore());
		}

		for (Map.Entry<String, Double> e : actualLegScores.entrySet()) {
			Assertions.assertEquals(expectedLegScores.get(e.getKey()), e.getValue());
		}

//		activation of bicycle contrib should decrease the score as we implement surface cobblestone
		Assertions.assertTrue(actualLegScores.get(BASIC) > actualLegScores.get(BICYCLE));
//		activation of perceived safety contrib should decrease the score even more as we implement a low level of perceived safety
		Assertions.assertTrue(actualLegScores.get(BICYCLE) > actualLegScores.get(BICYCLE_AND_PSAFE));
	}

	private static void addAndConfigureBicycleConfigGroup(Config config) {
		BicycleConfigGroup bicycleConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bicycleConfigGroup.setMarginalUtilityOfInfrastructure_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfComfort_m(-0.0002);
		bicycleConfigGroup.setMarginalUtilityOfGradient_pct_m(-0.0002);
		bicycleConfigGroup.setBicycleMode(TransportMode.bike);
	}
}
