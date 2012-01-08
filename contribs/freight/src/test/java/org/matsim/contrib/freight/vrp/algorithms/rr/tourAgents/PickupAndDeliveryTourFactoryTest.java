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
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.TDCosts;
import org.matsim.contrib.freight.vrp.algorithms.rr.VRPTestCase;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostAndTWProcessorTest.MyLocations;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpTourBuilder;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;

public class PickupAndDeliveryTourFactoryTest extends VRPTestCase{
	
	PickupAndDeliveryTourFactory tourBuilder;
	
	Tour tour;
	
	Vehicle vehicle;
	
	Shipment shipment;
	
	TourStatusProcessor tourActivityStatusUpdater;
	
	TourStatusProcessor tdTourStatusProcessor;
	
	TDCosts tdCosts;
	
	@Override
	public void setUp(){
		initJobsInPlainCoordinateSystem();
		tourActivityStatusUpdater = new TourCostProcessor(costs);
		
		Shipment s1 = VrpUtils.createShipment("1", makeId(0,0), makeId(0,10), 1, VrpUtils.createTimeWindow(0, Double.MAX_VALUE), 
				VrpUtils.createTimeWindow(8, 10));
		Shipment s2 = VrpUtils.createShipment("2", makeId(0,0), makeId(10,0), 1, VrpUtils.createTimeWindow(0, Double.MAX_VALUE), 
				VrpUtils.createTimeWindow(30,30));
		
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		tour = tourBuilder.build();
		
		vehicle = VrpUtils.createVehicle("1", makeId(0,0), 3);
		
		shipment = VrpUtils.createShipment("3", makeId(5,0), makeId(0,5), 1, VrpUtils.createTimeWindow(0, Double.MAX_VALUE), 
				VrpUtils.createTimeWindow(0,Double.MAX_VALUE));
		
		MyLocations loc = new MyLocations();
		loc.addLocation(makeId(0,10),new Coordinate(0,10));
		loc.addLocation(makeId(10,0),new Coordinate(10,0));
		loc.addLocation(makeId(0,0),new Coordinate(0,0));
		loc.addLocation(makeId(5,0), new Coordinate(5,0));
		loc.addLocation(makeId(0,5), new Coordinate(0,5));
		loc.addLocation(makeId(10,10), new Coordinate(10,10));
		
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
		
		tdCosts = new TDCosts(loc, timeBins, speedValues);
		tdTourStatusProcessor = new TourCostAndTWProcessor(tdCosts);
	}
	
	public void testAddShipment(){
		Tour newTour = new PickupAndDeliveryTourFactory(costs, constraints, tourActivityStatusUpdater).createTour(vehicle, tour, shipment, Double.MAX_VALUE);
		assertEquals(8,newTour.getActivities().size());
	}
	
	public void testAddShipmentAndResultingCosts(){
		Tour newTour = new PickupAndDeliveryTourFactory(costs, constraints, tourActivityStatusUpdater).createTour(vehicle, tour, shipment, Double.MAX_VALUE);
		assertEquals(50.0,newTour.costs.generalizedCosts);
		assertEquals(50.0,newTour.costs.distance);
		assertEquals(50.0,newTour.costs.time);
	}
	
	public void testAddShipmentAndResultingCostsWithTDCosts(){
		Tour newTour = new PickupAndDeliveryTourFactory(tdCosts, constraints, tdTourStatusProcessor).createTour(vehicle, tour, shipment, Double.MAX_VALUE);
		assertEquals(85.0,newTour.costs.generalizedCosts);
		assertEquals(50.0,newTour.costs.distance);
		assertEquals(35.0,newTour.costs.time);
	}
	
