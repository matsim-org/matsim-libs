package org.matsim.pt.transitSchedule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.URL;

/**
 * @author thibautd
 */
public class TransitScheduleReprojectionIOTest {
	private static final String INITIAL_CRS = TransformationFactory.CH1903_LV03_GT;
	private static final String TARGET_CRS = "EPSG:3857";
	private static final CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
					INITIAL_CRS,
					TARGET_CRS);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInput() {
		URL transitSchedule = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitschedule.xml");
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader( originalScenario ).readURL(transitSchedule);

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(INITIAL_CRS, TARGET_CRS, reprojectedScenario).readURL(transitSchedule);

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	@Test
	public void testOutput() {
		URL transitSchedule = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitschedule.xml");
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(originalScenario).readURL(transitSchedule);

		final String file = utils.getOutputDirectory()+"/schedule.xml";
		new TransitScheduleWriterV1( transformation , originalScenario.getTransitSchedule() ).write( file );

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(reprojectedScenario).readFile(file);

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	@Test
	public void testWithControlerAndConfigParameters() {
		final Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("pt-tutorial"));

		final Scenario originalScenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader( originalScenario ).readURL(ConfigGroup.getInputFileURL(config.getContext(), "transitschedule.xml"));

		config.transit().setTransitScheduleFile("transitschedule.xml");

		config.transit().setUseTransit( true );
		config.transit().setInputScheduleCRS( INITIAL_CRS );
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assert.assertEquals(
					"No coordinates transform performed!",
					transformation.transform(originalCoord),
					internalCoord);
		}

		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new TransitScheduleReader( dumpedScenario ).readFile( outputDirectory+"/output_transitSchedule.xml.gz" );

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assert.assertEquals(
					"coordinates were reprojected for dump",
					internalCoord,
					dumpedCoord);
		}
	}

	@Test
	public void testWithControlerAndAttributes() {
		final Config config = ConfigUtils.createConfig(ExamplesUtils.getTestScenarioURL("pt-tutorial"));

		final Scenario originalScenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader( originalScenario ).readURL(ConfigGroup.getInputFileURL(config.getContext(), "transitschedule.xml"));

		final String withAttributes = new File(utils.getOutputDirectory()).getAbsolutePath()+"/transitschedule.xml";
		ProjectionUtils.putCRS(originalScenario.getTransitSchedule(), INITIAL_CRS);
		new TransitScheduleWriter(originalScenario.getTransitSchedule()).writeFile(withAttributes);

		config.transit().setTransitScheduleFile(withAttributes);

		config.transit().setUseTransit( true );
		config.transit().setInputScheduleCRS( INITIAL_CRS );
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assert.assertEquals(
					"No coordinates transform performed!",
					transformation.transform(originalCoord),
					internalCoord);
		}

		Assert.assertEquals(
				"wrong CRS information after loading",
				TARGET_CRS,
				ProjectionUtils.getCRS(scenario.getTransitSchedule()));

		config.controler().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controler().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new TransitScheduleReader( dumpedScenario ).readFile( outputDirectory+"/output_transitSchedule.xml.gz" );

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assert.assertEquals(
					"coordinates were reprojected for dump",
					internalCoord,
					dumpedCoord);
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
					transformation.transform(original),
					transformed);
		}
	}

}
