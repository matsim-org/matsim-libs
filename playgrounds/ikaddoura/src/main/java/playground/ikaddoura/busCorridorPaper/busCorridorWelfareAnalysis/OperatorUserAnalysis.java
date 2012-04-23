/* *********************************************************************** *
 * project: org.matsim.*
 * Provider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
 */
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */
public class OperatorUserAnalysis {
	private final static Logger log = Logger.getLogger(OperatorUserAnalysis.class);

	DepartureArrivalEventHandler departureHandler;
	MoneyEventHandler moneyHandler;
	TransitEventHandler transitHandler;
	LinksEventHandler linksHandler;
	
	private final String lastEventFile;
	private final Network network;
	
	public OperatorUserAnalysis(String directoryExtIt, int lastInternalIteration, String networkFile) {
		this.lastEventFile = directoryExtIt + "/internalIterations/ITERS/it." + lastInternalIteration + "/" + lastInternalIteration + ".events.xml.gz";
		
		Scenario scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scen);		
		this.network = scen.getNetwork();
	}
	
	public void readEvents(Operator operator, Users users){
		
		EventsManager events = EventsUtils.createEventsManager();
		this.departureHandler = new DepartureArrivalEventHandler();
		this.moneyHandler = new MoneyEventHandler();
		this.transitHandler = new TransitEventHandler();
		this.linksHandler = new LinksEventHandler(this.network);
		
		events.addHandler(this.departureHandler);	
		events.addHandler(this.moneyHandler);	
		events.addHandler(this.transitHandler);
		events.addHandler(this.linksHandler);		
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.lastEventFile);
		
//		linksHandler.setTakt(day);
		
	}

	protected double getVehicleHours() {
		return this.departureHandler.getVehicleHours();
	}

	protected double getVehicleKm() {
		return linksHandler.getVehicleKm();
	}

	protected int getNumberOfBusesFromEvents() {
		return transitHandler.getVehicleIDs().size();
	}

	protected double getRevenue() {
		return moneyHandler.getRevenues();
	}

	protected int getSumOfWalkLegs() {
		return this.departureHandler.getNumberOfWalkLegs();
	}

	protected int getSumOfCarLegs() {
		return departureHandler.getNumberOfCarLegs();
	}

	protected int getSumOfPtLegs() {
		return departureHandler.getNumberOfPtLegs();
	}
}
