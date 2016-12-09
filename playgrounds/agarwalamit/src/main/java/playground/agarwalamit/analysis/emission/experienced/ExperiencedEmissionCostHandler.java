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
package playground.agarwalamit.analysis.emission.experienced;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.EmissionCostHandler;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;

/**
 * Emission costs (air pollution exposure cost module is used).
 *
 * @author amit
 */

public class ExperiencedEmissionCostHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler, EmissionCostHandler{

	private static final Logger LOG = Logger.getLogger(ExperiencedEmissionCostHandler.class);

	private final EmissionResponsibilityCostModule emissionCostModule;
	private final Map<Id<Vehicle>, Double> vehicleId2ColdEmissCosts = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2WarmEmissCosts = new HashMap<>();

	private final PersonFilter pf  ; // TODO : what if no person filter is available..?

	public ExperiencedEmissionCostHandler(final EmissionResponsibilityCostModule emissionCostModule) {
		this(emissionCostModule, null);
	}

	public ExperiencedEmissionCostHandler(final EmissionResponsibilityCostModule emissionCostModule, final PersonFilter pf) {
		this.emissionCostModule = emissionCostModule;
		this.pf = pf;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ColdEmissCosts.clear();
		this.vehicleId2WarmEmissCosts.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		double warmEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  warmEmissionCosts;

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
		double coldEmissionCosts = this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  coldEmissionCosts;

		if(this.vehicleId2ColdEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2ColdEmissCosts.get(vehicleId);
			this.vehicleId2ColdEmissCosts.put(vehicleId, nowCost+amount2Pay);
		} else {
			this.vehicleId2ColdEmissCosts.put(vehicleId, amount2Pay);
		}
	}

	public Map<Id<Person>, Double> getPersonId2ColdEmissionCosts() {
		final Map<Id<Person>, Double> personId2ColdEmissCosts =	this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2ColdEmissCosts;
	}

	public Map<Id<Person>, Double> getPersonId2WarmEmissionCosts() {
		final Map<Id<Person>, Double> personId2WarmEmissCosts =	this.vehicleId2WarmEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> Id.createPersonId(entry.getKey().toString()), entry -> entry.getValue())
		);
		return personId2WarmEmissCosts;
	}

	@Override
	public Map<Id<Person>, Double> getPersonId2TotalEmissionCosts() {
		return getPersonId2ColdEmissionCosts().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.getPersonId2WarmEmissionCosts().get(entry.getKey()))
		);
	}

	@Override
	public Map<Id<Vehicle>, Double> getVehicleId2TotalEmissionCosts(){
		return this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.vehicleId2WarmEmissCosts.get(entry.getKey()))
		);
	}

	public Map<String, Double> getUserGroup2WarmEmissionCosts(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		if (this.pf != null) {
			for (Map.Entry<Id<Person>, Double> entry : getPersonId2WarmEmissionCosts().entrySet()) {
				String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
				usrGrp2Cost.put(ug,   usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
			}
		} else {
			LOG.warn("The person filter is null, still, trying to get emission costs per user group. Returning emission costs for all persons.");
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(this.vehicleId2WarmEmissCosts));
		}
		return usrGrp2Cost;
	}

	public Map<String, Double> getUserGroup2ColdEmissionCosts(){
		Map<String, Double> usrGrp2Cost = new HashMap<>();
		if(this.pf!=null) {
			for (Map.Entry<Id<Person>, Double> entry : getPersonId2ColdEmissionCosts().entrySet()) {
				String ug = this.pf.getUserGroupAsStringFromPersonId(entry.getKey());
				usrGrp2Cost.put(ug, usrGrp2Cost.containsKey(ug) ? entry.getValue() + usrGrp2Cost.get(ug) : entry.getValue());
			}
		} else {
			LOG.warn("The person filter is null, still, trying to get emission costs per user group. Returning emission costs for all persons.");
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(this.vehicleId2ColdEmissCosts));
		}
		return usrGrp2Cost;
	}

	@Override
	public Map<String, Double> getUserGroup2TotalEmissionCosts(){
		return getUserGroup2ColdEmissionCosts().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(),
						entry -> entry.getValue() + getUserGroup2WarmEmissionCosts().get(entry.getKey()))
		);
	}

	@Override
	public boolean isFiltering() {
		return ! (pf==null);
	}

	public Map<Id<Vehicle>, Double> getVehicleId2ColdEmissionCosts() {
		return this.vehicleId2ColdEmissCosts;
	}

	public Map<Id<Vehicle>, Double> getVehicleId2WarmEmissionCosts() {
		return this.vehicleId2WarmEmissCosts;
	}

}
