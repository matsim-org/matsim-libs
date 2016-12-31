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

package playground.agarwalamit.analysis.tripTime;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class FilteredModalTripTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler, 
TransitDriverStartsEventHandler, ActivityStartEventHandler {
	
	public FilteredModalTripTravelTimeHandler(){
		this(null,null);
	}
	
	public FilteredModalTripTravelTimeHandler (final String userGroup, final PersonFilter pf){
		this.userGroup = userGroup;
		this.pf = pf;
		if ( (this.userGroup == null && this.pf != null) || (this.userGroup != null && this.pf == null) ) {
			throw new RuntimeException("Either of user group or person filter is null. Aborting...");
		}
	}
	
	private final ModalTripTravelTimeHandler delegate = new ModalTripTravelTimeHandler();
	final PersonFilter pf;
	final String userGroup;

	public void reset(int iteration) {
		delegate.reset(iteration);
	}

	public void handleEvent(PersonArrivalEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	public void handleEvent(PersonDepartureEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	public SortedMap<String, Map<Id<Person>, List<Double>>> getLegMode2PesonId2TripTimes() {
		return delegate.getLegMode2PesonId2TripTimes();
	}

	public void handleEvent(PersonStuckEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	public void handleEvent(TransitDriverStartsEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getDriverId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	public void handleEvent(ActivityStartEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}
}