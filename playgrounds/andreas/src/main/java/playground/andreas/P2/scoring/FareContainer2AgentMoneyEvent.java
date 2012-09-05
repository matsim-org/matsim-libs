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
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;

import playground.andreas.P2.scoring.fare.FareContainer;
import playground.andreas.P2.scoring.fare.FareContainerHandler;

/**
 * Collects {@link FareContainer} and creates {@link AgentMoneyEvent}.
 * 
 * @author aneumann
 *
 */
public class FareContainer2AgentMoneyEvent implements FareContainerHandler, AfterMobsimListener{

	private EventsManager eventsManager;
	private double mobsimShutdownTime;
	private HashMap<Id, List<FareContainer>> agentId2fareContainersMap = new HashMap<Id, List<FareContainer>>();

	public FareContainer2AgentMoneyEvent(Controler controler) {
		controler.addControlerListener(this);
		this.eventsManager = controler.getEvents();
		this.mobsimShutdownTime = controler.getConfig().getQSimConfigGroup().getEndTime();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO[AN] This can be used to hook in a Farebox
		for (Entry<Id, List<FareContainer>> agentId2fareContainersEntry : this.agentId2fareContainersMap.entrySet()) {
			double totalFareOfAgent = 0.0;
			for (FareContainer fareContainer : agentId2fareContainersEntry.getValue()) {
				totalFareOfAgent += fareContainer.getFare();
			}
			this.eventsManager.processEvent(new AgentMoneyEventImpl(this.mobsimShutdownTime, agentId2fareContainersEntry.getKey(), totalFareOfAgent));
		}
	}

	@Override
	public void handleFareContainer(FareContainer fareContainer) {
		if (this.agentId2fareContainersMap.get(fareContainer.getAgentId()) == null) {
			this.agentId2fareContainersMap.put(fareContainer.getAgentId(), new LinkedList<FareContainer>());
		}
		
		this.agentId2fareContainersMap.get(fareContainer.getAgentId()).add(fareContainer);
	}

	@Override
	public void reset(int iteration) {
		this.agentId2fareContainersMap = new HashMap<Id, List<FareContainer>>();
	}
}
