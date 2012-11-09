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
package playground.vsp.analysis.modules.level1.stopId2personEnterLeaveVehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * This module collects <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEventHandler</code> for each stop id.
 * 
 * @author ikaddoura
 *
 */
public class StopId2PersonEnterLeaveVehicleAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(StopId2PersonEnterLeaveVehicleAnalyzer.class);
	private ScenarioImpl scenario;
	private StopId2PersonEnterLeaveVehicleHandler enterLeaveHandler;
	
	private Map<Id, List<PersonEntersVehicleEvent>> stopId2PersonEnterEventMap;
	private Map<Id, List<PersonLeavesVehicleEvent>> stopId2PersonLeaveEventMap;
			
	public StopId2PersonEnterLeaveVehicleAnalyzer(String ptDriverPrefix) {
		super(StopId2PersonEnterLeaveVehicleAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.enterLeaveHandler = new StopId2PersonEnterLeaveVehicleHandler(this.ptDriverPrefix);
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
		this.stopId2PersonEnterEventMap = this.enterLeaveHandler.getStopId2PersonEnterEventMap();
		this.stopId2PersonLeaveEventMap = this.enterLeaveHandler.getStopId2PersonLeaveEventMap();
		// ...
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

}
