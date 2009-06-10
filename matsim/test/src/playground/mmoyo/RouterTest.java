/* *********************************************************************** *
 * project: org.matsim.*
 * RouterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.mmoyo.PTCase2.PTOb;

public class RouterTest extends MatsimTestCase {

	private static final String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 

	private static final String CONFIG=  path  + "config.xml";
	private static final String ZURICHPTN= path + "network.xml";
	private static final String ZURICHPTTIMETABLE= path + "PTTimetable.xml";
	private static final String ZURICHPTPLANS= path + "plans.xml";
	private static final String OUTPUTPLANS= path + "output_plans.xml";

	public void test1() {
		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE,ZURICHPTPLANS, OUTPUTPLANS); 
		pt.readPTNet(ZURICHPTN);

		// searches and shows a PT path between two coordinates 
		Coord coord1 = new CoordImpl(747420, 262794);   
		Coord coord2 = new CoordImpl(685862, 254136);
		Path path2 = pt.getPtRouter2().findPTPath (coord1, coord2, 24372, 300);
		System.out.println(path2.links.size());
		for (Link link : path2.links){
			System.out.println(link.getId()+ ": " + link.getFromNode().getId() + " " + link.getType() + link.getToNode().getId() );
		}

		assertEquals( 31, path2.links.size() ) ;
		assertEquals( "1311" , path2.links.get(10).getId().toString() ) ;
		assertEquals( "250" , path2.links.get(20).getId().toString() ) ;
	}
}
