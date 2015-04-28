/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.LinkedList;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

public class ParkingPSim implements Mobsim {

	private Scenario sc;
	private EventsManager eventsManager;
	private LinkedList<AgentWithParking> agentsMessage;

	public ParkingPSim(Scenario sc, EventsManager eventsManager, LinkedList<AgentWithParking> agentsMessage){
		this.sc = sc;
		this.eventsManager = eventsManager;
		this.agentsMessage = agentsMessage;
	}
	
	@Override
	public void run() {
		Message.messageQueue=new MessageQueue();
		Message.eventsManager=eventsManager;
		
		for (AgentWithParking awp:agentsMessage){
			awp.scheduleMessage();
		}
		
		Message.messageQueue.startSimulation();
	}

}

