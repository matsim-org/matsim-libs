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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.core.config.ConfigUtils;

/**
 * @author Ihab
 *
 */
public class OperatorUserAnalysis {
	private String lastEventFile;
	private final static Logger log = Logger.getLogger(OperatorUserAnalysis.class);
	private Network network;
	
	public OperatorUserAnalysis(String directoryExtIt, int lastInternalIteration, String networkFile) {
	
		this.lastEventFile = directoryExtIt+"/internalIterations/ITERS/it."+lastInternalIteration+"/"+lastInternalIteration+".events.xml.gz";
		
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(scen);		
		this.network = scen.getNetwork();
	}
	
	public void readEvents(Operator operator, Users users, Map<Integer, TimePeriod> day){
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		DepartureArrivalEventHandler departureHandler = new DepartureArrivalEventHandler();
		MoneyEventHandler moneyHandler = new MoneyEventHandler();
		TransitEventHandler transitHandler = new TransitEventHandler();
		LinksEventHandler linksHandler = new LinksEventHandler(network);
		
		events.addHandler(moneyHandler);	
		events.addHandler(transitHandler);
		events.addHandler(linksHandler);		
		events.addHandler(departureHandler);	
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(lastEventFile);
		
		users.setNumberOfPtLegs(departureHandler.getNumberOfPtLegs());
		users.setNumberOfCarLegs(departureHandler.getNumberOfCarLegs());
		users.setNumberOfWalkLegs(departureHandler.getNumberOfWalkLegs());
		log.info("Users analyzed.");
		
		operator.setRevenue(moneyHandler.getRevenues());
		operator.setNumberOfBusesFromEvents(transitHandler.getVehicleIDs().size()); // Anzahl der Busse aus den Events!		
		operator.setVehicleKm(linksHandler.getVehicleKm()); // vehicle-km aus den Events!
		operator.setVehicleHours(departureHandler.getVehicleHours()); // vehicle-hours aus den Events, nicht aus dem Fahrplan!
		linksHandler.setTakt(day);
		log.info("Operator analyzed.");
		
	}
}
