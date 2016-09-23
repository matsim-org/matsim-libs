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

package playground.agarwalamit.analysis.modalShare;

import java.util.SortedMap;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class FilteredModalShareEventHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler, PersonStuckEventHandler{

	private final ModalShareEventHandler delegate = new ModalShareEventHandler();
	private final String userGroup;
	private final PersonFilter pf;
	
	public FilteredModalShareEventHandler () {
		this(null,null);
	}
	
	public FilteredModalShareEventHandler (final String userGroup, final PersonFilter pf) {
		this.userGroup = userGroup;
		this.pf = pf;
		if ( (this.userGroup == null && this.pf != null) || (this.userGroup != null && this.pf == null) ) {
			throw new RuntimeException("Either of user group or person filter is null. Aborting...");
		}
	}
	
	@Override
	public void reset(int iteration) {
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getDriverId()))) {
				this.delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.userGroup == null || this.pf == null ) this.delegate.handleEvent(event);
		else {
			if(this.userGroup.equals(this.pf.getUserGroupAsStringFromPersonId(event.getPersonId()))) {
				this.delegate.handleEvent(event);
			}
		}	
	}
	
	public void handleRemainingTransitUsers(){
		this.delegate.handleRemainingTransitUsers();
	}

	public SortedMap<String, Integer> getMode2numberOflegs() {
		return this.delegate.getMode2numberOflegs();
	}
}