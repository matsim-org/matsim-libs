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
package playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2RouteId2DelayAtStop;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2RouteId2DelayAtStop.StopId2RouteId2DelayAtStopHandler;

/**
 * 
 * @author ikaddoura, aneumann
 *
 */
public class StopId2RouteId2DelayAtStopAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(StopId2RouteId2DelayAtStopAnalyzer.class);
	private MutableScenario scenario;
	private StopId2RouteId2DelayAtStopHandler delayHandler;
	private TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopData>> stopId2RouteId2DelayAtStop;
				
	public StopId2RouteId2DelayAtStopAnalyzer() {
		super(StopId2RouteId2DelayAtStopAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
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
	}

	@Override
	public void postProcessData() {
		this.stopId2RouteId2DelayAtStop = this.delayHandler.getStopId2RouteId2DelayAtStopMap();
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopData>> getStopId2RouteId2DelayAtStop() {
		return stopId2RouteId2DelayAtStop;
	}
	
}
