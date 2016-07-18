/* *********************************************************************** *
 * project: org.matsim.*
 * CarDistanceEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class CarDistanceEventHandler implements LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler{
	private final static Logger logger = Logger.getLogger(CarDistanceEventHandler.class);

	private Map<Id, Double> personId2CarDistance;
	private Map<UserGroup, Double> userGroup2carTrips;
	private final Network network;
	private final PersonFilter personFilter;
	
	// the following is not neccessary any more...see below
	private Map<Id, Id> personId2departureLinkId;
	private Map<Id, Double> depArrOnSameLinkCnt;
	
	public CarDistanceEventHandler(Network network) {
		this.personId2CarDistance = new HashMap<Id, Double>();
		this.userGroup2carTrips = new HashMap<UserGroup, Double>();
		this.network = network;
		this.personFilter = new PersonFilter();
		
		// the following is not neccessary any more...see below
		this.personId2departureLinkId = new HashMap<Id, Id>();
		this.depArrOnSameLinkCnt = new HashMap<Id, Double>();
	}

	@Override
	public void reset(int iteration) {
		this.personId2CarDistance = new HashMap<Id, Double>();
		this.userGroup2carTrips = new HashMap<UserGroup, Double>();;
		logger.info("resetting personId2CarDistance to " + this.personId2CarDistance + " ...");
		logger.info("resetting userGroup2carTrips to " + this.userGroup2carTrips + " ...");
		
		// the following is not neccessary any more...see below
		this.personId2departureLinkId = new HashMap<Id, Id>();
		this.depArrOnSameLinkCnt = new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getDriverId();
		Id linkId = event.getLinkId();
		Double linkLength_m = this.network.getLinks().get(linkId).getLength();
		
		if(this.personId2CarDistance.get(personId) == null){
			this.personId2CarDistance.put(personId, linkLength_m);
		} else {
			double distanceSoFar = this.personId2CarDistance.get(personId);
			double distanceAfterEvent = distanceSoFar + linkLength_m;
			this.personId2CarDistance.put(personId, distanceAfterEvent);
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {		
		// the following is not neccessary any more...see below
		personId2departureLinkId.put(event.getPersonId(), event.getLinkId());

		// calculating the number of trips...
		if(event.getLegMode().equals(TransportMode.car)){
			Id personId = event.getPersonId();
			for(UserGroup userGroup : UserGroup.values()){
				if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
					if(userGroup2carTrips.get(userGroup) == null){
						userGroup2carTrips.put(userGroup, 1.0);
					} else {
						double carTripsSoFar = userGroup2carTrips.get(userGroup);
						double carTripsAfter = carTripsSoFar + 1.0;
						userGroup2carTrips.put(userGroup, carTripsAfter);
					}
				}
			}
			
			// in order to get the number of car users right...see below
			if(this.personId2CarDistance.get(personId) == null){
				this.personId2CarDistance.put(personId, 0.0);
			} else {
				// do nothing
			}
		}
	}
	
	// the following is not neccessary any more...see above
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();

		if(personId2departureLinkId.get(personId) == null){
			logger.warn("Person " + personId + " is arriving on link " + linkId + " without having departed anywhere before...");
		} else {
			Id departureLinkId = personId2departureLinkId.get(personId);
			if(event.getLegMode().equals(TransportMode.car)){
				if(departureLinkId.equals(linkId)){
					if(depArrOnSameLinkCnt.get(personId) == null){
						depArrOnSameLinkCnt.put(personId, 1.0);
					} else {
						double cntSoFar = depArrOnSameLinkCnt.get(personId);
						double cntAfter = cntSoFar + 1.0;
						depArrOnSameLinkCnt.put(personId, cntAfter);
					}
				}
			}
		}
	}

	// the following is not neccessary any more...see above
	protected Map<Id, Double> getDepArrOnSameLinkCnt() {
		return depArrOnSameLinkCnt;
	}

	public Map<Id, Double> getPersonId2CarDistance() {
		return this.personId2CarDistance;
	}
	
	public Map<UserGroup, Double> getUserGroup2carTrips() {
		return this.userGroup2carTrips;
	}
}
