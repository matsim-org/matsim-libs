/* *********************************************************************** *
 * project: org.matsim.*
 * IniSolution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.vrp.basics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.freight.vrp.algorithms.rr.api.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.RRTourAgentWithTimeWindowFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Shipment;
import org.matsim.contrib.freight.vrp.algorithms.rr.basics.Solution;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.BestInsertion;
import org.matsim.contrib.freight.vrp.api.Customer;
import org.matsim.contrib.freight.vrp.api.SingleDepotVRP;

public class IniSolution implements SingleDepotInitialSolutionFactory {
	
	private int nVehicles;
	
	public IniSolution(int nVehicles) {
		super();
		this.nVehicles = nVehicles;
	}

	@Override
	public Collection<Tour> createInitialSolution(SingleDepotVRP vrp) {
		Solution emptySolution = createEmptySolution(vrp, new RRTourAgentWithTimeWindowFactory(vrp));
		BestInsertion bestInsertion = new BestInsertion(vrp);
		bestInsertion.run(emptySolution, getShipmentsWithoutService(vrp));
		return getTours(emptySolution);
	}

	private Collection<Tour> getTours(Solution emptySolution) {
		Collection<Tour> tours = new ArrayList<Tour>();
		for(TourAgent tA : emptySolution.getTourAgents()){
			tours.add(tA.getTour());
		}
		return tours;
	}

	private List<Shipment> getShipmentsWithoutService(SingleDepotVRP vrp) {
		List<Shipment> shipments = new ArrayList<Shipment>();
		Set<Customer> plannedCustomer = new HashSet<Customer>();
		for(Customer c : vrp.getCustomers().values()){
			if(plannedCustomer.contains(c)){
				continue;
			}
			if(c.hasRelation()){
				if(c.getDemand()<0){
					Customer toCustomer = c;
					Customer fromCustomer = c.getRelation().getCustomer();
					Shipment s = new Shipment(fromCustomer, toCustomer);
					shipments.add(s);
				}
				else{
					Customer toCustomer = c.getRelation().getCustomer();
					Customer fromCustomer = c;
					Shipment s = new Shipment(fromCustomer,toCustomer);
					shipments.add(s);
				}
				plannedCustomer.add(c);
				plannedCustomer.add(c.getRelation().getCustomer());
			}
		}
		return shipments; 
	}

	private Solution createEmptySolution(SingleDepotVRP vrp, RRTourAgentWithTimeWindowFactory rrTourAgentWithTimeWindowFactory) {
		Collection<TourAgent> emptyTours = new ArrayList<TourAgent>();
		for (int i=0; i<nVehicles; i++) {
			TourAgent tourAgent = createTourAgent(vrp.getVehicleType(), vrp.getDepot(), rrTourAgentWithTimeWindowFactory);
			emptyTours.add(tourAgent);
		}
		return new Solution(emptyTours);
	}
	
	private TourAgent createTourAgent(VehicleType vehicleType, Customer depot, RRTourAgentWithTimeWindowFactory rrTourAgentWithTimeWindowFactory) {
		Vehicle vehicle = VrpUtils.createVehicle(vehicleType);
		vehicle.setLocationId(depot.getLocation().getId());
		Tour emptyTour = VrpUtils.createEmptyRoundTour(depot);		
		TourAgent agent = rrTourAgentWithTimeWindowFactory.createTourAgent(emptyTour, vehicle);
		return agent;
	}

}
