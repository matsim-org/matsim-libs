/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.distance;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

/**
 * @author droeder
 *
 */
public class DistAnalysisHandler implements 
									TransitDriverStartsEventHandler, 
									LinkEnterEventHandler, LinkLeaveEventHandler, 
									AgentDepartureEventHandler, AgentArrivalEventHandler{
	
	private Map<Id, DistAnalysisPtDriver> drivers;
	private Map<Id, DistAnalysisAgent> agents;
	private Map<Id, DistAnalysisTransitRoute> routes;
	private Map<Id, DistAnalysisVehicle> vehicles;
	
	private Map<Id, Link> links;
	
	
	public DistAnalysisHandler(Map<Id, Link> links, Map<Id, DistAnalysisAgent> agents){
		this.links = links;
		this.agents = agents;
		
		this.drivers = new HashMap<Id, DistAnalysisPtDriver>();
		this.routes = new HashMap<Id, DistAnalysisTransitRoute>();
		this.vehicles = new HashMap<Id, DistAnalysisVehicle>();
	}
	

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(AgentDepartureEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent e) {
		if(!this.drivers.containsKey(e.getDriverId())){
			this.drivers.put(e.getDriverId(), new DistAnalysisPtDriver(e.getDriverId(), e.getVehicleId()));
		}
		
		if(!this.vehicles.containsKey(e.getTransitRouteId())){
			this.routes.put(e.getTransitRouteId(), new DistAnalysisTransitRoute());
		}
	}

}
