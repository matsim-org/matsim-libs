/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
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

package playground.marcel.pt.integration;

import org.matsim.events.Events;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;

import playground.marcel.pt.implementations.VehicleImpl;
import playground.marcel.pt.interfaces.Vehicle;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.tryout.BusDriver;

public class TransitQueueSimulation extends QueueSimulation {
	
	private TransitSchedule schedule = null;
	
	public TransitQueueSimulation(final NetworkLayer network, final Population population, final Events events) {
		super(network, population, events);
	}
	
	public void setTransitSchedule(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	@Override
	protected void createAgents() {
		super.createAgents();
		
		if (this.schedule != null) {
			TransitLine line1 = this.schedule.getTransitLines().values().iterator().next();
			TransitRoute route = line1.getRoutes().values().iterator().next();
			Departure departure = route.getDepartures().values().iterator().next();
			BusDriver driver = new BusDriver(route, departure);
			Vehicle bus = new VehicleImpl(20, events);
			driver.setVehicle(bus);
			bus.setDriver(driver);
			addVehicleToLink(new TransitQueueVehicle(bus));
			
			
//			for (TransitLine line : this.schedule.getTransitLines().values()) {
//				for (TransitRoute route : line.getRoutes().values()) {
//					for (Departure departure : route.getDepartures().values()) {
//						BusDriver driver = new BusDriver(route, departure);
//						Vehicle bus = new VehicleImpl(20, events);
//						driver.setVehicle(bus);
//						
//						
//						
//					}
//				}
//			}
		}

		
//		
//		if (this.plans == null) {
//			throw new RuntimeException("No valid Population found (plans == null)");
//		}
//		for (Person p : this.plans.getPersons().values()) {
//			PersonAgent agent = this.agentFactory.createPersonAgent(p);
//
//			QueueVehicle veh;
//			try {
//				veh = this.vehiclePrototype.newInstance();
//				//not needed in new agent class
//				veh.setDriver(agent);
//				agent.setVehicle(veh);
//
//				if (agent.initialize()) {
//					addVehicleToLink(veh);
//				}
//			} catch (InstantiationException e) {
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			}
//		}
		
	}
	
}
