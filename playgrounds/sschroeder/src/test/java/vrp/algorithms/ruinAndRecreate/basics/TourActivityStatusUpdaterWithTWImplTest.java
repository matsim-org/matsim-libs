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
import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.api.Customer;
import vrp.basics.Tour;

public class TourActivityStatusUpdaterWithTWImplTest extends VRPTestCase{
	
	Tour tour;
	
	Tour anotherTour;
	
	TourActivityStatusUpdater statusUpdater;
	
	@Override
	public void setUp(){
		initCustomersInPlainCoordinateSystem();
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
		
		Customer cust21 = customerMap.get(makeId(0,9));
		Customer cust22 = customerMap.get(makeId(10,0));
		Collection<Customer> anotherTourSequence = new ArrayList<Customer>();
		anotherTourSequence.add(depot);
		anotherTourSequence.add(cust21);
		anotherTourSequence.add(cust22);
		anotherTourSequence.add(depot);
		anotherTour = makeTour(anotherTourSequence);
		
		statusUpdater = new TourActivityStatusUpdaterWithTWImpl(costs);
	}
	
	@Override
	public void tearDown(){
		
	}
	
	public void testCalculatedDistance(){
		statusUpdater.update(tour);
		assertEquals(40.0, tour.costs.distance);
	}
	
	public void testCalculatedCosts(){
		statusUpdater.update(tour);
		assertEquals(40.0, tour.costs.generalizedCosts);
	}
	
	public void testCalculatedTime(){
		statusUpdater.update(tour);
		assertEquals(40.0, tour.costs.time);
	}
	
	public void testCurrentLoadsForTwoPickups(){
		statusUpdater.update(tour);
		assertEquals(0, tour.getActivities().get(0).getCurrentLoad());
		assertEquals(1, tour.getActivities().get(1).getCurrentLoad());
		assertEquals(2, tour.getActivities().get(2).getCurrentLoad());
		assertEquals(2, tour.getActivities().get(3).getCurrentLoad());
	}
	
	public void testCurrentLoadsForPickupAndDelivery(){
		statusUpdater.update(anotherTour);
		assertEquals(1, anotherTour.getActivities().get(0).getCurrentLoad());
		assertEquals(0, anotherTour.getActivities().get(1).getCurrentLoad());
		assertEquals(1, anotherTour.getActivities().get(2).getCurrentLoad());
		assertEquals(1, anotherTour.getActivities().get(3).getCurrentLoad());
	}
	
	public void testCalculatedDistanceForAnotherTour(){
		statusUpdater.update(anotherTour);
		assertEquals(38.0, anotherTour.costs.distance);
	}
	
	public void testCalculatedCostsForAnotherTour(){
		statusUpdater.update(anotherTour);
		assertEquals(38.0, anotherTour.costs.generalizedCosts);
	}
	
	public void testCalculatedTimeForAnotherTour(){
		statusUpdater.update(anotherTour);
		assertEquals(38.0, anotherTour.costs.time);
	}

	public void testPracticalTimeWindowsOfDepotStart(){
		statusUpdater.update(tour);
		assertEquals(0.0,tour.getActivities().get(0).getEarliestArrTime());
		assertEquals(0.0,tour.getActivities().get(0).getLatestArrTime());
	}
	
	public void testPracticalTimeWindowsOfFirstCustomer(){
		statusUpdater.update(tour);
		assertEquals(10.0,tour.getActivities().get(1).getEarliestArrTime());
		assertEquals(10.0,tour.getActivities().get(1).getLatestArrTime());
	}
	
	public void testPracticalTimeWindowsOfSecondCustomer(){
		statusUpdater.update(tour);
		assertEquals(30.0,tour.getActivities().get(2).getEarliestArrTime());
		assertEquals(30.0,tour.getActivities().get(2).getLatestArrTime());
	}
	
	public void testPracticalTimeWindowsOfDepotEnd(){
		statusUpdater.update(tour);
		assertEquals(40.0,tour.getActivities().get(3).getEarliestArrTime());
		assertEquals(Double.MAX_VALUE,tour.getActivities().get(3).getLatestArrTime());
	}
	
}
