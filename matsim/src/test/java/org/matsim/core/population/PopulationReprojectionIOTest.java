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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
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
	@Ignore( "dtd does not exist anymore" )
	public void testInput_V0() {
		final String testFile = utils.getOutputDirectory()+"/plans.xml.gz";

		// create test file in V0 format
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		// necessary for v4...
		new MatsimNetworkReader( scenario.getNetwork() ).readFile( NET_FILE );
		new MatsimPopulationReader( scenario ).readFile( BASE_FILE );
		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).writeFileV0( testFile );

		testConversionAtInput( testFile );
	}

	@Test
	public void testInput_V4() {
		final String testFile = utils.getOutputDirectory()+"/plans.xml.gz";

		// create test file in V0 format
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		// necessary for v4...
		new MatsimNetworkReader( scenario.getNetwork() ).readFile( NET_FILE );
		new MatsimPopulationReader( scenario ).readFile( BASE_FILE );
		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).writeFileV4( testFile );

		testConversionAtInput( testFile );
	}

	@Test
	public void testInput_V5() {
		final String testFile = utils.getOutputDirectory()+"/plans.xml.gz";

		// create test file in V0 format
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		// necessary for v4...
		new MatsimNetworkReader( scenario.getNetwork() ).readFile( NET_FILE );
		new MatsimPopulationReader( scenario ).readFile( BASE_FILE );
		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).writeFileV5( testFile );

		testConversionAtInput( testFile );
	}

	public void testConversionAtInput( final String inputFile ) {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// necessary for v4...
		new MatsimNetworkReader( originalScenario.getNetwork() ).readFile( NET_FILE );
		new MatsimNetworkReader( reprojectedScenario.getNetwork() ).readFile( NET_FILE );

		new MatsimPopulationReader( originalScenario ).readFile( inputFile );
		new MatsimPopulationReader( new Transformation() , reprojectedScenario ).readFile( inputFile );

		final Population originalPopulation = originalScenario.getPopulation();
		final Population reprojectedPopulation = reprojectedScenario.getPopulation();

		Assert.assertEquals(
				"unexpected size of reprojected population",
				originalPopulation.getPersons().size(),
				reprojectedPopulation.getPersons().size() );

		for ( Id<Person> personId : originalPopulation.getPersons().keySet() ) {
			final Person originalPerson = originalPopulation.getPersons().get( personId );
			final Person reprojectedPerson = reprojectedPopulation.getPersons().get( personId );

			assertPlanCorrectlyTransformed( originalPerson.getSelectedPlan() , reprojectedPerson.getSelectedPlan() );
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
