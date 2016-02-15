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
package playground.vsp.analysis.modules.bvgAna.anaLevel1.agentId2PersonEnterLeaveVehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * @author ikaddoura
 *
 */
public class AgentId2PersonEnterLeaveVehicleAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(AgentId2PersonEnterLeaveVehicleAnalyzer.class);
	private MutableScenario scenario;
	
	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	
	private AgentId2PersonEnterLeaveVehicleHandler handler;
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> agentId2enterEvents;
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> agentId2leaveEvents;
	
	public AgentId2PersonEnterLeaveVehicleAnalyzer() {
		super(AgentId2PersonEnterLeaveVehicleAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverIdAnalyzer = new PtDriverIdAnalyzer();
		this.ptDriverIdAnalyzer.init(scenario);
		this.anaModules.add(ptDriverIdAnalyzer);
		
		this.handler = new AgentId2PersonEnterLeaveVehicleHandler(this.ptDriverIdAnalyzer);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();

		// from (sub-)modules
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
		
		// own handler
		allEventHandler.add(this.handler);
		
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		log.info("Preprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
	}

	@Override
	public void postProcessData() {
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalysisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		this.agentId2enterEvents = this.handler.getAgentId2EnterEventMap();
		this.agentId2leaveEvents = this.handler.getAgentId2LeaveEventMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getAgentId2enterEvents() {
		return agentId2enterEvents;
	}

	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getAgentId2leaveEvents() {
		return agentId2leaveEvents;
	}

}
