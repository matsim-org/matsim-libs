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
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

/**
 * This analyzer calculates delay from link enter and link leave events and therefore provides only experienced delay.
 * <p> In order to get the caused delay for each person, see {@link CausedDelayAnalyzer}
 * 
 * @author amit
 */
public class ExperiencedDelayAnalyzer extends AbstractAnalyisModule {
	
	private final Logger logger = Logger.getLogger(ExperiencedDelayAnalyzer.class);
	private final String eventsFile;
	private ExperiencedDelayHandler congestionPerPersonHandler;
	private final int noOfTimeBins;
	private SortedMap<Double, Map<Id<Person>, Double>> congestionPerPersonTimeInterval;
	private Map<Double, Map<Id<Link>, Double>> congestionPerLinkTimeInterval;
	private EventsManager eventsManager;
	private Scenario scenario;
	private boolean isSortingForInsideMunich = false;

	public ExperiencedDelayAnalyzer(String eventFile, int noOfTimeBins) {
		super(ExperiencedDelayAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.noOfTimeBins = noOfTimeBins;
	}
	
	public ExperiencedDelayAnalyzer(String eventFile, int noOfTimeBins, boolean isSortingForInsideMunich) {
		super(ExperiencedDelayAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.noOfTimeBins = noOfTimeBins;
		this.isSortingForInsideMunich = isSortingForInsideMunich;
	}
	
	public void run(Scenario scenario){
		init(scenario);
		preProcessData();
		postProcessData();
		checkTotalDelayUsingAlternativeMethod();
	}
	
	public void init(Scenario scenario){
		this.scenario = scenario;
		this.congestionPerPersonHandler = new ExperiencedDelayHandler(this.noOfTimeBins,  this.scenario, isSortingForInsideMunich);
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
		this.eventsManager.addHandler(this.congestionPerPersonHandler);
		eventsReader.readFile(this.eventsFile);
	}

	@Override
	public void postProcessData() {
		this.congestionPerPersonTimeInterval= this.congestionPerPersonHandler.getDelayPerPersonAndTimeInterval();
		this.congestionPerLinkTimeInterval= this.congestionPerPersonHandler.getDelayPerLinkAndTimeInterval();
	}

	@Override
	public void writeResults(String outputFolder) {

	}

	public double getTotalDelaysInHours (){
		return this.congestionPerPersonHandler.getTotalDelayInHours();
	}
	
	public SortedMap<Double, Map<Id<Person>, Double>> getCongestionPerPersonTimeInterval() {
		return this.congestionPerPersonTimeInterval;
	}
	
	public Map<Double, Map<Id<Link>, Double>> getCongestionPerLinkTimeInterval() {
		return this.congestionPerLinkTimeInterval;
	}
	
	public void checkTotalDelayUsingAlternativeMethod(){
		EventsManager em = EventsUtils.createEventsManager();
		CongestionHandlerImplV3 implV3 = new CongestionHandlerImplV3(em, (ScenarioImpl) this.scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(implV3);
		eventsReader.readFile(this.eventsFile);
		if(implV3.getTotalDelay()/3600!=this.congestionPerPersonHandler.getTotalDelayInHours())
			throw new RuntimeException("Total Delays are not equal using two methods; values are "+implV3.getTotalDelay()/3600+","+this.congestionPerPersonHandler.getTotalDelayInHours());
	}
}
