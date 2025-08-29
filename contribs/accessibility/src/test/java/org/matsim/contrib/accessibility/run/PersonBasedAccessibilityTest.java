package org.matsim.contrib.accessibility.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.accessibility.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.matsim.contrib.accessibility.run.TinyAccessibilityTest.createTestScenario;


// STEP 1: make accessibility calculation depend on home location, but not on person attributes
public class PersonBasedAccessibilityTest {

	private static final Logger LOG = LogManager.getLogger(PersonBasedAccessibilityTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void runFromEvents() {
		final Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);

		acg.setPersonBased(true);
		acg.setTileSize_m(100);
		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.teleportedWalk);

		for(Modes4Accessibility mode : Modes4Accessibility.values()) {
			acg.setComputingAccessibilityForMode(mode, accModes.contains(mode));
		}


		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(0.);


		double min = 0.; // Values for bounding box usually come from a config file
		double max = 200.;

		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromPopulation);
		acg.setBoundingBoxBottom(min).setBoundingBoxTop(max ).setBoundingBoxLeft(min).setBoundingBoxRight(max );
		acg.setUseParallelization(false);

		// ---

		final Scenario scenario = createTestScenario(config);

		// add test person

		addPerson(scenario, 100, 100, "young", 10);
		addPerson(scenario, 100, 100, "middle", 30);
		addPerson(scenario, 100, 100, "old", 90);
		addPerson(scenario, 100, 200, "extra1", 30);
		addPerson(scenario, 200, 100, "extra2", 30);


		// ---

		final String eventsFile = utils.getClassInputDirectory() + "output_events.xml.gz";

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , eventsFile );
		PersonBasedResultsComparator dataListener = new PersonBasedResultsComparator();
		builder.addDataListener(dataListener);
		builder.build().run() ;

		Map<Tuple<Person, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();



		Map<String, Double> personAccMap = accessibilitiesMap.entrySet()
			.stream()
			.collect(Collectors.toMap(
				entry -> entry.getKey().getFirst().getId().toString(),
				entry -> entry.getValue().get("teleportedWalk")
			));

		Assertions.assertEquals(personAccMap.get("testPerson_young"), personAccMap.get("testPerson_middle"));
		Assertions.assertEquals(personAccMap.get("testPerson_old"), personAccMap.get("testPerson_middle") - 10.);
		Assertions.assertEquals(personAccMap.get("testPerson_old"), personAccMap.get("testPerson_young") - 10.);

		System.out.println(personAccMap);

//
//		// print results
//		accessibilitiesMap.forEach( (personTime, mode2acc) -> {
//			Person person = personTime.getFirst();
//			Double time = personTime.getSecond();
//			LOG.info("Person " + person.getId() + " at time " + time);
//			mode2acc.forEach( (mode, acc) -> LOG.info("  mode: " + mode + " acc: " + acc ) );
//		});


	}

	private static void addPerson(Scenario scenario, double homeX, double homeY, String personId, int age) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("testPerson_" + personId));
		person.getAttributes().putAttribute("homeX", homeX);
		person.getAttributes().putAttribute("homeY", homeY);
		person.getAttributes().putAttribute("age", age);
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(
				scenario.getPopulation().getFactory().createActivityFromCoord("home", new Coord(homeX, homeY)));

		person.addPlan(plan);
		person.setSelectedPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private class PersonBasedResultsComparator implements PersonDataExchangeInterface {
		private final Map<Tuple<Person, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;

		@Override
		public void setPersonAccessibilities(Person person, Double timeOfDay, String mode, double accessibility) {
			Tuple<Person, Double> key = new Tuple<>(person, timeOfDay);
			if (!accessibilitiesMap.containsKey(key)) {
				Map<String,Double> accessibilitiesByMode = new HashMap<>();
				accessibilitiesMap.put(key, accessibilitiesByMode);
			}
			accessibilitiesMap.get(key).put(mode, accessibility);
		}

		public Map<Tuple<Person, Double>, Map<String, Double>> getAccessibilitiesMap() {
			return accessibilitiesMap;
		}

		@Override
		public void finish() {

		}
	}
}
