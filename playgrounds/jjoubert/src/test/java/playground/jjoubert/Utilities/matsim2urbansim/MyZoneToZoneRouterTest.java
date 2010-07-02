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

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.testcases.MatsimTestCase;


public class MyZoneToZoneRouterTest extends MatsimTestCase{
	private Scenario scenario;
	private List<MyZone> zones;
	
	public void testMyZoneToZoneRouterConstructor(){
		// TODO Test constructor. Want a routable network at this point.
		setupTest();
		MyZoneToZoneRouter mzzr = new MyZoneToZoneRouter(scenario, zones);
	}
	
	public void testFindZoneToZoneTravelTime(){
		// TODO Test of we can successfully find the inter-zonal travel time.
	}
	
	public void testFindIntraZonalTravelTime(){
		// TODO Test if we can successfully find the intra-zonal travel time.
	}
	
	private void setupTest(){
		
	}
	
	
	
}
