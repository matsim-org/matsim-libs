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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * 
 * @author ikaddoura
 *
 */
public class StopId2DelayOfLine24hAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(StopId2DelayOfLine24hAnalyzer.class);
	private MutableScenario scenario;
	private StopId2RemainSeatedHandler remainSeatedHandler;
	private Map<Id, List<StopId2RemainSeatedData>> stopId2RemainSeated;
	
	public StopId2DelayOfLine24hAnalyzer() {
		super(StopId2DelayOfLine24hAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
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
		this.stopId2RemainSeated = this.remainSeatedHandler.getStopId2RemainSeatedDataMap();
		
	}

	@Override
	public void writeResults(String outputFolder) {
		for (Id stopId : this.remainSeatedHandler.getStopId2RemainSeatedDataMap().keySet()) {
			log.info(this.remainSeatedHandler.getStopId2RemainSeatedDataMap().get(stopId).toString());
		}
	}

}
