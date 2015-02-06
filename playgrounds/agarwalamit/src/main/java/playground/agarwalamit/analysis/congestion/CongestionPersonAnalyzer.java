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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.congestion.handlers.MarginalCongestionHandlerImplV3;

/**
 * @author amit
 */
public class CongestionPersonAnalyzer extends AbstractAnalyisModule {
	private final Logger logger = Logger.getLogger(CongestionPersonAnalyzer.class);
	private final String eventsFile;
	private CongestionPerPersonHandler congestionPerPersonHandler;
	private final int noOfTimeBins;
	private final String configFile;
	private Map<Double, Map<Id<Person>, Double>> congestionPerPersonTimeInterval;
	private EventsManager eventsManager;
	private Scenario scenario;

	public CongestionPersonAnalyzer(String configFile, String eventFile, int noOfTimeBins) {
		super(CongestionPersonAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.configFile = configFile;
		this.noOfTimeBins = noOfTimeBins;
	}
	public void init(Scenario scenario){
		this.scenario = scenario;
		this.congestionPerPersonHandler = new CongestionPerPersonHandler(this.noOfTimeBins, getEndTime(this.configFile), this.scenario);
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
	}

	@Override
	public void writeResults(String outputFolder) {

	}
	private Double getEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		this.logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		this.logger.info("Aggregating delays for " + (int) (endTime / 3600 / this.noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public double getTotalDelaysInHours (){
		return this.congestionPerPersonHandler.getTotalDelayInHours();
	}
	
	public Map<Double, Map<Id<Person>, Double>> getCongestionPerPersonTimeInterval() {
		return this.congestionPerPersonTimeInterval;
	}
	
	public void checkTotalDelayUsingAlternativeMethod(){
		EventsManager em = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV3 implV3 = new MarginalCongestionHandlerImplV3(em, (ScenarioImpl) this.scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(implV3);
		eventsReader.readFile(this.eventsFile);
		if(implV3.getTotalDelay()/3600!=this.congestionPerPersonHandler.getTotalDelayInHours())
			throw new RuntimeException("Total Delays are not equal using two methods; values are "+implV3.getTotalDelay()/3600+","+this.congestionPerPersonHandler.getTotalDelayInHours());
	}
}
