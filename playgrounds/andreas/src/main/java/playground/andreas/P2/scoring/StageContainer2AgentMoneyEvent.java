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

package playground.andreas.P2.scoring;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.andreas.P2.scoring.fare.StageContainer;
import playground.andreas.P2.scoring.fare.StageContainerHandler;
import playground.andreas.P2.scoring.fare.TicketMachine;

/**
 * Collects {@link StageContainer} and creates {@link PersonMoneyEvent}.
 * 
 * @author aneumann
 *
 */
public class StageContainer2AgentMoneyEvent implements StageContainerHandler, AfterMobsimListener{

	private EventsManager eventsManager;
	private double mobsimShutdownTime;
	private HashMap<Id, List<StageContainer>> agentId2stageContainerListMap = new HashMap<Id, List<StageContainer>>();
	private final TicketMachine ticketMachine;

	public StageContainer2AgentMoneyEvent(Controler controler, TicketMachine ticketMachine) {
		controler.addControlerListener(this);
		this.eventsManager = controler.getEvents();
		this.mobsimShutdownTime = controler.getConfig().getQSimConfigGroup().getEndTime();
		this.ticketMachine = ticketMachine;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO[AN] This can be used to hook in a Farebox
		for (Entry<Id, List<StageContainer>> agentId2stageContainersEntry : this.agentId2stageContainerListMap.entrySet()) {
			double totalFareOfAgent = 0.0;
			for (StageContainer stageContainer : agentId2stageContainersEntry.getValue()) {
				totalFareOfAgent += this.ticketMachine.getFare(stageContainer);
			}
			this.eventsManager.processEvent(new PersonMoneyEvent(this.mobsimShutdownTime, agentId2stageContainersEntry.getKey(), totalFareOfAgent));
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
	public void reset(int iteration) {
		this.agentId2stageContainerListMap = new HashMap<Id, List<StageContainer>>();
	}
}
