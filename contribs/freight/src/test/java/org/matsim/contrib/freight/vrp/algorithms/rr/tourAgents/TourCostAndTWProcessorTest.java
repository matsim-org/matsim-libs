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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.freight.vrp.algorithms.rr.TDCosts;
import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;

public class TourCostAndTWProcessorTest extends VRPTestCase{
	
	static class MyLocations implements Locations{

		private Map<String,Coordinate> locations = new HashMap<String, Coordinate>();

		public void addLocation(String id, Coordinate coord){
			locations.put(id, coord);
		}

		@Override
		public Coordinate getCoord(String id) {
			return locations.get(id);
		}
	}
	
	Tour tour;
	
	Tour anotherTour;
	
	TourStatusProcessor statusUpdater;
	
	TourStatusProcessor tdTourStatusProcessor;
	
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
		
		statusUpdater = new TourCostAndTWProcessor(costs);
		
		MyLocations loc = new MyLocations();
		loc.addLocation(makeId(0,10),new Coordinate(0,10));
		loc.addLocation(makeId(10,0),new Coordinate(10,0));
		loc.addLocation(makeId(0,0),new Coordinate(0,0));
		
		double depotClosingTime = 100.0;
		List<Double> timeBins = new ArrayList<Double>();
		timeBins.add(0.2*depotClosingTime);
		timeBins.add(0.4*depotClosingTime);
		timeBins.add(0.6*depotClosingTime);
		timeBins.add(0.8*depotClosingTime);
		timeBins.add(1.0*depotClosingTime);
		
		List<Double> speedValues = new ArrayList<Double>();
		speedValues.add(1.0);
		speedValues.add(2.0);
		speedValues.add(1.0);
		speedValues.add(2.0);
		speedValues.add(1.0);
		
		TDCosts tdCosts = new TDCosts(loc, timeBins, speedValues);
		tdTourStatusProcessor = new TourCostAndTWProcessor(tdCosts);
	}
	
	public void testCalculatedDistance(){
		statusUpdater.process(tour);
		assertEquals(40.0, tour.costs.distance);
	}
	
	public void testCalculatedTimeWithTDCost(){
		tdTourStatusProcessor.process(tour);
		assertEquals((10.0+10.0+5.0+5.0), tour.costs.time);
	}
	
	public void testCalculatedDistanceWithTDCost(){
		tdTourStatusProcessor.process(tour);
		assertEquals(40.0, tour.costs.distance);
	}

	public void testEarliestArrStart(){
		statusUpdater.process(tour);
		assertEquals(0.0,tour.getActivities().get(0).getEarliestArrTime());
	}
	
	public void testLatestArrStart(){
		statusUpdater.process(tour);
		assertEquals(0.0,tour.getActivities().get(0).getLatestArrTime());
	}
	
	public void testEarliestArrAtFirstPickup(){
		statusUpdater.process(tour);
		assertEquals(10.0,tour.getActivities().get(1).getEarliestArrTime());
	}
	
	public void testEarliestArrAtFirstPickupWithTDCost(){
		tdTourStatusProcessor.process(tour);
		assertEquals(10.0,tour.getActivities().get(1).getEarliestArrTime());
	}
	
	public void testLatestArrAtFirstPickup(){
		statusUpdater.process(tour);
		assertEquals(10.0,tour.getActivities().get(1).getLatestArrTime());
	}
	
	public void testLatestArrAtFirstPickupWithTDCost(){
		tdTourStatusProcessor.process(tour);
		assertEquals(12.0,tour.getActivities().get(1).getLatestArrTime());
	}
	
	public void testEarliestArrAtSecondPickup(){
		statusUpdater.process(tour);
		assertEquals(30.0,tour.getActivities().get(2).getEarliestArrTime());
	}
	
	public void testEarliestArrAtSecondPickupWithTDCosts(){
		tdTourStatusProcessor.process(tour);
		assertEquals(30.0,tour.getActivities().get(2).getEarliestArrTime());
	}
	
	public void testLatestArrAtSecondPickup(){
		statusUpdater.process(tour);
		assertEquals(30.0,tour.getActivities().get(2).getLatestArrTime());
	}
	
	public void testLatestArrAtSecondPickupWithTDCosts(){
		tdTourStatusProcessor.process(tour);
		assertEquals(30.0,tour.getActivities().get(2).getLatestArrTime());
	}
	
	public void testEarliestArrAtEnd(){
		statusUpdater.process(tour);
		assertEquals(40.0,tour.getActivities().get(5).getEarliestArrTime());
	}
	
	public void testEarliestArrAtEndWithTDCosts(){
		tdTourStatusProcessor.process(tour);
		assertEquals(35.0,tour.getActivities().get(5).getEarliestArrTime());
	}
	
	public void testLatestArrAtEnd(){
		statusUpdater.process(tour);
		assertEquals(Double.MAX_VALUE,tour.getActivities().get(5).getLatestArrTime());
	}
	
	public void testLatestArrAtEndWithTDCosts(){
		tdTourStatusProcessor.process(tour);
		assertEquals(Double.MAX_VALUE,tour.getActivities().get(5).getLatestArrTime());
	}
	

}
