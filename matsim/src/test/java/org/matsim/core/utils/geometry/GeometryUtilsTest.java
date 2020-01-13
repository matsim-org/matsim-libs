
/* *********************************************************************** *
 * project: org.matsim.*
 * GeometryUtilsTest.java
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

 /**
 * 
 */
package org.matsim.core.utils.geometry;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author kainagel
 *
 */
public class GeometryUtilsTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@SuppressWarnings("static-method")
	@Test
	public final void testIntersectingLinks() {
		
		Config config = ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL("equil"), "config.xml" ) ) ;
		
		final Network network = ScenarioUtils.loadScenario(config).getNetwork();

		{
			double xx = -15001 ;
			LineString testSegment = new GeometryFactory().createLineString(new Coordinate[]{new Coordinate(xx,0),new Coordinate(xx,10000)}) ;

			List<Link> results = GeometryUtils.findIntersectingLinks(testSegment, network);

			List<Id<Link>> expecteds = new ArrayList<>() ;
			expecteds.add( Id.createLinkId(1) ) ;

			Assert.assertEquals(expecteds.size(), results.size()) ;
			for ( int ii=0 ; ii<expecteds.size() ; ii++ ) {
				Assert.assertEquals( "wrong link id;", expecteds.get(ii), results.get(ii).getId() ) ;
			}
		}
		{
			double xx = -14001 ;
			LineString testSegment = new GeometryFactory().createLineString(new Coordinate[]{new Coordinate(xx,0),new Coordinate(xx,10000)}) ;

			List<Link> results = GeometryUtils.findIntersectingLinks(testSegment, network);

			List<Id<Link>> expecteds = new ArrayList<>() ;
			expecteds.add( Id.createLinkId(2) ) ;
			expecteds.add( Id.createLinkId(3) ) ;
			expecteds.add( Id.createLinkId(4) ) ;
			expecteds.add( Id.createLinkId(5) ) ;
			expecteds.add( Id.createLinkId(6) ) ;

			Assert.assertEquals(expecteds.size(), results.size()) ;
			for ( int ii=0 ; ii<expecteds.size() ; ii++ ) {
				Assert.assertEquals( expecteds.get(ii), results.get(ii).getId() ) ;
			}
		}
		
	}

}
