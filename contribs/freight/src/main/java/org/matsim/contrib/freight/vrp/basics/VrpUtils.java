/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;


import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRDriverAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;


/**
 * 
 * @author stefan schroeder
 *
 */

public class VrpUtils {

	public static Vehicle createVehicle(String id, String locationId, int vehicleCapacity) {
		return new Vehicle(id, locationId, vehicleCapacity);
	}
	
	public static RRSolution copySolution(RRSolution solution, VehicleRoutingProblem vrp, RRTourAgentFactory tourAgentFactory){
		List<RRDriverAgent> agents = new ArrayList<RRDriverAgent>();
		for(RRDriverAgent agent : solution.getTourAgents()){
			VrpTourBuilder tourBuilder = new VrpTourBuilder();
			Vehicle vehicle = agent.getVehicle();
			for(TourActivity tourAct : agent.getTour().getActivities()){
				tourBuilder.scheduleActivity(tourAct);
			}
			Tour tour = tourBuilder.build();
			RRDriverAgent newTourAgent = tourAgentFactory.createTourAgent(tour, vehicle);
			agents.add(newTourAgent);
		}
		return new RRSolution(agents);  
	}
	
	public static TimeWindow createTimeWindow(double start, double end){
		return new TimeWindow(start, end);
	}
	
	public static String createId(String id){
		return id;
	}

	public static Coordinate createCoord(int x, int y) {
		return new Coordinate(x,y);
	}

	public static Shipment createShipment(String id, String fromId, String toId, int size, TimeWindow pickupTW,TimeWindow deliverTW) {
		Shipment s = createShipment(id, fromId, toId, size);
		s.setPickupTW(pickupTW);
		s.setDeliveryTW(deliverTW);
		return s;
	}
	
	public static Shipment createShipment(String id, String fromId, String toId, int size){
		return new Shipment(id, fromId, toId, size);
	}

	
	
}
