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
package playground.ikaddoura.economics;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scenario.MutableScenario;


/**
 *
 * @author ikaddoura
 *
 */
public class PricingHandler implements PersonDepartureEventHandler {

	private final static Logger log = Logger.getLogger(PricingHandler.class);

	private final EventsManager events;
	private final MutableScenario scenario;
	private final double vtts_car;
	private double toll;

	public PricingHandler(EventsManager eventsManager, MutableScenario scenario, double toll) {
		this.events = eventsManager;
		this.scenario = scenario;
		this.vtts_car = (this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.toll = toll;
		log.info("VTTS_car: " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
	// ...
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			double amount = -1 * toll;
			
			PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount);
			this.events.processEvent(moneyEvent);
		}
	}
	
}