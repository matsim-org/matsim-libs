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

package playground.agarwalamit.analysis.legMode.distributions;

import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;

/**
 * @author amit
 */

public class FilteredDepartureTimeHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, 
ActivityStartEventHandler, PersonStuckEventHandler {

	private final DepartureTimeHandler delegate ;
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final String userGroup ;
	private static final Logger LOG = Logger.getLogger(FilteredDepartureTimeHandler.class);

	/**
	 * @param userGroup
	 * Data will include persons from the given user group.
	 */
	public FilteredDepartureTimeHandler (final double timeBinSize, final String userGroup){
		this.userGroup = userGroup;
		this.delegate = new DepartureTimeHandler(timeBinSize);
		LOG.warn("User group will be identified for Munich scenario only, i.e. Urban, (Rev)Commuter and Freight.");
	}

	/**
	 * No filtering is used. 
	 */
	public FilteredDepartureTimeHandler (final double timeBinSize){
		this(timeBinSize, null);
	}

	@Override
	public void reset(int iteration) {
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String ug = pf.getMyUserGroupFromPersonId(event.getPersonId());
		if( ug != null && ug.equals(userGroup)) {
			delegate.handleEvent(event);
		} else {
			delegate.handleEvent(event);
		}
	}
	public Map<String, SortedMap<Double, Integer>> getMode2TimeBin2Count() {
		return this.delegate.getMode2TimeBin2Count();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.delegate.handleEvent(event);
	}
	public void handleRemainingTransitUsers(){
		this.delegate.handleRemainingTransitUsers();
	}
}