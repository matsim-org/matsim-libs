/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;


import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;

public class TourActivityStatusUpdaterWithTWImplTest extends VRPTestCase{
	
	Tour tour;
	
	Tour anotherTour;
	
	TourActivityStatusUpdater statusUpdater;
	
	@Override
	public void setUp(){
		initJobsInPlainCoordinateSystem();
		
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		Shipment s1 = createShipment("1", makeId(0,10), makeId(0,0));
		s1.setPickupTW(VrpUtils.createTimeWindow(8,12));
		
		Shipment s2 = createShipment("2", makeId(10,0), makeId(0,0));
		s2.setPickupTW(VrpUtils.createTimeWindow(30,30));
		
		tourBuilder.scheduleStart(makeId(0,0), 0.0, 0.0);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		tour = tourBuilder.build();
		
		statusUpdater = new TourActivityStatusUpdaterWithTWImpl(costs);
	}
	
	@Override
	public void tearDown(){
		
	}
	
	public void testCalculatedDistance(){
		statusUpdater.update(tour);
		assertEquals(40.0, tour.costs.distance);
	}

	public void testEarliestArrStart(){
		statusUpdater.update(tour);
		assertEquals(0.0,tour.getActivities().get(0).getEarliestArrTime());
	}
	
	public void testLatestArrStart(){
		statusUpdater.update(tour);
		assertEquals(0.0,tour.getActivities().get(0).getLatestArrTime());
	}
	
	public void testEarliestArrAtFirstPickup(){
		statusUpdater.update(tour);
		assertEquals(10.0,tour.getActivities().get(1).getEarliestArrTime());
	}
	
	public void testLatestArrAtFirstPickup(){
		statusUpdater.update(tour);
		assertEquals(10.0,tour.getActivities().get(1).getLatestArrTime());
	}
	
	public void testEarliestArrAtSecondPickup(){
		statusUpdater.update(tour);
		assertEquals(30.0,tour.getActivities().get(2).getEarliestArrTime());
	}
	
	public void testLatestArrAtSecondPickup(){
		statusUpdater.update(tour);
		assertEquals(30.0,tour.getActivities().get(2).getLatestArrTime());
	}
	
	public void testEarliestArrAtEnd(){
		statusUpdater.update(tour);
		assertEquals(40.0,tour.getActivities().get(5).getEarliestArrTime());
	}
	
	public void testLatestArrAtEnd(){
		statusUpdater.update(tour);
		assertEquals(Double.MAX_VALUE,tour.getActivities().get(5).getLatestArrTime());
	}
	
}
