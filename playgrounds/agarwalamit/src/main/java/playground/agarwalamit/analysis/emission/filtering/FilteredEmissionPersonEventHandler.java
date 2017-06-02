/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission.filtering;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.EmissionPersonEventHandler;
import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class FilteredEmissionPersonEventHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger LOGGER = Logger.getLogger(FilteredEmissionPersonEventHandler.class.getName());

	private final Map<Id<Vehicle>, Id<Person>> vehicleId2PersonId2 = new HashMap<>();
	private final EmissionPersonEventHandler delegate;
	private final Network network;
	private final AreaFilter af;

	/**
	 * Area filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredEmissionPersonEventHandler(final Network network, final AreaFilter areaFilter){
		this.delegate = new EmissionPersonEventHandler();

		this.af = areaFilter;
		this.network = network;

		if (this.af !=null) {
			LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
		} else {
			LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
		}
	}

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredEmissionPersonEventHandler(){
		this(null,null);
	}


	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		this.vehicleId2PersonId2.clear();
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
 		Id<Person> driverId = this.vehicleId2PersonId2.get(event.getVehicleId());

		if (this.af!=null ) { // area filtering
			if (this.af.isLinkInsideShape(network.getLinks().get(event.getLinkId()))) {
				delegate.handleEvent(event);
			} else {
				//nothing to do
			}
		} else { // no filtering
			delegate.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Person> driverId = this.vehicleId2PersonId2.get(event.getVehicleId());

		if (this.af!=null ) { // area filtering
			if (this.af.isLinkInsideShape(network.getLinks().get(event.getLinkId()))) {
				delegate.handleEvent(event);
			} else {
				//nothing to do
			}
		} else { // no filtering
			delegate.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.vehicleId2PersonId2.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// Commeting following due to recent problem with berlin_open_scenario in which a few emission events are thrown
		// after vehicleLeavesTrafficEvent (in the same time step). If this causes some problem, probably use a later event (PersonArrivalEvent). Amit June'17
//		this.vehicleId2PersonId2.remove(event.getVehicleId(), event.getPersonId());
	}

	public Map<Id<Person>, Map<ColdPollutant, Double>> getPersonId2ColdEmissions() {
		return delegate.getPersonId2ColdEmissions();
	}

	public Map<Id<Person>, Map<ColdPollutant, Double>> getPersonId2ColdEmissions(String userGroup, PersonFilter personFilter) {
		return delegate.getPersonId2ColdEmissions().entrySet().parallelStream().filter(entry -> personFilter.getUserGroupAsStringFromPersonId(entry.getKey()).equals(userGroup)).collect(
				Collectors.toMap(e -> e.getKey(),e-> e.getValue()));
	}

	public Map<Id<Person>, Map<WarmPollutant, Double>> getPersonId2WarmEmissions() {
		return delegate.getPersonId2WarmEmissions();
	}

	public Map<Id<Person>, Map<WarmPollutant, Double>> getPersonId2WarmEmissions(String userGroup, PersonFilter personFilter) {
		return delegate.getPersonId2WarmEmissions().entrySet().parallelStream().filter(entry -> personFilter.getUserGroupAsStringFromPersonId(entry.getKey()).equals(userGroup)).collect(
				Collectors.toMap(e -> e.getKey(),e-> e.getValue()));
	}

	public Map<Id<Vehicle>, Map<ColdPollutant, Double>> getVehicleId2ColdEmissions() {
		return delegate.getVehicleId2ColdEmissions();
	}

	public Map<Id<Vehicle>, Map<WarmPollutant, Double>> getVehicleId2WarmEmissions() {
		return delegate.getVehicleId2WarmEmissions();
	}

	public Map<Id<Vehicle>, Map<String, Double>> getVehicleId2TotalEmissions(){
		return delegate.getVehicleId2TotalEmissions();
	}

	public Map<Id<Person>, Map<String, Double>> getPersonId2TotalEmissions(){
		return delegate.getPersonId2TotalEmissions();
	}

	public Map<Id<Person>, Map<String, Double>> getPersonId2TotalEmissions(String userGroup, PersonFilter personFilter){
		return delegate.getPersonId2TotalEmissions().entrySet().parallelStream().filter(entry -> personFilter.getUserGroupAsStringFromPersonId(entry.getKey()).equals(userGroup)).collect(
				Collectors.toMap(e -> e.getKey(),e-> e.getValue()));
	}

}