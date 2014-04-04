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
package playground.agarwalamit.siouxFalls;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */
public class CongestionLinkAnalyzer extends AbstractAnalyisModule {
	private final static Logger logger = Logger.getLogger(CongestionLinkAnalyzer.class);
	private final String eventsFile;
	private CongestionPerLinkHandler congestionPerLinkHandler;
	private final int noOfTimeBins;
	private final String configFile;
	private Map<Double, Map<Id, Double>> congestionPerLinkTimeInterval;
	private EventsManager eventsManager;

	public CongestionLinkAnalyzer(String configFile, String eventFile, int noOfTimeBins) {
		super(CongestionLinkAnalyzer.class.getSimpleName());
		this.eventsFile = eventFile;
		this.configFile = configFile;
		this.noOfTimeBins = noOfTimeBins;
	}
	public void init(Scenario scenario){
		this.congestionPerLinkHandler = new CongestionPerLinkHandler(this.noOfTimeBins, getEndTime(this.configFile), scenario);
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
	private Double getEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating delays for " + (int) (endTime / 3600 / this.noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	public Map<Double, Map<Id, Double>> getCongestionPerLinkTimeInterval() {
		return this.congestionPerLinkTimeInterval;
	}
}
