/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.LinkImpl;

import playground.fhuelsmann.emission.objects.HbefaColdEmissionTableCreator;

public class ColdEmissionHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentDepartureEventHandler{
	private static final Logger logger = Logger.getLogger(ColdEmissionHandler.class);

	private final Network network;
	private final HbefaColdEmissionTableCreator hbefaColdTable;
	private final ColdEmissionAnalysisModule coldEmissionAnalysisModule;
	private final EventsManager emissionEventsManager;

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> linkleave = new TreeMap<Id, Double>();
	private final Map<Id, Double> startEngine = new TreeMap<Id, Double>();
	private final Map<Id, Double> stopEngine = new TreeMap<Id, Double>();

	private final Map<Id, Double> accumulatedDistance = new TreeMap<Id, Double>();
	private final Map<Id, Double> parkingDuration = new TreeMap<Id, Double>();
	private final Map<Id, Id> personId2coldEmissionEventLinkId = new TreeMap<Id, Id>();

	public ColdEmissionHandler(
			final Network network,
			HbefaColdEmissionTableCreator hbefaTable,
			ColdEmissionAnalysisModule coldEmissionAnalysisModule,
			EventsManager emissionEventsManager ){
		this.network = network;
		this.hbefaColdTable = hbefaTable;
		this.coldEmissionAnalysisModule = coldEmissionAnalysisModule;
		this.emissionEventsManager = emissionEventsManager;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {	
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		Double time = event.getTime();
		this.linkleave.put(personId, time);

		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		double linkLength = link.getLength();

		if (this.accumulatedDistance.get(personId) != null){
			double distanceSoFar = this.accumulatedDistance.get(personId);
			this.accumulatedDistance.put(personId, distanceSoFar + linkLength);
		} else {
			this.accumulatedDistance.put(personId, linkLength);
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(event.getLegMode().equals("car")){
			Id personId= event.getPersonId();
			Double stopEngineTime = event.getTime();
			this.stopEngine.put(personId, stopEngineTime);
			
			double startEngineTime = this.startEngine.get(personId);
			double parkingDuration = this.parkingDuration.get(personId);
			Id coldEmissionEventLinkId = this.personId2coldEmissionEventLinkId.get(personId);
			
			Double accumulatedDistance;
			if(this.accumulatedDistance.containsKey(personId)){
				accumulatedDistance = this.accumulatedDistance.get(personId);
			} else {
				accumulatedDistance = 0.0;
				this.accumulatedDistance.put(personId, 0.0);
			}
			this.coldEmissionAnalysisModule.calculateColdEmissions(
					coldEmissionEventLinkId,
					personId,
					startEngineTime,
					parkingDuration,
					accumulatedDistance,
					this.hbefaColdTable,
					this.emissionEventsManager);
			this.accumulatedDistance.remove(personId);
		}
		else{
			// emissions to calculate...
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getLegMode().equals("car")){
			Id linkId = event.getLinkId();
			Id personId= event.getPersonId();
			Double startEngineTime = event.getTime();
			this.startEngine.put(personId, startEngineTime);
			this.personId2coldEmissionEventLinkId.put(personId, linkId);

			Double parkingDuration;
			if (this.stopEngine.containsKey(personId)){
				double stopEngineTime = this.stopEngine.get(personId);
				parkingDuration = startEngineTime - stopEngineTime;

			} else {
				parkingDuration = 43200.0; //parking duration is assumed to be at least 12 hours when parking overnight
			}
			this.parkingDuration.put(personId, parkingDuration);
			}
			else{
				// no engine to start...
			}
	}
}