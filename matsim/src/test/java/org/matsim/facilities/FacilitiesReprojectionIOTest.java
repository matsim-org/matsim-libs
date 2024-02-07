
/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesReprojectionIOTest.java
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

 package org.matsim.facilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

	/**
 * @author thibautd
 */
public class FacilitiesReprojectionIOTest {
	private static final String INITIAL_CRS = "EPSG:3857";
	private static final String TARGET_CRS = "WGS84";
	private static final CoordinateTransformation transformation =
			TransformationFactory.getCoordinateTransformation(
					INITIAL_CRS,
					TARGET_CRS);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testReprojectionAtImport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimFacilitiesReader( originalScenario ).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml"));
		new MatsimFacilitiesReader( INITIAL_CRS, TARGET_CRS, reprojectedScenario.getActivityFacilities() ).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml"));

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	 @Test
	 void testReprojectionAtExport() {
		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml"));

		final String outFile = utils.getOutputDirectory()+"/facilities.xml.gz";

		new FacilitiesWriter( transformation , originalScenario.getActivityFacilities() ).write( outFile );
		final Scenario reprojectedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( reprojectedScenario ).readFile( outFile );

		assertScenarioReprojectedCorrectly(originalScenario, reprojectedScenario);
	}

	 @Test
	 void testWithControlerAndObjectAttributes() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml"));

		ProjectionUtils.putCRS(originalScenario.getActivityFacilities(), INITIAL_CRS);
		new FacilitiesWriter(originalScenario.getActivityFacilities()).write(utils.getOutputDirectory()+"/facilities.xml");

		// write scenario and re-read it
		final Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile(utils.getOutputDirectory()+"/facilities.xml");
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<ActivityFacility> id : originalScenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					transformation.transform(originalCoord),
					internalCoord,
					"Wrong coordinate transform performed!");
		}

		Assertions.assertEquals(
				TARGET_CRS,
				ProjectionUtils.getCRS(scenario.getActivityFacilities()),
				"wrong CRS information after loading");

		config.controller().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controller().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new MatsimFacilitiesReader( dumpedScenario ).readFile( outputDirectory+"/output_facilities.xml.gz" );

		for ( Id<ActivityFacility> id : scenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord internalCoord = scenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assertions.assertEquals(
					internalCoord.getX(),
					dumpedCoord.getX(),
					epsilon,
					"coordinates were reprojected for dump" );
			Assertions.assertEquals(
					internalCoord.getY(),
					dumpedCoord.getY(),
					epsilon,
					"coordinates were reprojected for dump" );
		}
	}

	 @Test
	 void testWithControlerAndConfigParameters() {
		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		final Scenario originalScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader( originalScenario ).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml"));

		final Config config = ConfigUtils.createConfig();
		config.facilities().setInputFile( IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "facilities.xml").toString() );
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);

		config.facilities().setInputCRS(TransformationFactory.CH1903_LV03_GT );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem( TARGET_CRS );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		for ( Id<ActivityFacility> id : originalScenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord internalCoord = scenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assertions.assertNotEquals(
					originalCoord.getX(),
					internalCoord.getX(),
					epsilon,
					"No coordinates transform performed!" );
			Assertions.assertNotEquals(
					originalCoord.getY(),
					internalCoord.getY(),
					epsilon,
					"No coordinates transform performed!" );
		}

		config.controller().setLastIteration( -1 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controller().setOutputDirectory( outputDirectory );
		final Controler controler = new Controler( scenario );
		controler.run();

		final Scenario dumpedScenario = ScenarioUtils.createScenario( config );
		new MatsimFacilitiesReader( dumpedScenario ).readFile( outputDirectory+"/output_facilities.xml.gz" );

		for ( Id<ActivityFacility> id : originalScenario.getActivityFacilities().getFacilities().keySet() ) {
			final Coord originalCoord = originalScenario.getActivityFacilities().getFacilities().get( id ).getCoord();
			final Coord dumpedCoord = dumpedScenario.getActivityFacilities().getFacilities().get( id ).getCoord();

			Assertions.assertNotEquals(
					originalCoord.getX(),
					dumpedCoord.getX(),
					epsilon,
					"coordinates not reprojected for dump" );
			Assertions.assertNotEquals(
					originalCoord.getY(),
					dumpedCoord.getY(),
					epsilon,
					"coordinates not reprojected for dump" );
		}
	}

	private void assertScenarioReprojectedCorrectly(Scenario originalScenario, Scenario reprojectedScenario) {
		final ActivityFacilities originalFacilities = originalScenario.getActivityFacilities();
		final ActivityFacilities reprojectedFacilities = reprojectedScenario.getActivityFacilities();

		Assertions.assertEquals(
				originalFacilities.getFacilities().size(),
				reprojectedFacilities.getFacilities().size(),
				"unexpected size of reprojected facilities" );

		for (Id<ActivityFacility> id : originalFacilities.getFacilities().keySet() ) {
			final ActivityFacility originalFacility = originalFacilities.getFacilities().get( id );
			final ActivityFacility reprojectedFacility = reprojectedFacilities.getFacilities().get( id );

			assertReprojectedCorrectly( originalFacility , reprojectedFacility );
		}
	}

	private void assertReprojectedCorrectly(ActivityFacility originalFacility, ActivityFacility reprojectedFacility) {
		final Coord original = originalFacility.getCoord();
		final Coord transformed = reprojectedFacility.getCoord();

		Assertions.assertEquals(
				transformation.transform(original),
				transformed,
				"wrong reprojected coordinate");
	}
}
