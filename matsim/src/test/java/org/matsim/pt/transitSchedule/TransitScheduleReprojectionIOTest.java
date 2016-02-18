package org.matsim.pt.transitSchedule;

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
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class TransitScheduleReprojectionIOTest {
	private final String TEST_SCHEDULE = "test/scenarios/pt-tutorial/transitschedule.xml";

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReaderV1( originalScenario ).readFile( TEST_SCHEDULE );

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReaderV1( new Transformation() , reprojectedScenario ).readFile( TEST_SCHEDULE );

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	@Test
	public void testOutput() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReaderV1( originalScenario ).readFile( TEST_SCHEDULE );

		final String file = utils.getOutputDirectory()+"/schedule.xml";
		new TransitScheduleWriterV1( new Transformation() , originalScenario.getTransitSchedule() ).write( file );

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReaderV1( reprojectedScenario ).readFile( file );

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader( originalScenario ).readFile( TEST_SCHEDULE );

		final Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile( TEST_SCHEDULE );

		config.transit().setUseTransit( true );
		config.transit().setInputScheduleCRS( TransformationFactory.CH1903_LV03_GT );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem(  "EPSG:3857" );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();

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
		new TransitScheduleReader( dumpedScenario ).readFile( outputDirectory+"/output_transitSchedule.xml.gz" );

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getTransitSchedule().getFacilities().get( id ).getCoord();

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

	private void assertCorrectlyReprojected(
			final TransitSchedule originalSchedule,
			final TransitSchedule transformedSchedule) {
		Assert.assertEquals(
				"unexpected number of stops",
				originalSchedule.getFacilities().size(),
				transformedSchedule.getFacilities().size() );

		for ( Id<TransitStopFacility> stopId : originalSchedule.getFacilities().keySet() ) {
			final Coord original = originalSchedule.getFacilities().get( stopId ).getCoord();
			final Coord transformed = transformedSchedule.getFacilities().get( stopId ).getCoord();

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
	}

	private static class Transformation implements CoordinateTransformation {
		@Override
		public Coord transform(Coord coord) {
			return new Coord( coord.getX() + 1000 , coord.getY() + 1000 );
		}
	}
}
