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
package playground.agarwalamit.analysis.emission;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;

import playground.agarwalamit.munich.analysis.userGroup.EmissionsPerPersonPerUserGroup;
import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;

/**
 * @author amit
 */

public class FilteredColdEmissionHandler implements ColdEmissionEventHandler{
	private static final Logger LOGGER = Logger.getLogger(FilteredColdEmissionHandler.class.getName());

	private final EmissionsPerLinkColdEventHandler delegate;
	private final PersonFilter pf ;
	private final Network network;
	private final String ug ;
	private final AreaFilter af;

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String userGroup, final PersonFilter personFilter, 
			final Network network, final AreaFilter areaFilter){
		this.delegate = new EmissionsPerLinkColdEventHandler(simulationEndTime,noOfTimeBins);

		this.af = areaFilter;
		this.network = network;
		this.ug=userGroup;
		this.pf = personFilter;
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. Another class 
	 * {@link EmissionsPerPersonPerUserGroup} could give more detailed results based on person id for all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String userGroup, final PersonFilter personFilter){
		this(simulationEndTime,noOfTimeBins,userGroup,personFilter, null, null);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
		LOGGER.warn( "This could be achieved from the other class \"EmissionsPerPersonPerUserGroup\", alternatively verify your results with the other class.");
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final Network network, final AreaFilter areaFilter){
		this(simulationEndTime,noOfTimeBins,null,null,network,areaFilter);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredColdEmissionHandler (final double simulationEndTime, final int noOfTimeBins){
		this(simulationEndTime,noOfTimeBins,null,null,null,null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		
		Id<Person> driverId = Id.createPersonId(event.getVehicleId()); // AA_TODO: either it should be mapped to vehicle id or read events file too to get driver id

		if (this.af!=null) { // area filtering
			Link link = network.getLinks().get(event.getLinkId());
			if(! this.af.isLinkInsideShape(link)) return;

			if(this.ug==null || this.pf==null) {// only area filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // both filtering
				delegate.handleEvent(event);
			}

		} else {

			if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug))
			
			if(this.ug==null || this.pf==null) {// no filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.ug)) { // user group filtering
				delegate.handleEvent(event);
			}
		}
	}

	public Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		return delegate.getColdEmissionsPerLinkAndTimeInterval();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}
}