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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;
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

public class ExperiencedEmissionCostHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, WarmEmissionEventHandler, ColdEmissionEventHandler, EmissionCostHandler{

	private static final Logger LOG = Logger.getLogger(ExperiencedEmissionCostHandler.class);

	private final Map<Double, Map<Id<Vehicle>, Double>> vehicleId2ColdEmissCosts = new HashMap<>();
	private final Map<Double, Map<Id<Vehicle>, Double>> vehicleId2WarmEmissCosts = new HashMap<>();

	private final Map<Double, Map<Id<Person>, Double>> personId2ColdEmissCosts = new HashMap<>();
	private final Map<Double, Map<Id<Person>, Double>> personId2WarmEmissCosts = new HashMap<>();

	private final Map<Id<Vehicle>,Id<Person>> vehicle2Person = new HashMap<>();

	private boolean catchedAtLeastOneEmissionEvents = false;

	@Inject private QSimConfigGroup qSimConfigGroup;

	@Inject
	private EmissionResponsibilityCostModule emissionCostModule;
	@Inject(optional=true) private PersonFilter pf ;
	@Inject(optional=true) private double simulationEndTime;
	@Inject(optional=true) private double noOfTimeBins ;

	private double timeBinSize;

	public ExperiencedEmissionCostHandler(){}

	public ExperiencedEmissionCostHandler(final EmissionResponsibilityCostModule emissionCostModule, final PersonFilter pf) {
		double simulationEndTime = qSimConfigGroup.getEndTime();
		this.emissionCostModule = emissionCostModule;
		this.pf = pf;
		this.simulationEndTime = simulationEndTime;
		this.noOfTimeBins = 1;
		this.timeBinSize = simulationEndTime/ noOfTimeBins;
	}

	public ExperiencedEmissionCostHandler(final EmissionResponsibilityCostModule emissionCostModule, final PersonFilter pf, final double simulationEndTime, final double noOfTimeBin) {
		this.emissionCostModule = emissionCostModule;
		this.pf = pf;
		this.simulationEndTime = simulationEndTime;
		this.noOfTimeBins = noOfTimeBin;
		this.timeBinSize = simulationEndTime/ noOfTimeBins;
	}

