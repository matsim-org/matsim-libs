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

package org.matsim.core.utils.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author kainagel
 *
 */
public class GeometryUtilsTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	final void testIntersectingLinks() {

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL("equil"), "config.xml" ) ) ;

		final Network network = ScenarioUtils.loadScenario(config).getNetwork();

		{
			double xx = -15001 ;
			LineString testSegment = new GeometryFactory().createLineString(new Coordinate[]{new Coordinate(xx,0),new Coordinate(xx,10000)}) ;

			List<Link> results = GeometryUtils.findIntersectingLinks(testSegment, network);

			List<Id<Link>> expectedLinkIds = List.of(Id.createLinkId(1));

			Assertions.assertEquals(expectedLinkIds.size(), results.size()) ;
			for ( int ii=0 ; ii<expectedLinkIds.size() ; ii++ ) {
				Assertions.assertEquals( expectedLinkIds.get(ii), results.get(ii).getId(), "wrong link id" ) ;
			}
		}
		{
			double xx = -14001 ;
			LineString testSegment = new GeometryFactory().createLineString(new Coordinate[]{new Coordinate(xx,0),new Coordinate(xx,10000)}) ;

			List<Link> results = GeometryUtils.findIntersectingLinks(testSegment, network);

			Set<Id<Link>> intersectingLinkIds = new HashSet<>();
			for (Link link : results) {
				intersectingLinkIds.add(link.getId());
			}

			List<Id<Link>> expectedIds = List.of(Id.createLinkId(2), Id.createLinkId(3), Id.createLinkId(4), Id.createLinkId(5), Id.createLinkId(6));

			Assertions.assertEquals(expectedIds.size(), results.size());
			for (Id<Link> id : expectedIds) {
				Assertions.assertTrue(intersectingLinkIds.contains(id), "expected link " + id);
			}
		}

	}

}
