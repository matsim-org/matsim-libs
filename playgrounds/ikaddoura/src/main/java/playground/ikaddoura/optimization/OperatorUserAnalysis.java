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
package playground.ikaddoura.optimization;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.ikaddoura.optimization.handler.CarCongestionHandlerAdvanced;
import playground.ikaddoura.optimization.handler.FareData;
import playground.ikaddoura.optimization.handler.MoneyDetailEventHandler;
import playground.ikaddoura.optimization.handler.WaitingTimeHandler;
import playground.ikaddoura.optimization.operatorProfitModel.DepartureArrivalEventHandler;
import playground.ikaddoura.optimization.operatorProfitModel.LinksEventHandler;
import playground.ikaddoura.optimization.operatorProfitModel.MoneyEventHandler;
import playground.ikaddoura.optimization.operatorProfitModel.TransitEventHandler;

/**
 * @author Ihab
 *
 */
public class OperatorUserAnalysis {

	private DepartureArrivalEventHandler departureHandler;
	private MoneyEventHandler moneyHandler;
	private MoneyDetailEventHandler moneyDetailHandler;
	private TransitEventHandler transitHandler;
	private LinksEventHandler linksHandler;
	private WaitingTimeHandler waitHandler;
	private CarCongestionHandlerAdvanced congestionHandler;
	
	private final String lastEventFile;
	private final Network network;
	private final Double headway;
	
	public OperatorUserAnalysis(Scenario scenario, Double headway) {
		int lastInternalIteration = scenario.getConfig().controler().getLastIteration();
		this.lastEventFile = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + lastInternalIteration + "/" + lastInternalIteration + ".events.xml.gz";
		this.network = scenario.getNetwork();
		this.headway = headway;
	}
	
	public void readEvents(){
		EventsManager events = EventsUtils.createEventsManager();
		this.departureHandler = new DepartureArrivalEventHandler();
		this.moneyHandler = new MoneyEventHandler();
		this.moneyDetailHandler = new MoneyDetailEventHandler();
		this.transitHandler = new TransitEventHandler();
		this.linksHandler = new LinksEventHandler(this.network);
		this.waitHandler = new WaitingTimeHandler(headway);
		this.congestionHandler = new CarCongestionHandlerAdvanced(this.network);
		
		events.addHandler(this.departureHandler);	
		events.addHandler(this.moneyHandler);
		events.addHandler(this.moneyDetailHandler);
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
	
	public List<FareData> getFareData() {
		return this.moneyHandler.getfareDataList();
	}
	
	public Double getAverageFarePerAgent() {
		return this.moneyHandler.getAverageAmountPerPerson();
	}
	
	public Map<Double, Double> getAvgFarePerDepartureTimePeriod() {
		return this.moneyDetailHandler.getAvgFarePerTripDepartureTime();
	}

	public Map<Id, Double> getFirstTripFares() {
		return this.moneyDetailHandler.getPersonId2fareFirstTrip();
	}
	
	public Map<Id, Double> getSecondTripFares() {
		return this.moneyDetailHandler.getPersonId2fareSecondTrip();
	}
}
