package org.matsim.core.mobsim.qsim;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;
import java.util.Random;

public class QsimTimingTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void test_with_analyzeRuntimes() {

		var config = ConfigUtils.createConfig();
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.controler().setRunId("runtime-test");

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
		config.qsim().setNumberOfThreads(4);
		config.qsim().setMainModes(List.of(TransportMode.car));
		config.qsim().setEndTime(86400);

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.plansCalcRoute().setNetworkModes(List.of(TransportMode.car));

		config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "start" ).setTypicalDuration( 200 ) );
		config.planCalcScore().addActivityParams( new PlanCalcScoreConfigGroup.ActivityParams( "end" ).setTypicalDuration( 300 ) );

		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");

		config.global().setNumberOfThreads(16);

		var scenario = ScenarioUtils.loadScenario(config);

		createRandomPopulation(scenario.getPopulation(), scenario.getNetwork());

		var controler = new Controler(scenario);
		controler.run();
	}

	private static void createRandomPopulation(Population population, Network network) {

		var bbox = NetworkUtils.getBoundingBox(network.getNodes().values());

		var minX = bbox[0];
		var minY = bbox[1];
		var width = bbox[2] - minX;
		var height = bbox[3] - minY;
		var rect = new Rect(minX, minY, width, height);
		var random = new Random();

		for (int i = 0; i < 50000; i++) {

			var plan = population.getFactory().createPlan();
			plan.addActivity(createStartActivity(random, rect, population.getFactory()));
			plan.addLeg(createLeg(population.getFactory()));
			plan.addActivity(createEndActivity(random, rect, population.getFactory()));

			var person = population.getFactory().createPerson(Id.createPersonId(i));
			person.addPlan(plan);

			population.addPerson(person);
		}
	}

	private static Activity createStartActivity(Random random, Rect bbox, PopulationFactory factory) {

		var x = bbox.minX + random.nextDouble() * bbox.width;
		var y = bbox.minY + random.nextDouble() * bbox.height;
		var coord = new Coord(x, y);
		var activity = factory.createActivityFromCoord("start", coord);

		// everyone is departing at 9am
		activity.setEndTime(32400);
		return activity;
	}

	private static Activity createEndActivity(Random random, Rect bbox, PopulationFactory factory) {

		var x = bbox.minX + random.nextDouble() * bbox.width;
		var y = bbox.minY + random.nextDouble() * bbox.height;
		var coord = new Coord(x, y);
		var activity = factory.createActivityFromCoord("end", coord);

		// everyone wants to arrive at 11am
		activity.setStartTime(39600);
		return activity;
	}

	private static Leg createLeg(PopulationFactory factory) {
		return factory.createLeg(TransportMode.car);
	}

	private static class Rect {
		public Rect(double minX, double minY, double width, double height) {
			this.minX = minX;
			this.minY = minY;
			this.width = width;
			this.height = height;
		}

		private final double minX;
		private final double minY;
		private final double width;
		private final double height;
	}
}
