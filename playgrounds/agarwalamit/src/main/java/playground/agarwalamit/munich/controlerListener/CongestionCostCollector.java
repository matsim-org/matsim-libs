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
package playground.agarwalamit.munich.controlerListener;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * @author amit
 */

public class CongestionCostCollector implements CongestionEventHandler {
	private final static Logger LOG = Logger.getLogger(CongestionCostCollector.class);

	private final double vttsCar;
	private double amountSum = 0.;
	private final Map<Id<Person>, Double> causingPerson2Cost = new HashMap<>();

	public CongestionCostCollector(MutableScenario scenario) {
        this.vttsCar = (scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario
				.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		LOG.info("VTTS_car: " + vttsCar);
	}

	@Override
	public void reset(int iteration) {
		this.amountSum = 0.;
		this.causingPerson2Cost.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {

		double amount = event.getDelay() / 3600 * this.vttsCar;
		this.amountSum = this.amountSum + amount;
		Id<Person> causingPerson = Id.createPersonId(event.getCausingAgentId().toString()); 

		if(this.causingPerson2Cost.containsKey(causingPerson)){
			double costNow = this.causingPerson2Cost.get(causingPerson);
			this.causingPerson2Cost.put(causingPerson, costNow+amount);
		} else {
			this.causingPerson2Cost.put(causingPerson, amount);
		}
	}

	public Map<Id<Person>, Double> getCausingPerson2Cost() {
		return causingPerson2Cost;
	}

	public double getAmountSum() {
		return amountSum;
	}
}