	public void testAddShipmentInDesiredOrder(){
		Shipment s1 = createShipment("1", makeId(0,0), makeId(10,0));
		Shipment s2 = createShipment("2", makeId(0,0), makeId(10,0));
		Shipment s3 = createShipment("3", makeId(0,0), makeId(10,0));
		Shipment s4 = createShipment("10", makeId(10,10), makeId(0,0));
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		Tour tour = tourBuilder.build();
		Tour newTour =  new PickupAndDeliveryTourFactory(costs, constraints, tourActivityStatusUpdater).createTour(vehicle, tour, s4, Double.MAX_VALUE);
		assertEquals(40.0,newTour.costs.generalizedCosts);
		assertEquals( makeId(10,10),newTour.getActivities().get(3).getLocationId());
		assertEquals(makeId(0,0), newTour.getActivities().get(6).getLocationId());
	}
	
	public void testAddShipmentInDesiredOrderWithTDCosts(){
		Shipment s1 = createShipment("1", makeId(0,0), makeId(10,0));
		Shipment s2 = createShipment("2", makeId(0,0), makeId(10,0));
		Shipment s3 = createShipment("3", makeId(0,0), makeId(10,0));
		Shipment s4 = createShipment("10", makeId(10,10), makeId(0,0));
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		Tour tour = tourBuilder.build();
		
		Tour newTour =  new PickupAndDeliveryTourFactory(tdCosts, constraints, tdTourStatusProcessor).createTour(vehicle, tour, s4, Double.MAX_VALUE);
		assertEquals(40.0,newTour.costs.distance);
		assertEquals(30.0,newTour.costs.time);
		assertEquals( makeId(10,10),newTour.getActivities().get(3).getLocationId());
		assertEquals(makeId(0,0), newTour.getActivities().get(6).getLocationId());
	}
	
	public void testAddShipmentInDesiredOrderV2(){
		Shipment s1 = createShipment("1", makeId(0,0), makeId(10,0));
		Shipment s2 = createShipment("2", makeId(0,0), makeId(10,0));
		Shipment s3 = createShipment("3", makeId(0,0), makeId(10,0));
		Shipment s4 = createShipment("10", makeId(10,10), makeId(0,0));
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.schedulePickup(s3);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleDelivery(s3);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		Tour tour = tourBuilder.build();
		Tour newTour =  new PickupAndDeliveryTourFactory(costs, constraints, tourActivityStatusUpdater).createTour(vehicle, tour, s4, Double.MAX_VALUE);
		assertEquals(40.0,newTour.costs.generalizedCosts);
		assertEquals(makeId(10,10),newTour.getActivities().get(5).getLocationId());
		assertEquals(makeId(0,0), newTour.getActivities().get(8).getLocationId());
	}
	
	public void testAddShipmentInDesiredOrderV2WithTDCosts(){
		Shipment s1 = createShipment("1", makeId(10,0), makeId(0,0));
		Shipment s2 = createShipment("2", makeId(10,0), makeId(0,0));
		Shipment s3 = createShipment("3", makeId(10,0), makeId(0,0));
		Shipment s4 = createShipment("10", makeId(10,10), makeId(0,0));
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart(makeId(0,0), 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(s1);
		tourBuilder.schedulePickup(s2);
		tourBuilder.schedulePickup(s3);
		tourBuilder.scheduleDelivery(s1);
		tourBuilder.scheduleDelivery(s2);
		tourBuilder.scheduleDelivery(s3);
		tourBuilder.scheduleEnd(makeId(0,0), 0.0, Double.MAX_VALUE);
		Tour tour = tourBuilder.build();
		Tour newTour =  new PickupAndDeliveryTourFactory(tdCosts, constraints, tdTourStatusProcessor).createTour(vehicle, tour, s4, Double.MAX_VALUE);
		assertEquals(40.0,newTour.costs.time);
		assertEquals(60.0,newTour.costs.distance);
		assertEquals(makeId(10,10),newTour.getActivities().get(1).getLocationId());
		assertEquals(makeId(0,0), newTour.getActivities().get(8).getLocationId());
	}
}
