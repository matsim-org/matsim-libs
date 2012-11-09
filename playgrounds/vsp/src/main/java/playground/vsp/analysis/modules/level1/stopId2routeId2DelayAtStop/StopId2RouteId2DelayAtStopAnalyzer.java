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
package playground.vsp.analysis.modules.level1.stopId2routeId2DelayAtStop;

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
 * This module collects the planned and realized departures at one stop for a specific route of a line.
 * <b>All</b> departures will be taken into account, regardless of the line or route served.
 * 
 * @author ikaddoura
 *
 */
public class StopId2RouteId2DelayAtStopAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(StopId2RouteId2DelayAtStopAnalyzer.class);
	private ScenarioImpl scenario;
	private StopId2RouteId2DelayAtStopHandler delayHandler;
	private TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopData>> stopId2RouteId2DelayAtStop;
	
			
	public StopId2RouteId2DelayAtStopAnalyzer(String ptDriverPrefix) {
		super(StopId2RouteId2DelayAtStopAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.delayHandler = new StopId2RouteId2DelayAtStopHandler();
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
		this.stopId2RouteId2DelayAtStop = this.delayHandler.getStopId2RouteId2DelayAtStopMap();
		// TODO: analyze delays...
	}

	@Override
	public void writeResults(String outputFolder) {
		for (Id stopId : this.stopId2RouteId2DelayAtStop.keySet()){
			log.info(stopId + ": " + this.stopId2RouteId2DelayAtStop.get(stopId).toString());
		}
	}

}
