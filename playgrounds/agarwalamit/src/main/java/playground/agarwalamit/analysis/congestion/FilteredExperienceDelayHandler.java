/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.congestion;

import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class FilteredExperienceDelayHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	
	private static final Logger LOGGER = Logger.getLogger(FilteredExperienceDelayHandler.class);
	private final ExperiencedDelayHandler delegate ;
	private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();
	private final PersonFilter pf ;
	private final AreaFilter af;
	private final String userGroup;
	private final Network network;
	
	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredExperienceDelayHandler(final Scenario scenario, final int noOfTimeBins){
		this(scenario, noOfTimeBins, null, null, null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}
	
	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. 
	 */
	public FilteredExperienceDelayHandler(final Scenario scenario, final int noOfTimeBins, final String userGroup, final PersonFilter personFilter){
		this(scenario, noOfTimeBins, userGroup, personFilter, null);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
	}
	
	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredExperienceDelayHandler(final Scenario scenario, final int noOfTimeBins, final AreaFilter areaFilter){
		this(scenario, noOfTimeBins, null, null, areaFilter);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}
	
	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered. 
	 */
	public FilteredExperienceDelayHandler(final Scenario scenario, final int noOfTimeBins, 
			final String userGroup, final PersonFilter personFilter, final AreaFilter areaFilter){
		delegate = new ExperiencedDelayHandler(scenario, noOfTimeBins);
		this.pf = personFilter;
		this.userGroup = userGroup;
		this.af = areaFilter;
		this.network = scenario.getNetwork();
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}
	
	@Override
	public void reset(int iteration) {
		veh2DriverDelegate.reset(iteration);
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (isHandlingEvent(event.getPersonId(), event.getLinkId())) delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
		if (isHandlingEvent(driverId, event.getLinkId())) delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
		if (isHandlingEvent(driverId, event.getLinkId())) delegate.handleEvent(event);
	}
	
	private boolean isHandlingEvent(Id<Person> personId, Id<Link> linkId){
		boolean isHandlingEvent = false;

		if (this.af!=null) { // area filtering
			Link link = network.getLinks().get(linkId);
			if(! this.af.isLinkInsideShape(link)) return false;
			
			if(this.userGroup==null || this.pf==null) {// only area filtering
				return true; 
			} else if (this.pf.getUserGroupAsStringFromPersonId(personId).equals(this.userGroup)) { // both filtering
				return true;
			}
			
		} else {
			
			if(this.userGroup==null || this.pf==null) {// no filtering
				return true;
			} else if (this.pf.getUserGroupAsStringFromPersonId(personId).equals(this.userGroup)) { // user group filtering
				return true;
			}
		}
		return isHandlingEvent;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (isHandlingEvent(event.getPersonId(), event.getLinkId())) delegate.handleEvent(event);
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getDelayPerPersonAndTimeInterval() {
		return delegate.getDelayPerPersonAndTimeInterval();
	}

	public Map<Double, Map<Id<Link>, Double>> getDelayPerLinkAndTimeInterval() {
		return delegate.getDelayPerLinkAndTimeInterval();
	}

	public double getTotalDelayInHours() {
		return delegate.getTotalDelayInHours();
	}

	public Map<Double, Map<Id<Link>, Integer>> getTime2linkIdLeaveCount() {
		return delegate.getTime2linkIdLeaveCount();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		veh2DriverDelegate.handleEvent(event);
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		veh2DriverDelegate.handleEvent(event);
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		delegate.handleEvent(event);
	}
}
