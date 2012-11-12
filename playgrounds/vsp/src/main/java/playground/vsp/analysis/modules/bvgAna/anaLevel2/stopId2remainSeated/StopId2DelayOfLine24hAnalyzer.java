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
package playground.vsp.analysis.modules.bvgAna.anaLevel2.stopId2remainSeated;

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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.StopId2RouteId2DelayAtStopData;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.StopId2RouteId2DelayAtStopHandler;

/**
 * 
 * @author ikaddoura
 *
 */
public class StopId2DelayOfLine24hAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(StopId2DelayOfLine24hAnalyzer.class);
	private ScenarioImpl scenario;
	private StopId2RemainSeatedHandler remainSeatedHandler;
	
	public StopId2DelayOfLine24hAnalyzer(String ptDriverPrefix) {
		super(StopId2DelayOfLine24hAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.remainSeatedHandler = new StopId2RemainSeatedHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.remainSeatedHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
	}

	@Override
	public void postProcessData() {
		
	}

	@Override
	public void writeResults(String outputFolder) {
		for (Id stopId : this.remainSeatedHandler.getStopId2RemainSeatedDataMap().keySet()) {
			log.info(this.remainSeatedHandler.getStopId2RemainSeatedDataMap().get(stopId).toString());
		}
	}

}
