/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.StageContainerHandler;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

/**
 * Collects {@link StageContainer} and creates {@link PersonMoneyEvent}.
 * 
 * @author aneumann
 *
 */
public final class StageContainer2AgentMoneyEvent implements StageContainerHandler, AfterMobsimListener{

	private final EventsManager eventsManager;
	private final double mobsimShutdownTime;
	private HashMap<Id<Person>, List<StageContainer>> agentId2stageContainerListMap = new HashMap<>();
	private final TicketMachineI ticketMachine;

	public StageContainer2AgentMoneyEvent(MatsimServices controler, TicketMachineI ticketMachine) {
		controler.addControlerListener(this);
		this.eventsManager = controler.getEvents();
		this.mobsimShutdownTime = controler.getConfig().qsim().getEndTime();
		this.ticketMachine = ticketMachine;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO[AN] This can be used to hook in a Farebox
		for (Entry<Id<Person>, List<StageContainer>> agentId2stageContainersEntry : this.agentId2stageContainerListMap.entrySet()) {
			double totalFareOfAgent = 0.0;
			for (StageContainer stageContainer : agentId2stageContainersEntry.getValue()) {
				totalFareOfAgent += this.ticketMachine.getFare(stageContainer);
			}
			this.eventsManager.processEvent(new PersonMoneyEvent(this.mobsimShutdownTime, agentId2stageContainersEntry.getKey(), -totalFareOfAgent));
		}
	}

	@Override
	public void handleFareContainer(StageContainer stageContainer) {
		if (this.agentId2stageContainerListMap.get(stageContainer.getAgentId()) == null) {
			this.agentId2stageContainerListMap.put(stageContainer.getAgentId(), new LinkedList<StageContainer>());
		}
		
		this.agentId2stageContainerListMap.get(stageContainer.getAgentId()).add(stageContainer);
	}

	@Override
	public void reset() {
		this.agentId2stageContainerListMap = new HashMap<>();
	}
}
