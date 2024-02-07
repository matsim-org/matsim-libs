
/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReprojectionIOTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.pt.transitSchedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
	private static final Logger log = LogManager.getLogger( TransitScheduleReprojectionIOTest.class ) ;

	private static final String INITIAL_CRS = TransformationFactory.CH1903_LV03_GT;
	private static final String TARGET_CRS = "EPSG:3857";
	private static final CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
					INITIAL_CRS,
					TARGET_CRS);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testInput() {
		URL transitSchedule = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitschedule.xml");
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader( originalScenario ).readURL(transitSchedule );

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(INITIAL_CRS, TARGET_CRS, reprojectedScenario).readURL(transitSchedule );

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	 @Test
	 void testOutput() {
		URL transitSchedule = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "transitschedule.xml");
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(originalScenario).readURL(transitSchedule );

		final String file = utils.getOutputDirectory()+"/schedule.xml";
		new TransitScheduleWriterV1( transformation , originalScenario.getTransitSchedule() ).write( file );

		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(reprojectedScenario).readFile(file);

		assertCorrectlyReprojected( originalScenario.getTransitSchedule() , reprojectedScenario.getTransitSchedule() );
	}

	 @Test
	 void testWithControlerAndConfigParameters() {
		// read transitschedule.xml into empty scenario:
		Scenario originalScenario ;
		{
			final Config config0 = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			originalScenario = ScenarioUtils.createScenario( config0 );
			new TransitScheduleReader( originalScenario ).readURL( ConfigGroup.getInputFileURL( config0.getContext(), "transitschedule.xml" ) );
		}

		final String outputDirectory = utils.getOutputDirectory()+"/output/";

		// read same thing via scenario loader:
		Scenario scenario ;
		{
			final Config config = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			config.transit().setTransitScheduleFile( "transitschedule.xml" );
			config.transit().setUseTransit( true );
			config.transit().setInputScheduleCRS( INITIAL_CRS );
			config.global().setCoordinateSystem( TARGET_CRS );
			config.controller().setLastIteration( -1 );
			config.controller().setOutputDirectory( outputDirectory );
			config.network().setInputFile("multimodalnetwork.xml");
			scenario = ScenarioUtils.loadScenario( config );
		}

		// TODO: test also with loading from Controler C'tor?

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					transformation.transform(originalCoord),
					internalCoord,
					"No coordinates transform performed!");
		}

		final Controler controler = new Controler( scenario );
		controler.run();

		Scenario dumpedScenario ;
		{
			final Config config = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			dumpedScenario = ScenarioUtils.createScenario( config );
		}
		new TransitScheduleReader( dumpedScenario ).readFile( outputDirectory+"/output_transitSchedule.xml.gz" );

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					internalCoord,
					dumpedCoord,
					"coordinates were reprojected for dump");
		}
	}

	 @Test
	 void testWithControlerAndAttributes() {
		// read transit schedule into empty scenario:
		Scenario originalScenario ;
		{
			final Config config = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			originalScenario = ScenarioUtils.createScenario( config );
			new TransitScheduleReader( originalScenario ).readURL( ConfigGroup.getInputFileURL( config.getContext(), "transitschedule.xml" ) );
		}

		final String outputDirectory = utils.getOutputDirectory()+"/output/";

		// same thing via scenario loader,
		Scenario scenario ;
		{

			// add CRS to file, and write it to file:
			ProjectionUtils.putCRS( originalScenario.getTransitSchedule(), INITIAL_CRS );
			final String withAttributes = new File( utils.getOutputDirectory() ).getAbsolutePath() + "/transitschedule.xml";
			// (need the absolute path since later it is put into the config, and that will otherwise be relative to some other context. kai, sep'18)
			new TransitScheduleWriter( originalScenario.getTransitSchedule() ).writeFile( withAttributes );

			final Config config = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			config.transit().setTransitScheduleFile( withAttributes );
			config.transit().setUseTransit( true );
			config.transit().setInputScheduleCRS( INITIAL_CRS );
			config.network().setInputFile("multimodalnetwork.xml");
			// yyyyyy Is it so plausible that this is given here when the test is about having this in the file? kai, sep'18
			config.global().setCoordinateSystem( TARGET_CRS );
			config.controller().setLastIteration( -1 );
			config.controller().setOutputDirectory( outputDirectory );

			log.info( "" ) ;
			log.info("just before we are getting the exception:") ;
			log.info( "context=" + config.getContext() ) ;
			log.info( "transitScheduleFilename=" + withAttributes ) ;
			log.info("") ;

			// TODO: test also with loading from Controler C'tor?
			scenario = ScenarioUtils.loadScenario( config );
		}
		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					transformation.transform(originalCoord),
					internalCoord,
					"No coordinates transform performed!");
		}

		Assertions.assertEquals(
				TARGET_CRS,
				ProjectionUtils.getCRS(scenario.getTransitSchedule()),
				"wrong CRS information after loading");

		final Controler controler = new Controler( scenario );
		controler.run();

		Scenario dumpedScenario ;
		{
			final Config config = ConfigUtils.createConfig( ExamplesUtils.getTestScenarioURL( "pt-tutorial" ) );
			dumpedScenario=ScenarioUtils.createScenario( config );
		}
		new TransitScheduleReader( dumpedScenario ).readFile( outputDirectory+"/output_transitSchedule.xml.gz" );

		for ( Id<TransitStopFacility> id : originalScenario.getTransitSchedule().getFacilities().keySet() ) {
			final Coord internalCoord = scenario.getTransitSchedule().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getTransitSchedule().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					internalCoord,
					dumpedCoord,
					"coordinates were reprojected for dump");
		}
	}

	private void assertCorrectlyReprojected(
			final TransitSchedule originalSchedule,
			final TransitSchedule transformedSchedule) {
		Assertions.assertEquals(
				originalSchedule.getFacilities().size(),
				transformedSchedule.getFacilities().size(),
				"unexpected number of stops" );

		for ( Id<TransitStopFacility> stopId : originalSchedule.getFacilities().keySet() ) {
			final Coord original = originalSchedule.getFacilities().get( stopId ).getCoord();
			final Coord transformed = transformedSchedule.getFacilities().get( stopId ).getCoord();

			Assertions.assertEquals(
					transformation.transform(original),
					transformed,
					"wrong reprojected X value");
		}
	}

}
