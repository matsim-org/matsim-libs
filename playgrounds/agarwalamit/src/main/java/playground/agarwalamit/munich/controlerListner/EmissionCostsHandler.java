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
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;

/**
 * @author amit
 */

public class EmissionCostsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler{
	private final EmissionCostModule emissionCostModule;
	private final Map<Id<Vehicle>, Double> vehicleId2ColdEmissCosts = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2WarmEmissCosts = new HashMap<>();

	private final MunichPersonFilter pf = new MunichPersonFilter();

	public EmissionCostsHandler(EmissionCostModule emissionCostModule) {
		this.emissionCostModule = emissionCostModule;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ColdEmissCosts.clear();
		this.vehicleId2WarmEmissCosts.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		double warmEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions());
		double amount2Pay = - warmEmissionCosts;

		if(this.vehicleId2WarmEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2WarmEmissCosts.get(vehicleId);
			this.vehicleId2WarmEmissCosts.put(vehicleId, nowCost+amount2Pay);
		} else {
			this.vehicleId2WarmEmissCosts.put(vehicleId, amount2Pay);
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		double coldEmissionCosts = this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions());
		double amount2Pay = - coldEmissionCosts;

		if(this.vehicleId2ColdEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2ColdEmissCosts.get(vehicleId);
			this.vehicleId2ColdEmissCosts.put(vehicleId, nowCost+amount2Pay);
		} else {
			this.vehicleId2ColdEmissCosts.put(vehicleId, amount2Pay);
		}
	}

	@Deprecated
	public Map<Id<Person>, Double> getPersonId2ColdEmissionsCosts() {
		final Map<Id<Person>, Double> personId2ColdEmissCosts =	this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2ColdEmissCosts;
	}

	@Deprecated
	public Map<Id<Person>, Double> getPersonId2WarmEmissionsCosts() {
		final Map<Id<Person>, Double> personId2WarmEmissCosts =	this.vehicleId2WarmEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2WarmEmissCosts;
	}

	public Map<Id<Vehicle>, Double> getVehicleId2TotalEmissionsCosts(){
		return this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.vehicleId2WarmEmissCosts.get(entry.getKey()))
		);
	}

	public Map<String, Double> getUserGroup2WarmEmissionsCost(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		for (Map.Entry<Id<Person>, Double> entry : getPersonId2WarmEmissionsCosts().entrySet()) {
			String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
			usrGrp2Cost.put(ug,   usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
		}
		return usrGrp2Cost;
	}

	public Map<String, Double> getUserGroup2ColdEmissionsCost(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		for (Map.Entry<Id<Person>, Double> entry : getPersonId2ColdEmissionsCosts().entrySet()) {
			String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
			usrGrp2Cost.put(ug,   usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
		}
		return usrGrp2Cost;
	}

	public Map<Id<Vehicle>, Double> getVehicleId2ColdEmissionsCosts() {
		return this.vehicleId2ColdEmissCosts;
	}

	public Map<Id<Vehicle>, Double> getVehicleId2WarmEmissionsCosts() {
		return this.vehicleId2WarmEmissCosts;
	}
}
