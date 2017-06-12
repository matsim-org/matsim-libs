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
package playground.agarwalamit.analysis.emission.caused;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.*;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.EmissionCostHandler;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;

/**
 * Emission costs (flat emission cost module is used).
 *
 * @author amit
 */

public class CausedEmissionCostHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, WarmEmissionEventHandler, ColdEmissionEventHandler, EmissionCostHandler{

	private static final Logger LOG = Logger.getLogger(CausedEmissionCostHandler.class);

	private final EmissionCostModule emissionCostModule;
	private final Map<Id<Vehicle>, Double> vehicleId2ColdEmissCosts = new HashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2WarmEmissCosts = new HashMap<>();

	private final PersonFilter pf ;

	private final Map<Id<Vehicle>,Id<Person>> vehicle2Person = new HashMap<>(); // technically, a person may have more than one vehicle.

	public CausedEmissionCostHandler(final EmissionCostModule emissionCostModule) {
		this(emissionCostModule, null);
	}

	public CausedEmissionCostHandler(final EmissionCostModule emissionCostModule, final PersonFilter pf) {
		this.emissionCostModule = emissionCostModule;
		this.pf = pf;
	}

	public static void main(String[] args) {
		String emissionEventsFile = FileUtils.RUNS_SVN+ "/detEval/emissionCongestionInternalization/iatbr/output/ei/ITERS/it.1500/1500.emission.events.xml.gz";
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EmissionsConfigGroup emissionsConfigGroup = new EmissionsConfigGroup();
		emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);
		emissionsConfigGroup.setConsideringCO2Costs(true);

		EmissionCostModule ecm = new EmissionCostModule(emissionsConfigGroup);
		CausedEmissionCostHandler ech = new CausedEmissionCostHandler(ecm);
		eventsManager.addHandler(ech);
		new EmissionEventsReader(eventsManager).readFile(emissionEventsFile);

		Map<String, Double> usrGrp2Cost = ech.getUserGroup2TotalEmissionCosts();
		usrGrp2Cost.entrySet().forEach(entry -> System.out.print(entry.getKey()+"\t"+entry.getValue()));
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ColdEmissCosts.clear();
		this.vehicleId2WarmEmissCosts.clear();
		this.vehicle2Person.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();

		if(this.vehicleId2WarmEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2WarmEmissCosts.get(vehicleId);
			this.vehicleId2WarmEmissCosts.put(vehicleId, nowCost+ this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions()));
		} else {
			this.vehicleId2WarmEmissCosts.put(vehicleId,
					this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions()));
		}
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();

		if(this.vehicleId2ColdEmissCosts.containsKey(vehicleId)){
			double nowCost = this.vehicleId2ColdEmissCosts.get(vehicleId);
			this.vehicleId2ColdEmissCosts.put(vehicleId, nowCost+ this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions()));
		} else {
			this.vehicleId2ColdEmissCosts.put(vehicleId,
					this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions()));
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.vehicle2Person.put(event.getVehicleId(),event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.vehicle2Person.remove(event.getVehicleId());
	}

	public Map<Id<Person>, Double> getPersonId2ColdEmissionCosts() {
		return this.vehicleId2ColdEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> this.vehicle2Person.get(entry.getKey()), entry -> entry.getValue())
		);
	}

	public Map<Id<Person>, Double> getPersonId2WarmEmissionCosts() {
		return this.vehicleId2WarmEmissCosts.entrySet().stream().collect(
				Collectors.toMap(entry -> this.vehicle2Person.get(entry.getKey()), entry -> entry.getValue())
		);
	}

	@Override
	public Map<Id<Person>, Double> getPersonId2TotalEmissionCosts() {
		return getPersonId2ColdEmissionCosts().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue() + this.getPersonId2WarmEmissionCosts().get(entry.getKey()))
		);
	}

	@Override
	public Map<Double, Map<Id<Vehicle>, Double>> getTimeBin2VehicleId2TotalEmissionCosts() {
		throw new UnsupportedOperationException("This information is not updated yet.");
	}

	@Override
	public Map<Double, Map<Id<Person>, Double>> getTimeBin2PersonId2TotalEmissionCosts() {
		throw new UnsupportedOperationException("This information is not updated yet.");
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
