
/* *********************************************************************** *
 * project: org.matsim.*
 * CountsReprojectionIOTest.java
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

 package org.matsim.counts;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
public class CountsReprojectionIOTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testInput() {
		final String file = utils.getOutputDirectory()+"/counts.xml";

		final Counts<Link> originalCounts = createDummyCounts();
		new CountsWriter( originalCounts ).write( file );

		final Counts<Link> reprojectedCounts = new Counts();
		new MatsimCountsReader( new Transformation() , reprojectedCounts ).readFile( file );

		assertCountsAreReprojectedCorrectly( originalCounts , reprojectedCounts );
	}

	 @Test
	 void testOutput() {
		final String file = utils.getOutputDirectory()+"/counts.xml";

		final Counts<Link> originalCounts = createDummyCounts();
		new CountsWriter( new Transformation() , originalCounts ).write( file );

		final Counts<Link> reprojectedCounts = new Counts();
		new MatsimCountsReader( reprojectedCounts ).readFile( file );

		assertCountsAreReprojectedCorrectly( originalCounts , reprojectedCounts );
	}

	 @Test
	 void testWithControlerAndConfigParameters() {
		final String file = utils.getOutputDirectory()+"/counts.xml";

		final Counts<Link> originalCounts = createDummyCounts();
		new CountsWriter( originalCounts ).write( file );

		// accept a rounding error of 1 cm.
		// this is used both to compare equality and non-equality, so the more we accept difference between input
		// and output coordinates, the more we require the internally reprojected coordinates to be different.
		// It is thus OK to use a reasonably "high" tolerance compared to usual double comparisons.
		final double epsilon = 0.01;

		final Config config = ConfigUtils.createConfig();

		config.counts().setInputFile( file );
		config.counts().setInputCRS( TransformationFactory.CH1903_LV03_Plus_GT );
		// web mercator. This would be a pretty silly choice for simulation,
		// but does not matter for tests. Just makes sure that (almost) every
		// coordinate can be projected
		config.global().setCoordinateSystem(  "EPSG:3857" );

		// TODO: test also with loading from Controler C'tor?
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		config.controller().setLastIteration( 0 );
		final String outputDirectory = utils.getOutputDirectory()+"/output/";
		config.controller().setOutputDirectory( outputDirectory );

		final Controler controler = new Controler( scenario );
		controler.run();

		final Counts<Link> internalCounts = controler.getInjector().getInstance( Key.get( new TypeLiteral<Counts<Link>>() {} ) );
		for ( Id<Link> id : originalCounts.getCounts().keySet() ) {
			final Coord originalCoord = originalCounts.getCount( id ).getCoord();
			final Coord internalCoord = internalCounts.getCount( id ).getCoord();

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


		final Counts<Link> dumpedCounts = new Counts<>();
		new MatsimCountsReader( dumpedCounts ).readFile( outputDirectory+"/output_counts.xml.gz" );

		for ( Id<Link> id : originalCounts.getCounts().keySet() ) {
			final Coord originalCoord = originalCounts.getCount( id ).getCoord();
			final Coord dumpedCoord = dumpedCounts.getCount( id ).getCoord();

			Assertions.assertEquals(
					originalCoord.getX(),
					dumpedCoord.getX(),
					epsilon,
					"coordinates were not reprojected for dump" );
			Assertions.assertEquals(
					originalCoord.getY(),
					dumpedCoord.getY(),
					epsilon,
					"coordinates were not reprojected for dump" );
		}
	}

	private void assertCountsAreReprojectedCorrectly(
			Counts<Link> originalCounts,
			Counts<Link> reprojectedCounts) {
		Assertions.assertEquals(
				originalCounts.getCounts().size(),
				reprojectedCounts.getCounts().size(),
				"unexpected number of counts" );

		for ( Id<Link> id : originalCounts.getCounts().keySet() ) {
			final Coord original = originalCounts.getCount( id ).getCoord();
			final Coord transformed = reprojectedCounts.getCount( id ).getCoord();

			Assertions.assertEquals(
					original.getX() + 1000 ,
					transformed.getX(),
					MatsimTestUtils.EPSILON,
					"wrong reprojected X value" );
			Assertions.assertEquals(
					original.getY() + 1000 ,
					transformed.getY(),
					MatsimTestUtils.EPSILON,
					"wrong reprojected Y value" );
		}
	}

	private Counts<Link> createDummyCounts() {
		final Counts<Link> counts = new Counts<Link>();

		counts.setYear( 1988 );

		for ( double i=0; i < 10; i+=1 ) {
			for ( double j=0; j < 10; j+=1 ) {
				final Count<Link> c = counts.createAndAddCount(
						Id.createLinkId( i+"-"+j ),
						i+"-"+j );
				c.setCoord( new Coord( i , j ) );
				c.createVolume( 1 , 0 );
			}
		}

		return counts;
	}

	private static class Transformation implements CoordinateTransformation {
		@Override
		public Coord transform(Coord coord) {
			double elevation;
			try{
				elevation = coord.getZ();
				return new Coord( coord.getX() + 1000 , coord.getY() + 1000 , elevation);
			} catch (Exception e){
				return new Coord( coord.getX() + 1000 , coord.getY() + 1000 );
			}
		}
	}
}
