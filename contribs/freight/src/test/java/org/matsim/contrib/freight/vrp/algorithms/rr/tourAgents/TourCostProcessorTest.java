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

public class TourCostProcessorTest extends VRPTestCase{
	
	Tour tour;
	
	Tour anotherTour;
	
	TourCostProcessor statusUpdater;
	
	@Override
	public void setUp(){
		initJobsInPlainCoordinateSystem();
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		Shipment s1 = createShipment("1", makeId(0,0), makeId(0,10));
		Shipment s2 = createShipment("2", makeId(0,0), makeId(10,0));
		Shipment s3 = createShipment("3", makeId(0,9), makeId(10,0));
		
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		tour = tourBuilder.build();
		
		VrpTourBuilder anotherTourBuilder = new VrpTourBuilder();
		anotherTourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		anotherTourBuilder.schedulePickup(s3);
		anotherTourBuilder.scheduleDelivery(s3);
		anotherTourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		anotherTour = anotherTourBuilder.build();
		
		statusUpdater = new TourCostProcessor(costs);
	}
	
	@Override
	public void tearDown(){
		
	}
	
	public void testCalculatedDistance(){
		statusUpdater.process(tour);
		assertEquals(40.0, tour.costs.distance);
	}
	
	public void testCalculatedCosts(){
		statusUpdater.process(tour);
		assertEquals(40.0, tour.costs.generalizedCosts);
	}
	
	public void testCalculatedTime(){
		statusUpdater.process(tour);
		assertEquals(40.0, tour.costs.time);
	}
	
	public void testCalculatedDistanceForAnotherTour(){
		statusUpdater.process(anotherTour);
		assertEquals(38.0, anotherTour.costs.distance);
	}
	
	public void testCalculatedCostsForAnotherTour(){
		statusUpdater.process(anotherTour);
		assertEquals(38.0, anotherTour.costs.generalizedCosts);
	}
	
	public void testCalculatedTimeForAnotherTour(){
		statusUpdater.process(anotherTour);
		assertEquals(38.0, anotherTour.costs.time);
	}

}
