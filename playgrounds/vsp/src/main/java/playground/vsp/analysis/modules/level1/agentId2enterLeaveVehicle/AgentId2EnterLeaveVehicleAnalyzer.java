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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.level1.agentId2enterLeaveVehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * This module analyzes the entering and leaving event for each person.
 * Ignoring the pt driver and not differentiating between public and private vehicles.
 * 
 * @author ikaddoura
 *
 */
public class AgentId2EnterLeaveVehicleAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(AgentId2EnterLeaveVehicleAnalyzer.class);
	private ScenarioImpl scenario;
	private AgentId2EnterLeaveVehicleEventHandler enterLeaveHandler;
	
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> agentId2EnterEvent;
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> agentId2LeaveEvent;
			
	public AgentId2EnterLeaveVehicleAnalyzer(String ptDriverPrefix) {
		super(AgentId2EnterLeaveVehicleAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.enterLeaveHandler = new AgentId2EnterLeaveVehicleEventHandler(this.ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.enterLeaveHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		
		this.agentId2EnterEvent = this.enterLeaveHandler.getAgentId2EnterEventMap();
		this.agentId2LeaveEvent = this.enterLeaveHandler.getAgentId2LeaveEventMap();
		
	}

	@Override
	public void writeResults(String outputFolder) {
		// nothing to write so far
	}
	
	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getAgentId2EnterEvent() {
		return agentId2EnterEvent;
	}

	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getAgentId2LeaveEvent() {
		return agentId2LeaveEvent;
	}

}
