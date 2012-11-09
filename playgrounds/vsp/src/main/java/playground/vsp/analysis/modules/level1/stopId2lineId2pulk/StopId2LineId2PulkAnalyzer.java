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
package playground.vsp.analysis.modules.level1.stopId2lineId2pulk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * This module analyzes pt trip travel times.
 * 
 * @author ikaddoura
 *
 */
public class StopId2LineId2PulkAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(StopId2LineId2PulkAnalyzer.class);
	private ScenarioImpl scenario;
	private StopId2LineId2PulkEventHandler pulkHandler;
	private TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> stopId2LineId2PulkDataList;
			
	public StopId2LineId2PulkAnalyzer(String ptDriverPrefix) {
		super(StopId2LineId2PulkAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.pulkHandler = new StopId2LineId2PulkEventHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.pulkHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.stopId2LineId2PulkDataList = this.pulkHandler.getStopId2LineId2PulkDataList();
		// ...
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

}
