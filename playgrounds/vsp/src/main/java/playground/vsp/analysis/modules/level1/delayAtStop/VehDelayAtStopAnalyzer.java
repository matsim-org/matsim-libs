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
 * This module collects <code>VehicleDepartsAtFacilityEvent</code> for each vehicle.
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.level1.delayAtStop;

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
 * This module collects <code>VehicleDepartsAtFacilityEvent</code> for each vehicle.
 * 
 * @author ikaddoura, aneumann
 *
 */
public class VehDelayAtStopAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(VehDelayAtStopAnalyzer.class);
	private ScenarioImpl scenario;
	private VehId2DelayAtStopEventHandler delayHandler;
	private TreeMap<Id, LinkedList<VehId2DelayAtStopData>> vehId2DelayAtStopData;	
	
	public VehDelayAtStopAnalyzer(String ptDriverPrefix) {
		super(VehDelayAtStopAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.delayHandler = new VehId2DelayAtStopEventHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.delayHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.vehId2DelayAtStopData = this.delayHandler.getVehId2DelayAtStopMap();
		// e.g. analyze avg delay per stop
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}
	
	public TreeMap<Id, LinkedList<VehId2DelayAtStopData>> getVehId2DelayAtStopData() {
		return vehId2DelayAtStopData;
	}

}
