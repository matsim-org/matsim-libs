/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.SignalGroupStateChangedEventImpl;


/**
 * @author dgrether
 *
 */
public class SignalSystemImpl implements SignalSystem {

	private SignalController controller;
	private StateLogic stateLogic;
	private Map<Id, SignalGroup> signalGroups = new HashMap<Id, SignalGroup>();
	private SignalSystemsManager manager;
	
	private Set<SignalGroupStateChangeRequest> requests = new HashSet<SignalGroupStateChangeRequest>();
	
	private PriorityQueue<SignalGroupStateChangeRequest> sortedRequests = new PriorityQueue<SignalGroupStateChangeRequest>();
	private Id id;
	private Map<Id, Signal> signals = new HashMap<Id, Signal>();
	
	public SignalSystemImpl(Id id) {
		this.id = id;
	}

	@Override
	public SignalSystemsManager getSignalSystemsManager() {
		return this.manager;
	}

	@Override
	public void setSignalSystemsManager(SignalSystemsManager signalManager) {
		this.manager = signalManager;
	}

	@Override
	public void setSignalSystemController(SignalController controller) {
		this.controller = controller;
	}
	
	public void scheduleDropping(double timeSeconds, Id signalGroup){
		Set<SignalGroupStateChangeRequest> rqs = this.manager.getAmberLogic().processDropping(timeSeconds, this.getId(), signalGroup);
		requests.addAll(rqs);
	}
	
	public void scheduleOnset(double timeSeconds, Id signalGroupId){
		if (this.stateLogic != null){
			this.stateLogic.checkOnset(timeSeconds, signalGroupId);
		}
		Set<SignalGroupStateChangeRequest> rqs = this.manager.getAmberLogic().processOnsets(timeSeconds, this.getId(), signalGroupId);
		requests.addAll(rqs);
	}
	
	@Override
	public void updateState(double timeSeconds) {
		this.controller.updateState(timeSeconds);
		SignalGroupStateChangedEvent stateEvent;
		this.sortedRequests.addAll(this.requests);
		SignalGroupStateChangeRequest request = this.sortedRequests.peek();
		while (request != null && request.getTimeOfDay() <= timeSeconds){
			this.signalGroups.get(request.getSignalGroupId()).setState(request.getRequestedState());
			
			stateEvent = new SignalGroupStateChangedEventImpl(timeSeconds, this.getId(), request.getSignalGroupId(), request.getRequestedState());
			this.getSignalSystemsManager().getEventsManager().processEvent(stateEvent);
			this.sortedRequests.poll();
			request = this.sortedRequests.peek();
		}
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public void addSignal(Signal signal) {
		this.signals.put(signal.getId(), signal);
	}

	@Override
	public Map<Id, Signal> getSignals() {
		return this.signals;
	}

	@Override
	public void addSignalGroup(SignalGroup group) {
		this.signalGroups.put(group.getId(), group);
	}


}
