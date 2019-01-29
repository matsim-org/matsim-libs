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
package org.matsim.contrib.signals.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;


/**
 * @author dgrether
 *
 */
public class SignalSystemImpl implements SignalSystem {
	
	private static final Logger log = Logger.getLogger(SignalSystemImpl.class);

	public static final int SWITCH_OFF_SEQUENCE_LENGTH = 5;
	
	private SignalController signalController;
	private Map<Id<SignalGroup>, SignalGroup> signalGroups = new HashMap<>();
	private SignalSystemsManager signalManager;
	
	private Set<SignalGroupStateChangeRequest> requests = new HashSet<SignalGroupStateChangeRequest>();
	
	private PriorityQueue<SignalGroupStateChangeRequest> sortedRequests = new PriorityQueue<SignalGroupStateChangeRequest>();
	private Id<SignalSystem> id;
	private Map<Id<Signal>, Signal> signals = new HashMap<>();
	
	
	public SignalSystemImpl(Id<SignalSystem> id) {
		this.id = id;
	}

	@Override
	public void setSignalSystemsManager(SignalSystemsManager signalManager) {
		this.signalManager = signalManager;
	}

	@Override
	public void setSignalSystemController(SignalController controller) {
		this.signalController = controller;
	}
	
	@Override
	public void scheduleDropping(double timeSeconds, Id<SignalGroup> signalGroupId) {
//		log.debug("dropping  at time " + timeSeconds + " of  group " + signalGroupId);
		Set<SignalGroupStateChangeRequest> rqs = this.signalManager.getAmberLogic().processDropping(timeSeconds, this.getId(), signalGroupId);
		requests.addAll(rqs);
	}
	
	@Override
	public void scheduleOnset(double timeSeconds, Id<SignalGroup> signalGroupId) {
//		log.debug("onset at time " + timeSeconds + " of  group " + signalGroupId);
		Set<SignalGroupStateChangeRequest> rqs = this.signalManager.getAmberLogic().processOnsets(timeSeconds, this.getId(), signalGroupId);
		requests.addAll(rqs);
	}
	
	@Override
	public void updateState(double timeSeconds) {
		this.signalController.updateState(timeSeconds);
		SignalGroupStateChangedEvent stateEvent;
		this.sortedRequests.addAll(this.requests);
		this.requests.clear();
		SignalGroupStateChangeRequest request = this.sortedRequests.peek();
		while (request != null && request.getTimeOfDay() <= timeSeconds){
//			log.debug("system id " + this.id + " group " + request.getSignalGroupId() + " state " + request.getRequestedState() + " at time " + timeSeconds);
			this.signalGroups.get(request.getSignalGroupId()).setState(request.getRequestedState());
			stateEvent = new SignalGroupStateChangedEvent(timeSeconds, this.getId(), request.getSignalGroupId(), request.getRequestedState());
			this.signalManager.getEventsManager().processEvent(stateEvent);
			this.sortedRequests.poll();
			request = this.sortedRequests.peek();
		}
	}
	
	@Override
	public void switchOff(double timeSeconds) {
		Set<SignalGroupStateChangeRequest> req = new HashSet<SignalGroupStateChangeRequest>();
		for (Id<SignalGroup> sgId : this.signalGroups.keySet()){
			req.add(new SignalGroupStateChangeRequestImpl(sgId, SignalGroupState.YELLOW, timeSeconds));
			req.add(new SignalGroupStateChangeRequestImpl(sgId, SignalGroupState.OFF, timeSeconds + SWITCH_OFF_SEQUENCE_LENGTH));
		}
		this.sortedRequests.addAll(req);
	}


	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		this.signalController.simulationInitialized(simStartTimeSeconds);
	}

	
	@Override
	public Id<SignalSystem> getId() {
		return this.id;
	}

	@Override
	public void addSignal(Signal signal) {
		this.signals.put(signal.getId(), signal);
	}

	@Override
	public Map<Id<Signal>, Signal> getSignals() {
		return this.signals;
	}

	@Override
	public void addSignalGroup(SignalGroup group) {
		this.signalGroups.put(group.getId(), group);
	}
	
	@Override
	public Map<Id<SignalGroup>, SignalGroup> getSignalGroups(){
		return this.signalGroups;
	}

	@Override
	public SignalController getSignalController() {
		return this.signalController;
	}

	@Override
	public void startPlan(double now) {
		Set<SignalGroupStateChangeRequest> req = new HashSet<SignalGroupStateChangeRequest>();
		for (Id<SignalGroup> sgId : this.signalGroups.keySet()){
			req.add(new SignalGroupStateChangeRequestImpl(sgId, SignalGroupState.START_PLAN, now));
		}
		this.sortedRequests.addAll(req);
	}

}
