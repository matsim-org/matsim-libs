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
package playground.agarwalamit.munich.controlerListner;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import playground.benjamin.internalization.EmissionCostModule;

/**
 * @author amit
 */

public class EmissionCostsCollector implements WarmEmissionEventHandler, ColdEmissionEventHandler{
	private EmissionCostModule emissionCostModule;
	private Map<Id<Person>, Double> personId2ColdEmissCosts = new HashMap<>();
	private Map<Id<Person>, Double> personId2WarmEmissCosts = new HashMap<>();

	public EmissionCostsCollector(EmissionCostModule emissionCostModule) {
		this.emissionCostModule = emissionCostModule;
	}

	@Override
	public void reset(int iteration) {
		this.personId2ColdEmissCosts.clear();
		this.personId2WarmEmissCosts.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
		double warmEmissionCosts = emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions());
		double amount2Pay = - warmEmissionCosts;

		if(this.personId2WarmEmissCosts.containsKey(personId)){
			double nowCost = this.personId2WarmEmissCosts.get(personId);
			this.personId2WarmEmissCosts.put(personId, nowCost+amount2Pay);
		} else {
			this.personId2WarmEmissCosts.put(personId, amount2Pay);
		}
	}

	public Map<Id<Person>, Double> getPersonId2ColdEmissCosts() {
		return personId2ColdEmissCosts;
	}

	public Map<Id<Person>, Double> getPersonId2WarmEmissCosts() {
		return personId2WarmEmissCosts;
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
		double coldEmissionCosts = emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions());
		double amount2Pay = - coldEmissionCosts;
		if(this.personId2ColdEmissCosts.containsKey(personId)){
			double nowCost = this.personId2ColdEmissCosts.get(personId);
			this.personId2ColdEmissCosts.put(personId, nowCost+amount2Pay);
		} else {
			this.personId2ColdEmissCosts.put(personId, amount2Pay);
		}
	}
}
