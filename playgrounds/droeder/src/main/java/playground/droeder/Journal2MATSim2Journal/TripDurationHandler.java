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
package playground.droeder.Journal2MATSim2Journal;

import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.opengis.metadata.citation.Telephone;

/**
 * @author droeder
 *
 */
public class TripDurationHandler implements ActivityEndEventHandler, ActivityStartEventHandler, 
											LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler,
											PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
											AgentDepartureEventHandler, AgentArrivalEventHandler{

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityStartEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent e) {
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
	public void handleEvent(PersonLeavesVehicleEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent e) {
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




}
