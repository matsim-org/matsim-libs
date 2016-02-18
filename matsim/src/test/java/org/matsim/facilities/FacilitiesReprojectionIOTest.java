package org.matsim.facilities;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class FacilitiesReprojectionIOTest {
	private static final String TEST_FILE = "test/scenarios/chessboard/facilities.xml";

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testReprojectionAtImport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimFacilitiesReader( originalScenario ).readFile( TEST_FILE );
		new MatsimFacilitiesReader( new Transformation() , reprojectedScenario ).readFile( TEST_FILE );

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	@Test
	public void testReprojectionAtExport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).readFile( TEST_FILE );

		final String outFile = utils.getOutputDirectory()+"/facilities.xml.gz";

		new FacilitiesWriter( new Transformation() , originalScenario.getActivityFacilities() ).write( outFile );
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( reprojectedScenario ).readFile( outFile );

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).readFile( TEST_FILE );

		final Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile( TEST_FILE );

		config.facilities().setInputCRS(TransformationFactory.CH1903_LV03_GT );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem(  "EPSG:3857" );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<ActivityFacility> id : originalScenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assert.assertNotEquals(
					"No coordinates transform performed!",
					originalCoord.getX(),
					internalCoord.getX(),
					epsilon );
			Assert.assertNotEquals(
					"No coordinates transform performed!",
					originalCoord.getY(),
					internalCoord.getY(),
					epsilon );
		}

		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new MatsimFacilitiesReader( dumpedScenario ).readFile( outputDirectory+"/output_facilities.xml.gz" );

		for ( Id<ActivityFacility> id : originalScenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assert.assertEquals(
					"coordinates were not reprojected for dump",
					originalCoord.getX(),
					dumpedCoord.getX(),
					epsilon );
			Assert.assertEquals(
					"coordinates were not reprojected for dump",
					originalCoord.getY(),
					dumpedCoord.getY(),
					epsilon );
		}
	}

	private void assertScenarioReprojectedCorrectly(Scenario originalScenario, Scenario reprojectedScenario) {
		final ActivityFacilities originalFacilities = originalScenario.getActivityFacilities();
		final ActivityFacilities reprojectedFacilities = reprojectedScenario.getActivityFacilities();

		Assert.assertEquals(
				"unexpected size of reprojected facilities",
				originalFacilities.getFacilities().size(),
				reprojectedFacilities.getFacilities().size() );

		for (Id<ActivityFacility> id : originalFacilities.getFacilities().keySet() ) {
			final ActivityFacility originalFacility = originalFacilities.getFacilities().get( id );
			final ActivityFacility reprojectedFacility = reprojectedFacilities.getFacilities().get( id );

			assertReprojectedCorrectly( originalFacility , reprojectedFacility );
		}
	}

	private void assertReprojectedCorrectly(ActivityFacility originalFacility, ActivityFacility reprojectedFacility) {
		final Coord original = originalFacility.getCoord();
		final Coord transformed = reprojectedFacility.getCoord();

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
