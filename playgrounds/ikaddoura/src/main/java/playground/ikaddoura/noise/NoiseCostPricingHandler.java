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
package playground.ikaddoura.noise;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;


/**
 * 
 * @author lkroeger
 *
 */
public class NoiseCostPricingHandler implements NoiseEventHandler {

//	private final static Logger log = Logger.getLogger(NoiseCostPricingHandler.class);

	private final EventsManager events;
	private List<PersonMoneyEvent> moneyEvents = new ArrayList<PersonMoneyEvent>();
	private double amountSum = 0.;

	public NoiseCostPricingHandler(EventsManager events) {
		this.events = events;
	}

	@Override
	public void reset(int iteration) {
		moneyEvents.clear();
		this.amountSum = 0.;
	}

	public double getAmountSum() {
		return amountSum;
	}

	@Override
	public void handleEvent(NoiseEvent event) {
		double amount = event.getAmount() *(-1);
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getVehicleId(), amount);
		this.events.processEvent(moneyEvent);
		moneyEvents.add(moneyEvent);
		
		amountSum = amountSum + amount;
	}
	
}