	@Override
	public void reset(int iteration) {
		this.vehicleId2ColdEmissCosts.clear();
		this.vehicleId2WarmEmissCosts.clear();
		this.personId2ColdEmissCosts.clear();
		this.personId2WarmEmissCosts.clear();
		this.vehicle2Person.clear();
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		catchedAtLeastOneEmissionEvents= true;
		Id<Vehicle> vehicleId = event.getVehicleId();
		double warmEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(event.getWarmEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  warmEmissionCosts;

		Id<Person> personId = this.vehicle2Person.get(vehicleId);
		if(personId==null) throw new RuntimeException("no person is found for vehicle "+vehicleId+". This occus at "+ event.toString());

		double endOfTimeInterval = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;

		Map<Id<Vehicle>,Double> vehi2emiss = this.vehicleId2WarmEmissCosts.get(endOfTimeInterval);
		Map<Id<Person>, Double> person2emiss = this.personId2WarmEmissCosts.get(endOfTimeInterval);


		if(vehi2emiss==null) {
			vehi2emiss = new HashMap<>();
			vehi2emiss.put(vehicleId,amount2Pay);

			person2emiss = new HashMap<>();
			person2emiss.put(personId,amount2Pay);
		} else {
			if (vehi2emiss.containsKey(vehicleId)) {
				double nowCost = vehi2emiss.get(vehicleId);
				vehi2emiss.put(vehicleId,nowCost+amount2Pay);
				person2emiss.put(personId, nowCost+ amount2Pay);
			} else {
				vehi2emiss.put(vehicleId,amount2Pay);
				person2emiss.put(personId, amount2Pay);
			}
		}
		this.vehicleId2WarmEmissCosts.put(endOfTimeInterval, vehi2emiss);
		this.personId2WarmEmissCosts.put(endOfTimeInterval, person2emiss);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		catchedAtLeastOneEmissionEvents= true;
		Id<Vehicle> vehicleId = event.getVehicleId();
		double coldEmissionCosts = this.emissionCostModule.calculateColdEmissionCosts(event.getColdEmissions(), event.getLinkId(), event.getTime());
		double amount2Pay =  coldEmissionCosts;

		Id<Person> personId = this.vehicle2Person.get(vehicleId);
		if(personId==null) throw new RuntimeException("no person is found for vehicle "+vehicleId+". This occus at "+ event.toString());

		double endOfTimeInterval = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;

		Map<Id<Vehicle>,Double> vehi2emiss = this.vehicleId2ColdEmissCosts.get(endOfTimeInterval);
		Map<Id<Person>, Double> person2emiss = this.personId2ColdEmissCosts.get(endOfTimeInterval);


		if(vehi2emiss==null) {
			vehi2emiss = new HashMap<>();
			vehi2emiss.put(vehicleId,amount2Pay);

			person2emiss = new HashMap<>();
			person2emiss.put(personId,amount2Pay);
		} else {
			if (vehi2emiss.containsKey(vehicleId)) {
				double nowCost = vehi2emiss.get(vehicleId);
				vehi2emiss.put(vehicleId,nowCost+amount2Pay);
				person2emiss.put(personId, nowCost+ amount2Pay);
			} else {
				vehi2emiss.put(vehicleId,amount2Pay);
				person2emiss.put(personId, amount2Pay);
			}
		}
		this.vehicleId2ColdEmissCosts.put(endOfTimeInterval,vehi2emiss);
		this.personId2ColdEmissCosts.put(endOfTimeInterval,person2emiss);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.vehicle2Person.put(event.getVehicleId(),event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// it is possible that an emission event is thrown after "vehicle leaves traffic" event.
//		this.vehicle2Person.remove(event.getVehicleId());
	}

	@Override
	public Map<Double, Map<Id<Person>, Double>> getTimeBin2PersonId2TotalEmissionCosts() {
		Set<Double> timeBins = new HashSet<>(getTimeBin2PersonId2WarmEmissionCosts().keySet());
		timeBins.addAll(getTimeBin2PersonId2ColdEmissionCosts().keySet());

		Map<Double, Map<Id<Person>, Double>> outMap = new HashMap<>();

		for(Double d : timeBins) {
			Map<Id<Person>,Double> person2cost = new HashMap<>();

			Set<Id<Person>> personsSet = new HashSet<>();
			if (getTimeBin2PersonId2WarmEmissionCosts().get(d)!=null) personsSet.addAll(getTimeBin2PersonId2WarmEmissionCosts().get(d).keySet());
			if (getTimeBin2PersonId2ColdEmissionCosts().get(d)!=null) personsSet.addAll(getTimeBin2PersonId2ColdEmissionCosts().get(d).keySet());

			for(Id<Person> personId : personsSet) {
				double warmCost = 0.;
				double coldCost = 0.;

				if (getTimeBin2PersonId2ColdEmissionCosts().get(d)!=null && getTimeBin2PersonId2ColdEmissionCosts().get(d).get(personId)!=null) {
					warmCost = getTimeBin2PersonId2ColdEmissionCosts().get(d).get(personId);
				}

				if (getTimeBin2PersonId2WarmEmissionCosts().get(d)!=null && getTimeBin2PersonId2WarmEmissionCosts().get(d).get(personId)!=null) {
					coldCost = getTimeBin2PersonId2WarmEmissionCosts().get(d).get(personId);
				}

				person2cost.put(personId, coldCost+warmCost);
			}
			outMap.put(d,person2cost);
		}
		return outMap;
	}

	public Map<Id<Person>, Double> getPersonId2WarmEmissionCosts() {
		Map<Id<Person>, Double> outMap = new HashMap<>(); // TODO : yet to update the following
		for(Map<Id<Person>, Double> value : getTimeBin2PersonId2WarmEmissionCosts().values()){
			for(Map.Entry<Id<Person>, Double> e : value.entrySet()) {
				if (outMap.containsKey( e.getKey() ) ) {
					outMap.put( e.getKey(), outMap.get(e.getKey()) + e.getValue());
				} else {
					outMap.put(e.getKey(), e.getValue());
				}
			}
		}
		return outMap;
	}

	public Map<Id<Person>, Double> getPersonId2ColdEmissionCosts() {
		Map<Id<Person>, Double> outMap = new HashMap<>(); // TODO : yet to update the following
		for(Map<Id<Person>, Double> value : getTimeBin2PersonId2ColdEmissionCosts().values()){
			for(Map.Entry<Id<Person>, Double> e : value.entrySet()) {
				if (outMap.containsKey( e.getKey() ) ) {
					outMap.put( e.getKey(), outMap.get(e.getKey()) + e.getValue());
				} else {
					outMap.put(e.getKey(), e.getValue());
				}
			}
		}
		return outMap;
	}

	@Override
	public Map<Id<Person>, Double> getPersonId2TotalEmissionCosts() {
		Map<Id<Person>,Double> outMap = new HashMap<>();
		for(Id<Person> personId : this.getPersonId2WarmEmissionCosts().keySet()) {
			double warmCost = 0.;
			if (this.getPersonId2ColdEmissionCosts().get(personId)!=null) warmCost = this.getPersonId2ColdEmissionCosts().get(personId);
			outMap.put(personId,warmCost+this.getPersonId2ColdEmissionCosts().get(personId));
		}
		return outMap;
	}

	@Override
	public Map<Double, Map<Id<Vehicle>, Double>> getTimeBin2VehicleId2TotalEmissionCosts() {
		Set<Double> timeBins = new HashSet<>(getTimeBin2VehicleId2WarmEmissionCosts().keySet());
		timeBins.addAll(getTimeBin2VehicleId2ColdEmissionCosts().keySet());

		Map<Double, Map<Id<Vehicle>, Double>> outMap = new HashMap<>();

		for(Double d : timeBins) {
			Map<Id<Vehicle>,Double> vehicle2cost = new HashMap<>();

			Set<Id<Vehicle>> vehiclesSet = new HashSet<>();
			if (getTimeBin2VehicleId2WarmEmissionCosts().get(d)!=null) vehiclesSet.addAll(getTimeBin2VehicleId2WarmEmissionCosts().get(d).keySet());
			if (getTimeBin2VehicleId2ColdEmissionCosts().get(d)!=null) vehiclesSet.addAll(getTimeBin2VehicleId2ColdEmissionCosts().get(d).keySet());

			for(Id<Vehicle> vehicleId : vehiclesSet) {
				double warmCost = 0.;
				double coldCost = 0.;

				if (getTimeBin2VehicleId2ColdEmissionCosts().get(d)!=null && getTimeBin2VehicleId2ColdEmissionCosts().get(d).get(vehicleId)!=null) {
					warmCost = getTimeBin2PersonId2ColdEmissionCosts().get(d).get(vehicleId);
				}

				if (getTimeBin2VehicleId2WarmEmissionCosts().get(d)!=null && getTimeBin2VehicleId2WarmEmissionCosts().get(d).get(vehicleId)!=null) {
					coldCost = getTimeBin2VehicleId2WarmEmissionCosts().get(d).get(vehicleId);
				}

				vehicle2cost.put(vehicleId, coldCost+warmCost);
			}
			outMap.put(d,vehicle2cost);
		}
		return outMap;
	}

	@Override
	public Map<Id<Vehicle>, Double> getVehicleId2TotalEmissionCosts(){
		// TODO : yet to update the following
		Map<Id<Vehicle>,Double> veh2emiss = new HashMap<>();
		for(Double time : getTimeBin2VehicleId2TotalEmissionCosts().keySet()){
			for (Id<Vehicle> vehicleId : getTimeBin2VehicleId2TotalEmissionCosts().get(time).keySet()) {
				if (veh2emiss.containsKey(vehicleId)) {
					veh2emiss.put(vehicleId,veh2emiss.get(vehicleId) + getTimeBin2VehicleId2TotalEmissionCosts().get(time).get(vehicleId));
				} else {
					veh2emiss.put(vehicleId, getTimeBin2VehicleId2TotalEmissionCosts().get(time).get(vehicleId));
				}
			}
		}
		return veh2emiss;
	}

	public Map<Double, Double> getTimeBin2TotalCosts() {
		return getTimeBin2PersonId2TotalEmissionCosts().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> MapUtils.doubleValueSum(entry.getValue())));
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
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(getPersonId2WarmEmissionCosts()));
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
			usrGrp2Cost.put("AllPersons", MapUtils.doubleValueSum(getPersonId2ColdEmissionCosts()));
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

