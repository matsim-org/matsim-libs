/* *********************************************************************** *
 * project: org.matsim.*
 * MyZoneToZoneRouterTest.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.testcases.MatsimTestCase;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;


public class MyZoneToZoneRouterTest extends MatsimTestCase{
	private Scenario scenario;
	private List<MyZone> zones;
	private String inputFolder;
	
	public void testMyZoneToZoneRouterConstructor(){
		// TODO Test constructor. Want a routable network at this point.
		setupNetwork();
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(scenario, zones);
		assertNull("Router should be null.", mzzr.getRouter());
		
		mzzr.prepareTravelTimeData(inputFolder + "/10.events.txt.gz");
		assertNotNull("Router should exist.", mzzr.getRouter());
		assertEquals("Router should be of type Dijkstra.", Dijkstra.class, mzzr.getRouter().getClass());
	}
	
	public void testFindZoneToZoneTravelTime(){
		setupNetwork();
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		pr.readFile(inputFolder + "/output_plans.xml.gz");
		
		MyPlansProcessor mpp = new MyPlansProcessor(scenario, zones);
		mpp.processPlans();
		mpp.writeOdMatrixToDbf("/Users/johanwjoubert/Desktop/Temp/Equil/Dbf.dbf");
		DenseDoubleMatrix2D matrix = mpp.getOdMatrix();
		
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(scenario, zones);
		mzzr.prepareTravelTimeData(inputFolder + "/50.events.txt.gz");
		MyLinkStatsReader mlsr = new MyLinkStatsReader(inputFolder + "/50.linkstats.txt");
		DenseDoubleMatrix2D od = mzzr.processZones(matrix, mlsr.readSingleHour("6-7"));
//		mzzr.writeOdMatrixToDbf("/Users/johanwjoubert/Desktop/Temp/Equil/DbfFinal.dbf", od);
		assertEquals("Wrong intrazonal travel time for zone 1.", 435, Math.round(od.get(0, 0)));
		assertEquals("Wrong intrazonal travel time for zone 2.", 270, Math.round(od.get(1, 1)));
		assertEquals("Wrong intrazonal travel time for zone 3.", 270, Math.round(od.get(2, 2)));
		assertEquals("Wrong intrazonal travel time for zone 4.", 252, Math.round(od.get(3, 3)));
		assertEquals("Wrong intrazonal travel time for zone 5.", 660, Math.round(od.get(4, 4)));
		
		// TODO Add some more asserts for interzonal travel time.		
	}


	/**
	 * Creates polygons (MyZone) objects based on the equil example.
	 */
	private void setupNetwork(){

		inputFolder  = (new File(getInputDirectory())).getParentFile().getAbsolutePath();
		GeometryFactory gf = new GeometryFactory();
		zones = new ArrayList<MyZone>(5);
		// Zone 1.
		Coordinate c1 = new Coordinate(-25500, -10500);
		Coordinate c2 = new Coordinate(-9000, -10500);
		Coordinate c3 = new Coordinate(-9000, 500);
		Coordinate c4 = new Coordinate(-25500, 500);
		Coordinate[] ca1 = {c1,c2,c3,c4,c1};
		Polygon[] p1 = {gf.createPolygon(gf.createLinearRing(ca1), null)};
		MyZone z1 = new MyZone(p1, gf, new IdImpl("1"));
		zones.add(z1);

		// Zone 2.
		Coordinate c5 = new Coordinate(-9000, 3700);
		Coordinate c6 = new Coordinate(1000, 3700);
		Coordinate c7 = new Coordinate(1000, 6500);
		Coordinate c8 = new Coordinate(-9000, 6500);
		Coordinate[] ca2 = {c5,c6,c7,c8,c5};
		Polygon[] p2 = {gf.createPolygon(gf.createLinearRing(ca2), null)};
		MyZone z2 = new MyZone(p2, gf, new IdImpl("2"));
		zones.add(z2);

		// Zone 3.
		Coordinate c9 = new Coordinate(1000, 500);
		Coordinate[] ca3 = {c3,c9,c6,c5,c3};
		Polygon[] p3 = {gf.createPolygon(gf.createLinearRing(ca3), null)};
		MyZone z3 = new MyZone(p3, gf, new IdImpl("3"));
		zones.add(z3);

		// Zone 4.
		Coordinate c10 = new Coordinate(1000, -10500);
		Coordinate[] ca4 = {c2,c10,c9,c3,c2};
		Polygon[] p4 = {gf.createPolygon(gf.createLinearRing(ca4), null)};
		MyZone z4 = new MyZone(p4, gf, new IdImpl("4"));
		zones.add(z4);

		// Zone 5.
		Coordinate c11 = new Coordinate(5500, -10500);
		Coordinate c12 = new Coordinate(5500, 6500);
		Coordinate[] ca5 = {c10,c11,c12,c7,c10};
		Polygon[] p5 = {gf.createPolygon(gf.createLinearRing(ca5), null)};
		MyZone z5 = new MyZone(p5, gf, new IdImpl("5"));
		zones.add(z5);	

		scenario = new ScenarioImpl(); 
		// Read plans and network.
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
		nr.readFile(inputFolder + "/output_network.xml.gz");
	}
}