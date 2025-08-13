package org.matsim.contrib.accessibility.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

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

		acg.setTileSize_m(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);

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

		addPerson(scenario, 200, 200, 0);
		addPerson(scenario, 100, 100, 1);
		addPerson(scenario, 100, 150, 2);


		// ---

		final String eventsFile = utils.getClassInputDirectory() + "output_events.xml.gz";

		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , eventsFile );
//		builder.addDataListener( new TinyAccessibilityTest.ResultsComparator() );
		builder.build().run() ;

	}

	private static void addPerson(Scenario scenario, double homeX, double homeY, int personId) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("testPerson" + personId));
		person.getAttributes().putAttribute("homeX", homeX);
		person.getAttributes().putAttribute("homeY", homeY);
		scenario.getPopulation().addPerson(person);
	}
}
