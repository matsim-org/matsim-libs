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

import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
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

	private final EmissionPersonEventHandler delegate;
	private final PersonFilter pf ;
	private final Network network;
	private final String ug ;
	private final AreaFilter af;

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredEmissionPersonEventHandler(final String userGroup, final PersonFilter personFilter,
											  final Network network, final AreaFilter areaFilter){
		this.delegate = new EmissionPersonEventHandler();

		this.af = areaFilter;
		this.network = network;
		this.ug=userGroup;
		this.pf = personFilter;

		if( (this.ug==null && this.pf!=null) || this.ug!=null && this.pf==null ) {
			throw new RuntimeException("Either of person filter or user group is null.");
		} else if( this.ug!=null && this.af !=null) {
			LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the "+this.ug+" user group will be considered.");
		} else if(this.ug!=null) {
			LOGGER.info("User group filtering is used, result will include all links but persons from "+this.ug+" user group only.");
		} else if (this.af !=null) {
			LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
		} else {
			LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
		}
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. Another class
	 * {@link playground.agarwalamit.munich.analysis.userGroup.EmissionsPerPersonPerUserGroup} could give more detailed results based on person id for all user groups.
	 */
	public FilteredEmissionPersonEventHandler( final String userGroup, final PersonFilter personFilter){
		this(userGroup,personFilter, null, null);
		LOGGER.warn( "This could be achieved from the other class \"EmissionsPerPersonPerUserGroup\", alternatively verify your results with the other class.");
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredEmissionPersonEventHandler( final Network network, final AreaFilter areaFilter){
		this(null,null,network,areaFilter);
	}

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredEmissionPersonEventHandler(){
		this(null,null,null,null);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
 		Id<Person> driverId = this.delegate.getDriverOfVehicle(event.getVehicleId());

		if (this.af!=null) { // area filtering
			Link link = network.getLinks().get(event.getLinkId());
			if(! this.af.isLinkInsideShape(link)) return;

			if(this.ug==null || this.pf==null) {// only area filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // both filtering
				delegate.handleEvent(event);
			}

		} else {
			if(this.ug==null || this.pf==null) {// no filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
				delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id<Person> driverId = this.delegate.getDriverOfVehicle(event.getVehicleId());

		if (this.af!=null) { // area filtering
			Link link = network.getLinks().get(event.getLinkId());
			if(! this.af.isLinkInsideShape(link)) return;

			if(this.ug==null || this.pf==null) {// only area filtering
				delegate.handleEvent(event);
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // both filtering
				delegate.handleEvent(event);
			}

		} else {
			if(this.ug==null || this.pf==null) {// no filtering
				delegate.handleEvent(event);
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
				delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
		return delegate.getDriverOfVehicle(vehicleId);
	}

	public Map<Id<Person>, Map<ColdPollutant, Double>> getPersonId2ColdEmissions() {
		return delegate.getPersonId2ColdEmissions();
	}

	public Map<Id<Person>, Map<WarmPollutant, Double>> getPersonId2WarmEmissions() {
		return delegate.getPersonId2WarmEmissions();
	}

	public Map<Id<Vehicle>, Map<ColdPollutant, Double>> getVehicleId2ColdEmissions() {
		return delegate.getVehicleId2ColdEmissions();
	}

	public Map<Id<Vehicle>, Map<WarmPollutant, Double>> getVehicleId2WarmEmissions() {
		return delegate.getVehicleId2WarmEmissions();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}
}