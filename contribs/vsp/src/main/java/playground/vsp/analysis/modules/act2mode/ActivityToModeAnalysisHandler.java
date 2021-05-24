/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.act2mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.PtConstants;

/**
 * @author droeder
 *
 */
public class ActivityToModeAnalysisHandler implements 
											ActivityEndEventHandler, PersonDepartureEventHandler,
											PersonArrivalEventHandler, ActivityStartEventHandler,
											TransitDriverStartsEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(ActivityToModeAnalysisHandler.class);
	private Network net;

	private List<ActivityToMode> departures;
	private List<ActivityToMode> arrivals;
	private List<Id> transitDriver;
	private Set<Id> personsOfInterest;
	private Map<Id, ActivityEndEvent> person2DepActAndTime;
	private Map<Id, String> person2ArrMode;
	
	public ActivityToModeAnalysisHandler(Network net, Set<Id> personsOfInterest) {
		this.net = net;
		this.transitDriver = new ArrayList<Id>();
		this.departures = new ArrayList<ActivityToMode>();
		this.arrivals = new ArrayList<ActivityToMode>();
		this.personsOfInterest = personsOfInterest;
		this.person2DepActAndTime = new HashMap<Id, ActivityEndEvent>();
		this.person2ArrMode = new HashMap<Id, String>();
	}
	
	private boolean processPerson(Id id){
		// process no transitDrivers!
		if(this.transitDriver.contains(id)) return false;
		// process only Person of Interest or all of no subset is defined!
		if(this.personsOfInterest == null) return true;
		if(this.personsOfInterest.contains(id)) return true;
		return false;
	}

	@Override
	public void reset(int iteration) {

	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		//check if we are interested in this person
		if(!processPerson(event.getPersonId())) return;
		// check if we'r interested in the activityType
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			this.person2DepActAndTime.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		//check if we are interested in this person
		if(!processPerson(event.getPersonId())) return;
		// chekc if this is not a transitWalk
		if(!event.getLegMode().equalsIgnoreCase(TransportMode.transit_walk)){
			ActivityEndEvent e = this.person2DepActAndTime.remove(event.getPersonId());
			// and if we stored an event for this person (should not happen, that there is no event..."
			if(! (e == null) ){
				Link link = this.net.getLinks().get(e.getLinkId());
				this.departures.add(
						new ActivityToMode(e.getActType(), event.getLegMode(), e.getTime(), link.getToNode().getCoord())
						);
			}
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//check if we are interested in this person
		if(!processPerson(event.getPersonId())) return;
		// and in this mode
		if(!event.getLegMode().equalsIgnoreCase(TransportMode.transit_walk)){
			this.person2ArrMode.put(event.getPersonId(), event.getLegMode());
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		//check if we are interested in this person
		if(!processPerson(event.getPersonId())) return;
		// and in this activityType
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			if(this.person2ArrMode.containsKey(event.getPersonId())){
				Link link = this.net.getLinks().get(event.getLinkId());
				this.arrivals.add(
						new ActivityToMode(event.getActType(), this.person2ArrMode.get(event.getPersonId()), event.getTime(),link.getToNode().getCoord())
						);
			}
		}
	}



	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDriver.add(event.getDriverId());
	}
	
	public List<ActivityToMode> getDepartures(){
		return this.departures;
	}
	
	public List<ActivityToMode> getArrivals(){
		return this.arrivals;
	}
	
	
}

