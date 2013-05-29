/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightModalSplitEventHandler
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
package air.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.utils.collections.Tuple;


/**
 * Collects the modal split by o-d pair
 * @author dgrether
 *
 */
public class DgFlightModalSplitEventHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	private Map<Id, AgentDepartureEvent> id2depEventMap;
	private Map<Tuple<Id, Id>, ModalSplitData> resultsByOdPair;
	private Set<Id> processedPersonIds;
	private int numberOfPtTrips;
	private int numberOfTrainTrips;
	private int numberOfStuckTrips;
	
	public static final class ModalSplitData {
		int train = 0;
		int pt = 0;
	}
	
	public DgFlightModalSplitEventHandler(){ 
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.id2depEventMap = new HashMap<Id, AgentDepartureEvent>();
		this.resultsByOdPair = new HashMap<Tuple<Id, Id>, ModalSplitData>();
		this.processedPersonIds = new HashSet<Id>();
		this.numberOfPtTrips = 0;
		this.numberOfTrainTrips = 0;
		this.numberOfStuckTrips = 0;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (this.checkLegMode(event.getLegMode())) {
			this.id2depEventMap.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (this.checkLegMode(event.getLegMode())) {
			AgentDepartureEvent departureEvent = this.id2depEventMap.get(event.getPersonId());
			ModalSplitData result = this.getOrCreateResultEntry(departureEvent.getLinkId(), event.getLinkId());
			if (event.getLegMode().equals("pt")) {
				result.pt++;
				if (! this.processedPersonIds.contains(event.getPersonId())) {
					this.numberOfPtTrips++;
					this.processedPersonIds.add(event.getPersonId());
				}
			}
			else if (event.getLegMode().equals("train")) {
				result.train++;
				if (! this.processedPersonIds.contains(event.getPersonId())) {
					this.numberOfTrainTrips++;
					this.processedPersonIds.add(event.getPersonId());
				}
			}
			else {
				throw new IllegalStateException("train or pt expected");
			}
		}
	}

	public void handleEvent(AgentStuckEvent event) {
		if (this.checkLegMode(event.getLegMode())) {
			this.numberOfStuckTrips++;
			if (this.processedPersonIds.contains(event.getPersonId())) {
				this.numberOfPtTrips--;
			}
		}
	}

	private ModalSplitData getOrCreateResultEntry(Id from, Id to) {
		Tuple<Id, Id> tuple = new Tuple<Id, Id>(from, to);
		if (! this.resultsByOdPair.containsKey(tuple)){
			this.resultsByOdPair.put(tuple, new ModalSplitData());
		}
		return this.resultsByOdPair.get(tuple);
	}
	
	private boolean checkLegMode(String mode) {
		return mode.equals("pt") || mode.equals("train");
	}

	
	public Map<Tuple<Id, Id>, ModalSplitData> getResultsByOdPair() {
		return resultsByOdPair;
	}

	
	public int getNumberOfPtTrips() {
		return numberOfPtTrips;
	}

	
	public int getNumberOfTrainTrips() {
		return numberOfTrainTrips;
	}

	
	public int getNumberOfStuckTrips() {
		return numberOfStuckTrips;
	}

}
