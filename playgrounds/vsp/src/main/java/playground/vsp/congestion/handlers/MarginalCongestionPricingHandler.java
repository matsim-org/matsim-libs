/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.congestion.handlers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.ScenarioImpl;

import playground.vsp.congestion.events.CongestionEvent;


/**
 * This handler calculates agent money events based on marginal congestion events.
 * The causing agent has to pay a toll depending on the number of affected agents and the external delay in sec.
 * The delay is monetized using the behavioral parameters from the scenario.
 * 
 * @author ikaddoura
 *
 */
public class MarginalCongestionPricingHandler implements CongestionEventHandler {

	private final static Logger log = Logger.getLogger(MarginalCongestionPricingHandler.class);

	private final EventsManager events;
	private final ScenarioImpl scenario;
	private final double vtts_car;
	private double amountSum = 0.;

	public MarginalCongestionPricingHandler(EventsManager eventsManager, ScenarioImpl scenario) {
		this.events = eventsManager;
		this.scenario = scenario;
		this.vtts_car = (this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("Using the same VTTS for each agent to translate delays into monetary units.");
		log.info("VTTS_car: " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
		this.amountSum = 0.;
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		
		double amount = event.getDelay() / 3600 * this.vtts_car;
		this.amountSum = this.amountSum + amount;
		
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgentId(), amount);
		this.events.processEvent(moneyEvent);
	}

	public double getAmountSum() {
		return amountSum;
	}
	
}