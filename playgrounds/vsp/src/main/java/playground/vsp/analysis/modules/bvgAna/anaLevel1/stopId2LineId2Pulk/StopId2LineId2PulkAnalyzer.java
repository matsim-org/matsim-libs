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
package playground.vsp.analysis.modules.bvgAna.anaLevel1.stopId2LineId2Pulk;


import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author ikaddoura
 *
 */
public class StopId2LineId2PulkAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(StopId2LineId2PulkAnalyzer.class);
	private MutableScenario scenario;
	
	private StopId2LineId2PulkHandler handler;
	private TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> stopId2LineId2Pulk;
	
	public StopId2LineId2PulkAnalyzer() {
		super(StopId2LineId2PulkAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		this.handler = new StopId2LineId2PulkHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> allEventHandler = new LinkedList<EventHandler>();
		allEventHandler.add(this.handler);
		return allEventHandler;
	}

	@Override
	public void preProcessData() {
		
	}

	@Override
	public void postProcessData() {
		this.stopId2LineId2Pulk = this.handler.getStopId2LineId2PulkDataList();	
	}

	@Override
	public void writeResults(String outputFolder) {
		// ...
	}

	public TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> getStopId2LineId2Pulk() {
		return stopId2LineId2Pulk;
	}

}
