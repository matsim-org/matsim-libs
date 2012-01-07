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
package org.matsim.contrib.freight.vrp.basics;


import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgentFactory;


/**
 * 
 * @author stefan schroeder
 *
 */

public class VrpUtils {

	public static Vehicle createVehicle(String id, String locationId, int vehicleCapacity) {
		return new Vehicle(id, locationId, vehicleCapacity);
	}
	
	public static RRSolution copySolution(RRSolution solution, VehicleRoutingProblem vrp, TourAgentFactory tourAgentFactory){
		List<TourAgent> agents = new ArrayList<TourAgent>();
		List<Vehicle> vehicles = new ArrayList<Vehicle>(vrp.getVehicles());
		List<TourAgent> tourAgentsInSolution = new ArrayList<TourAgent>(solution.getTourAgents());
		for(int i=0;i<tourAgentsInSolution.size();i++){
			VrpTourBuilder tourBuilder = new VrpTourBuilder();
			Vehicle vehicle = vehicles.get(i);
			TourAgent agent = tourAgentsInSolution.get(i);
			for(TourActivity tourAct : agent.getTour().getActivities()){
				tourBuilder.scheduleActivity(tourAct);
			}
			Tour tour = tourBuilder.build();
			TourAgent newTourAgent = tourAgentFactory.createTourAgent(tour, vehicle);
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
