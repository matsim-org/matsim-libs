/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.congestion;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.congestion.handlers.MarginalCongestionHandlerImplV3;

/**
 * @author amit
 */
public class CongestionLinkAnalyzer extends AbstractAnalyisModule {
	private final String eventsFile;
	private CongestionPerLinkHandler congestionPerLinkHandler;
	private final int noOfTimeBins;
	private Map<Double, Map<Id, Double>> congestionPerLinkTimeInterval;
	private EventsManager eventsManager;
	private Scenario scenario;
	private double simulationEndTime;

	public CongestionLinkAnalyzer(double simulationEndTime, String eventFile, int noOfTimeBins) {
		super(CongestionLinkAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.noOfTimeBins = noOfTimeBins;
		this.simulationEndTime = simulationEndTime;
	}
	
	public void run(Scenario scenario){
		init(scenario);
		preProcessData();
		postProcessData();
		checkTotalDelayUsingAlternativeMethod();
	}
	
	public void init(Scenario scenario){
		this.scenario = scenario;
		this.congestionPerLinkHandler = new CongestionPerLinkHandler(this.noOfTimeBins, this.simulationEndTime , this.scenario);
	}
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		return handler;
	}

	@Override
	public void preProcessData() {
		this.eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventsReader = new MatsimEventsReader(this.eventsManager);
		this.eventsManager.addHandler(this.congestionPerLinkHandler);
		eventsReader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.congestionPerLinkTimeInterval= this.congestionPerLinkHandler.getDelayPerLinkAndTimeInterval();
	}

	@Override
	public void writeResults(String outputFolder) {
		
	}

	public double getTotalDelaysInHours (){
		return this.congestionPerLinkHandler.getTotalDelayInHours();
	}
	
	public Map<Double, Map<Id, Double>> getCongestionPerLinkTimeInterval() {
		return this.congestionPerLinkTimeInterval;
	}
	
	public void checkTotalDelayUsingAlternativeMethod(){
		EventsManager em = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV3 implV3 = new MarginalCongestionHandlerImplV3(em, (ScenarioImpl) scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(implV3);
		eventsReader.readFile(this.eventsFile);
		if(implV3.getTotalDelay()/3600!=this.congestionPerLinkHandler.getTotalDelayInHours())
			throw new RuntimeException("Total Delays are not equal using two methods; values are "+implV3.getTotalDelay()/3600+","+this.congestionPerLinkHandler.getTotalDelayInHours());
	}
}
