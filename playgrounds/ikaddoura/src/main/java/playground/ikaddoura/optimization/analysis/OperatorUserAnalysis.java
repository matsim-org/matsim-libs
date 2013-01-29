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
package playground.ikaddoura.optimization.analysis;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.ikaddoura.optimization.handler.CarCongestionHandlerAdvanced;
import playground.ikaddoura.optimization.handler.DepartureArrivalEventHandler;
import playground.ikaddoura.optimization.handler.LinksEventHandler;
import playground.ikaddoura.optimization.handler.MoneyEventHandler;
import playground.ikaddoura.optimization.handler.TransitEventHandler;
import playground.ikaddoura.optimization.handler.WaitingTimeHandler;

/**
 * @author Ihab
 *
 */
public class OperatorUserAnalysis {
//	private final static Logger log = Logger.getLogger(OperatorUserAnalysis.class);

	DepartureArrivalEventHandler departureHandler;
	MoneyEventHandler moneyHandler;
	TransitEventHandler transitHandler;
	LinksEventHandler linksHandler;
	WaitingTimeHandler waitHandler;
	CarCongestionHandlerAdvanced congestionHandler;
	
	private final String lastEventFile;
	private final Network network;
	private final Double headway;
	
	public OperatorUserAnalysis(Scenario scenario, String directoryExtIt, Double headway) {
		int lastInternalIteration = scenario.getConfig().controler().getLastIteration();
		this.lastEventFile = directoryExtIt + "/internalIterations/ITERS/it." + lastInternalIteration + "/" + lastInternalIteration + ".events.xml.gz";
		this.network = scenario.getNetwork();
		this.headway = headway;
	}
	
	public void readEvents(){
		EventsManager events = EventsUtils.createEventsManager();
		this.departureHandler = new DepartureArrivalEventHandler();
		this.moneyHandler = new MoneyEventHandler();
		this.transitHandler = new TransitEventHandler();
		this.linksHandler = new LinksEventHandler(this.network);
		this.waitHandler = new WaitingTimeHandler(headway);
		this.congestionHandler = new CarCongestionHandlerAdvanced(this.network);
		
		events.addHandler(this.departureHandler);	
		events.addHandler(this.moneyHandler);	
		events.addHandler(this.transitHandler);
		events.addHandler(this.linksHandler);
		events.addHandler(this.waitHandler);
		events.addHandler(this.congestionHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.lastEventFile);
	}

	public double getVehicleHours() {
		return this.departureHandler.getVehicleHours();
	}

	public double getVehicleKm() {
		return linksHandler.getVehicleKm();
	}

	public int getNumberOfBusesFromEvents() {
		return transitHandler.getVehicleIDs().size();
	}

	public double getRevenue() {
		return moneyHandler.getRevenues();
	}

	public int getSumOfWalkLegs() {
		return this.departureHandler.getNumberOfWalkLegs();
	}

	public int getSumOfCarLegs() {
		return departureHandler.getNumberOfCarLegs();
	}

	public int getSumOfPtLegs() {
		return departureHandler.getNumberOfPtLegs();
	}

	public WaitingTimeHandler getWaitHandler() {
		return waitHandler;
	}

	public CarCongestionHandlerAdvanced getCongestionHandler() {
		return congestionHandler;
	}
	
}
