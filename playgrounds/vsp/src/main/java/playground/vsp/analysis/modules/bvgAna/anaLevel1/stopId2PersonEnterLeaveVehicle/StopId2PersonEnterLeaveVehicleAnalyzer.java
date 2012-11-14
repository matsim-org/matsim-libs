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
package playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2PersonEnterLeaveVehicle;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverPrefixAnalyzer;

/**
 * @author ikaddoura
 *
 */
public class StopId2PersonEnterLeaveVehicleAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(StopId2PersonEnterLeaveVehicleAnalyzer.class);
	private ScenarioImpl scenario;
	
	private List<AbstractAnalyisModule> anaModules = new LinkedList<AbstractAnalyisModule>();
	private PtDriverPrefixAnalyzer ptDriverPrefixAnalyzer;
	
	private StopId2PersonEnterLeaveVehicleHandler handler;
	private Map<Id, List<PersonEntersVehicleEvent>> stopId2PersonEnterEvent;
	private Map<Id, List<PersonLeavesVehicleEvent>> stopId2PersonLeaveEvent;
	
	public StopId2PersonEnterLeaveVehicleAnalyzer() {
		super(StopId2PersonEnterLeaveVehicleAnalyzer.class.getSimpleName());
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		
		// (sub-)module
		this.ptDriverPrefixAnalyzer = new PtDriverPrefixAnalyzer();
		this.ptDriverPrefixAnalyzer.init(scenario);
		this.anaModules.add(ptDriverPrefixAnalyzer);
		
		this.handler = new StopId2PersonEnterLeaveVehicleHandler(this.ptDriverPrefixAnalyzer);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();

		// from (sub-)modules
		for (AbstractAnalyisModule module : this.anaModules) {
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
		for (AbstractAnalyisModule module : this.anaModules) {
			module.preProcessData();
		}
		log.info("Preprocessing all (sub-)modules... done.");
	}

	@Override
	public void postProcessData() {
		log.info("Postprocessing all (sub-)modules...");
		for (AbstractAnalyisModule module : this.anaModules) {
			module.postProcessData();
		}
		log.info("Postprocessing all (sub-)modules... done.");
		
		this.stopId2PersonEnterEvent = this.handler.getStopId2PersonEnterEventMap();
		this.stopId2PersonLeaveEvent = this.handler.getStopId2PersonLeaveEventMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public Map<Id, List<PersonEntersVehicleEvent>> getStopId2PersonEnterEvent() {
		return stopId2PersonEnterEvent;
	}

	public Map<Id, List<PersonLeavesVehicleEvent>> getStopId2PersonLeaveEvent() {
		return stopId2PersonLeaveEvent;
	}

	
}
