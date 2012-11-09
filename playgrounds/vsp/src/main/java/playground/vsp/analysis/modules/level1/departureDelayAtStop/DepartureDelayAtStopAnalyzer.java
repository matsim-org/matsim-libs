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
package playground.vsp.analysis.modules.level1.departureDelayAtStop;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * This module analyzes for each agent the time departing from a pt interaction and entering a public vehicle.
 * 
 * @author ikaddoura
 *
 */
public class DepartureDelayAtStopAnalyzer extends AbstractAnalyisModule{
	private final static Logger log = Logger.getLogger(DepartureDelayAtStopAnalyzer.class);
	private ScenarioImpl scenario;
	private DepartureDelayAtStopHandler departureDelayAtStopHandler;
	
	private TreeMap<Id, DepartureDelayAtStopData> personId2DelayAtStopMap;
		
	public DepartureDelayAtStopAnalyzer(String ptDriverPrefix) {
		super(DepartureDelayAtStopAnalyzer.class.getSimpleName(), ptDriverPrefix);
	}
	
	public void init(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.departureDelayAtStopHandler = new DepartureDelayAtStopHandler(this.ptDriverPrefix);
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.departureDelayAtStopHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		
		this.personId2DelayAtStopMap = this.departureDelayAtStopHandler.getPersonId2DelayAtStopMap();
		// TODO: calculate delays (e.g. for each person: avg. delay between pt interaction and entering the public vehicle)
		
	}

	@Override
	public void writeResults(String outputFolder) {
		// TODO
	}
	
}
