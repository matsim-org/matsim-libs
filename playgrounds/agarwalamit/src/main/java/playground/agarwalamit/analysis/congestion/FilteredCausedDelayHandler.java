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
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * @author amit
 */

public class FilteredCausedDelayHandler implements CongestionEventHandler {
	
	private static final Logger LOGGER = Logger.getLogger(FilteredCausedDelayHandler.class);
	private final CausedDelayHandler delegate ;
	private final PersonFilter pf ;
	private final AreaFilter af;
	private final String userGroup;
	private final Network network;
	
	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredCausedDelayHandler(final Scenario scenario, final int noOfTimeBins, final double simulationEndTime){
		this(scenario, noOfTimeBins, null, null, null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}
	
	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. 
	 */
	public FilteredCausedDelayHandler(final Scenario scenario, final int noOfTimeBins, final double simulationEndTime, final String userGroup, final PersonFilter personFilter){
		this(scenario, noOfTimeBins, userGroup, personFilter, null);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
	}
	
	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredCausedDelayHandler(final Scenario scenario, final int noOfTimeBins, final double simulationEndTime, final AreaFilter areaFilter){
		this(scenario, noOfTimeBins, null, null, areaFilter);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}
	
	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered. 
	 */
	public FilteredCausedDelayHandler(final Scenario scenario, final int noOfTimeBins, 
			 final String userGroup, final PersonFilter personFilter, final AreaFilter areaFilter){
		delegate = new CausedDelayHandler(scenario, noOfTimeBins);
		this.pf = personFilter;
		this.userGroup = userGroup;
		this.af = areaFilter;
		this.network = scenario.getNetwork();
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		Id<Person> driverId = event.getCausingAgentId();
		
		if (this.af!=null) { // area filtering
			Link link = network.getLinks().get(event.getLinkId());
			if(! this.af.isLinkInsideShape(link)) return;
			
			if(this.userGroup==null || this.pf==null) {// only area filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.userGroup)) { // both filtering
				delegate.handleEvent(event);
			}
			
		} else {
			
			if(this.userGroup==null || this.pf==null) {// no filtering
				delegate.handleEvent(event); 
			} else if (this.pf.getUserGroupAsStringFromPersonId(driverId).equals(this.userGroup)) { // user group filtering
				delegate.handleEvent(event);
			}
		}
	}
	
	public SortedMap<Double, Map<Id<Link>, Double>> getTimeBin2Link2Delay() {
		return delegate.getTimeBin2Link2Delay();
	}
	
	/**
	 * @return  set of UNIQUE causing persons (toll payers) on each link in each time bin
	 */
	public SortedMap<Double, Map<Id<Link>, Set<Id<Person>>>> getTimeBin2Link2CausingPersons() {
		return delegate.getTimeBin2Link2CausingPersons();
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2CausingPerson2Delay() {
		return delegate.getTimeBin2CausingPerson2Delay();
	}

	/**
	 * @return  set of UNIQUE causing persons (toll payers)  in each time bin
	 */
	public SortedMap<Double,Set<Id<Person>>> getTimeBin2CausingPersons() {
		return delegate.getTimeBin2CausingPersons();
	}
}