	// a check which should go away in future (End of 2017 or so) amit may'17
	private void checkForCatchingEmissionEvents(final boolean catchedAtLeastOneEmissionEvents){
		if (! catchedAtLeastOneEmissionEvents) {
			throw new RuntimeException("Read events file does not have any emission events, please check. " +
					"This may be due to the recent merging of emission events to the normal events channel.");
		}
	}

	@Override
	public boolean isFiltering() {
		return ! (pf==null);
	}

	public Map<Double, Map<Id<Vehicle>, Double>> getTimeBin2VehicleId2ColdEmissionCosts() {
		checkForCatchingEmissionEvents(catchedAtLeastOneEmissionEvents);
		return this.vehicleId2ColdEmissCosts;
	}

	public Map<Double, Map<Id<Vehicle>, Double>> getTimeBin2VehicleId2WarmEmissionCosts() {
		checkForCatchingEmissionEvents(catchedAtLeastOneEmissionEvents);
		return this.vehicleId2WarmEmissCosts;
	}

	public Map<Double, Map<Id<Person>, Double>> getTimeBin2PersonId2ColdEmissionCosts() {
		checkForCatchingEmissionEvents(catchedAtLeastOneEmissionEvents);
		return this.personId2ColdEmissCosts;
	}

	public Map<Double, Map<Id<Person>, Double>> getTimeBin2PersonId2WarmEmissionCosts() {
		checkForCatchingEmissionEvents(catchedAtLeastOneEmissionEvents);
		return this.personId2WarmEmissCosts;
	}

}
