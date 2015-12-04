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
package playground.vsp.analysis.modules.bvgAna.anaLevel2.stopId2delayOfLine24h;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2RouteId2DelayAtStop.StopId2RouteId2DelayAtStopAnalyzer;
import playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2RouteId2DelayAtStop.StopId2RouteId2DelayAtStopData;

/**
 * 
 * @author ikaddoura, andreas
 *
 */
public class StopId2DelayOfLine24hAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(StopId2DelayOfLine24hAnalyzer.class);
	private MutableScenario scenario;

	private List<AbstractAnalysisModule> anaModules = new LinkedList<AbstractAnalysisModule>();
	private StopId2RouteId2DelayAtStopAnalyzer stopId2RouteId2DelayAtStopAnalyzer;
	
	private TreeMap<Id, StopId2DelayOfLine24hData> stopId2DelayOfLine24hMap;
			
	public StopId2DelayOfLine24hAnalyzer() {
		super(StopId2DelayOfLine24hAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		this.stopId2RouteId2DelayAtStopAnalyzer = new StopId2RouteId2DelayAtStopAnalyzer();
		this.stopId2RouteId2DelayAtStopAnalyzer.init(scenario);
		this.anaModules.add(stopId2RouteId2DelayAtStopAnalyzer);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();
		for (AbstractAnalysisModule module : this.anaModules) {
			for (EventHandler handler : module.getEventHandler()) {
				allEventHandler.add(handler);
			}
		}
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
		
		// own postProcessing
		TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopData>> results = this.stopId2RouteId2DelayAtStopAnalyzer.getStopId2RouteId2DelayAtStop();
		this.stopId2DelayOfLine24hMap = new TreeMap<Id, StopId2DelayOfLine24hData>();
		
		for (Entry<Id, TreeMap<Id, StopId2RouteId2DelayAtStopData>> stopEntry : results.entrySet()) {
			StopId2DelayOfLine24hData stopId2DelayOfLine24hMapData = new StopId2DelayOfLine24hData(stopEntry.getKey());
			this.stopId2DelayOfLine24hMap.put(stopEntry.getKey(), stopId2DelayOfLine24hMapData);
			
			for (StopId2RouteId2DelayAtStopData routeData : stopEntry.getValue().values()) {
				Id lineId = routeData.getLineId();
				
				ArrayList<Double> plannedDepartures = routeData.getPlannedDepartures();
				ArrayList<Double> realizedDepartures = routeData.getRealizedDepartures();
				
				if(plannedDepartures.size() != realizedDepartures.size()){
					this.log.error("Number of planned and realized deparutes NOT the same.");
				}
				
				for (int i = 0; i < plannedDepartures.size(); i++) {
					stopId2DelayOfLine24hMapData.addDelayForLine(lineId, realizedDepartures.get(i).doubleValue() - plannedDepartures.get(i).doubleValue());
				}				
			}			
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		
//		for (Id stopId : this.stopId2DelayOfLine24hMap.keySet()) {
//			log.info(this.stopId2DelayOfLine24hMap.get(stopId).toString());
//		}
		
	}

	public TreeMap<Id, StopId2DelayOfLine24hData> getStopId2DelayOfLine24hMap() {
		return stopId2DelayOfLine24hMap;
	}

}
