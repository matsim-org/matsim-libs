package org.matsim.core.population;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Iterator;
import java.util.List;

/**
 * @author thibautd
 */
public class PopulationReprojectionIOTest {
	private static final String NET_FILE = "test/scenarios/berlin/network.xml.gz";
	private static final String BASE_FILE = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore("dtd does not exist anymore")
	public void testInput_V0() {
		final String testFile = utils.getOutputDirectory() + "/plans.xml.gz";

		// create test file in V0 format
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(scenario).readFile(BASE_FILE);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV0(testFile);

		testConversionAtInput(testFile);
	}

	@Test
	public void testInput_V4() {
		final String testFile = utils.getOutputDirectory() + "/plans.xml.gz";

		// create test file in V4 format
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(scenario).readFile(BASE_FILE);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(testFile);

		testConversionAtInput(testFile);
	}

	@Test
	public void testInput_V5() {
		final String testFile = utils.getOutputDirectory() + "/plans.xml.gz";

		// create test file in V5 format
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(scenario).readFile(BASE_FILE);
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV5(testFile);

		testConversionAtInput(testFile);
	}

	@Test
	public void testOutput_V4() {
		final String testFile = utils.getOutputDirectory() + "/plans.xml.gz";

		// read test population
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(originalScenario).readFile(BASE_FILE);

		// write test population with conversion
		new PopulationWriter(
				new Transformation(),
				originalScenario.getPopulation(),
				originalScenario.getNetwork()).writeFileV4( testFile );

		// read converted population
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(reprojectedScenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(reprojectedScenario).readFile(testFile);

		assertPopulationCorrectlyTransformed( originalScenario.getPopulation() , reprojectedScenario.getPopulation() );
	}

	@Test
	public void testOutput_V5() {
		final String testFile = utils.getOutputDirectory() + "/plans.xml.gz";

		// read test population
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(originalScenario).readFile(BASE_FILE);

		// write test population with conversion
		new PopulationWriter(
				new Transformation(),
				originalScenario.getPopulation(),
				originalScenario.getNetwork()).writeFileV5( testFile );

		// read converted population
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(reprojectedScenario).readFile(testFile);

		assertPopulationCorrectlyTransformed( originalScenario.getPopulation() , reprojectedScenario.getPopulation() );
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		// read test population
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).readFile(NET_FILE);
		new MatsimPopulationReader(originalScenario).readFile(BASE_FILE);
		final Config config = ConfigUtils.createConfig();

		// specify config
		// need to reproject network as well...
		config.network().setInputCRS( TransformationFactory.DHDN_GK4 );
		config.plans().setInputCRS( TransformationFactory.DHDN_GK4 );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem(  "EPSG:3857" );

		config.network().setInputFile( NET_FILE );
		config.plans().setInputFile( BASE_FILE );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person originalPerson = originalScenario.getPopulation().getPersons().get( id );
			final Person internalPerson = scenario.getPopulation().getPersons().get( id );

			final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( internalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					originalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> originalIterator = originalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( originalIterator.hasNext() ) {
				final Activity o = originalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertNotEquals(
						"No coordinates transform performed!",
						o.getCoord().getX(),
						r.getCoord().getX(),
						epsilon );
				Assert.assertNotEquals(
						"No coordinates transform performed!",
						o.getCoord().getY(),
						r.getCoord().getY(),
						epsilon );
			}
		}

		// do not perform ANY mobsim run
		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new MatsimPopulationReader( dumpedScenario ).readFile( outputDirectory+"/output_plans.xml.gz" );

		for ( Id<Person> id : originalScenario.getPopulation().getPersons().keySet() ) {
			final Person originalPerson = originalScenario.getPopulation().getPersons().get( id );
			final Person dumpedPerson = dumpedScenario.getPopulation().getPersons().get( id );

			final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
			final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( dumpedPerson.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );

			Assert.assertEquals(
					"unexpected number of activities in reprojected plan",
					originalActivities.size(),
					reprojectedActivities.size() );

			final Iterator<Activity> originalIterator = originalActivities.iterator();
			final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

			while ( originalIterator.hasNext() ) {
				final Activity o = originalIterator.next();
				final Activity r = reprojectedIterator.next();

				Assert.assertEquals(
						"coordinates were not reprojected for dump",
						o.getCoord().getX(),
						r.getCoord().getX(),
						epsilon );
				Assert.assertEquals(
						"coordinates were not reprojected for dump",
						o.getCoord().getY(),
						r.getCoord().getY(),
						epsilon );
			}
		}
	}

	public void testConversionAtInput(final String inputFile) {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// necessary for v4...
		new MatsimNetworkReader(originalScenario.getNetwork()).readFile(NET_FILE);
		new MatsimNetworkReader(reprojectedScenario.getNetwork()).readFile(NET_FILE);

		new MatsimPopulationReader(originalScenario).readFile(inputFile);
		new MatsimPopulationReader(new Transformation(), reprojectedScenario).readFile(inputFile);

		final Population originalPopulation = originalScenario.getPopulation();
		final Population reprojectedPopulation = reprojectedScenario.getPopulation();

		assertPopulationCorrectlyTransformed( originalPopulation , reprojectedPopulation );
	}

	private void assertPopulationCorrectlyTransformed(
			final Population originalPopulation,
			final Population reprojectedPopulation) {
		Assert.assertEquals(
				"unexpected size of reprojected population",
				originalPopulation.getPersons().size(),
				reprojectedPopulation.getPersons().size());

		for (Id<Person> personId : originalPopulation.getPersons().keySet()) {
			final Person originalPerson = originalPopulation.getPersons().get(personId);
			final Person reprojectedPerson = reprojectedPopulation.getPersons().get(personId);

			assertPlanCorrectlyTransformed(originalPerson.getSelectedPlan(), reprojectedPerson.getSelectedPlan());
		}
	}

	private void assertPlanCorrectlyTransformed(
			final Plan originalPlan,
			final Plan reprojectedPlan) {
		final List<Activity> originalActivities = TripStructureUtils.getActivities( originalPlan , EmptyStageActivityTypes.INSTANCE );
		final List<Activity> reprojectedActivities = TripStructureUtils.getActivities( reprojectedPlan , EmptyStageActivityTypes.INSTANCE );

		Assert.assertEquals(
				"unexpected number of activities in reprojected plan",
				originalActivities.size(),
				reprojectedActivities.size() );

		final Iterator<Activity> originalIterator = originalActivities.iterator();
		final Iterator<Activity> reprojectedIterator = reprojectedActivities.iterator();

		while ( originalIterator.hasNext() ) {
			final Activity o = originalIterator.next();
			final Activity r = reprojectedIterator.next();

			assertIsCorrectlyTransformed( o.getCoord() , r.getCoord() );
		}
	}

	private void assertIsCorrectlyTransformed( final Coord original , final Coord transformed ) {
		Assert.assertEquals(
				"wrong reprojected X value",
				original.getX() + 1000 ,
				transformed.getX(),
				MatsimTestUtils.EPSILON );
		Assert.assertEquals(
				"wrong reprojected Y value",
				original.getY() + 1000 ,
				transformed.getY(),
				MatsimTestUtils.EPSILON );
	}

	private static class Transformation implements CoordinateTransformation {
		@Override
		public Coord transform(Coord coord) {
			return new Coord( coord.getX() + 1000 , coord.getY() + 1000 );
		}
	}
}
