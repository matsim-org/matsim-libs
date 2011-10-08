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
package vrp.algorithms.ruinAndRecreate.basics;

import java.util.ArrayList;
import java.util.Collection;

import vrp.VRPTestCase;
import vrp.api.Customer;
import vrp.basics.Tour;

public class BestTourBuilderTest extends VRPTestCase{
	
	BestTourBuilder tourBuilder;
	
	Tour tour;
	
	Shipment shipment;
	
	@Override
	public void setUp(){
		initCustomersInPlainCoordinateSystem();
		tourBuilder = new BestTourBuilder();
		tourBuilder.setCosts(costs);
		tourBuilder.setTourActivityStatusUpdater(new TourActivityStatusUpdaterImpl(costs));
		
		Customer depot = getDepot();
		Customer cust1 = customerMap.get(makeId(0,10));
		Customer cust2 = customerMap.get(makeId(10,0));
		cust1.setTheoreticalTimeWindow(8, 12);
		cust2.setTheoreticalTimeWindow(30, 30);
		Collection<Customer> tourSequence = new ArrayList<Customer>();
		tourSequence.add(depot);
		tourSequence.add(cust1);
		tourSequence.add(cust2);
		tourSequence.add(depot);
		tour = makeTour(tourSequence);
		
		Customer cust31 = customerMap.get(makeId(5,5));
		Customer cust32 = customerMap.get(makeId(9,10));
		shipment = new Shipment(cust31, cust32);
	}
	
	public void testAddShipment(){
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, Double.MAX_VALUE);
		assertEquals(6,newTour.getActivities().size());
	}
	
	public void testAddShipmentAndResultingCosts(){
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, Double.MAX_VALUE);
		assertEquals(50.0,newTour.costs.generalizedCosts);
		assertEquals(50.0,newTour.costs.distance);
		assertEquals(50.0,newTour.costs.time);
	}
	
	public void testAddShipmentInDesiredOrder(){
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, Double.MAX_VALUE);
		assertEquals(customerMap.get(makeId(5,5)),newTour.getActivities().get(1).getCustomer());
		assertEquals(customerMap.get(makeId(9,10)),newTour.getActivities().get(3).getCustomer());
	}
	
	public void testAddShipmentAtBeginning(){
		Customer cust31 = customerMap.get(makeId(0,1));
		Customer cust32 = customerMap.get(makeId(0,2));
		Shipment shipment = new Shipment(cust31, cust32);
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment, Double.MAX_VALUE);
		assertEquals(cust31,newTour.getActivities().get(1).getCustomer());
		assertEquals(cust32,newTour.getActivities().get(2).getCustomer());
	}
	
	public void testAddShipmentAtEnd(){
		Customer cust31 = customerMap.get(makeId(8,0));
		Customer cust32 = customerMap.get(makeId(7,0));
		Shipment shipment = new Shipment(cust31, cust32);
		Tour newTour = tourBuilder.addShipmentAndGetTour(tour, shipment,Double.MAX_VALUE);
		assertEquals(cust31,newTour.getActivities().get(2).getCustomer());
		assertEquals(cust32,newTour.getActivities().get(4).getCustomer());
		assertEquals(40.0,newTour.costs.generalizedCosts);
	}
	

}
