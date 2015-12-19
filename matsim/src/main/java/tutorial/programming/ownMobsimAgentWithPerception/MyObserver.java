/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.ownMobsimAgentWithPerception;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * Observer that listens to events, builds a congestion level based on that, and returns a "best outgoing link".
 * 
 * @author nagel
 */
class MyObserver implements BasicEventHandler {
	Map<Id<Link>,Double> nVehs = new HashMap<>() ;
	private Scenario scenario;
	
	MyObserver( Scenario sc ) {
		this.scenario = sc ;
	}
	
	@Override
	public void handleEvent(Event event) {
		Id<Link> linkId = null ;

		if ( event instanceof LinkEnterEvent ) {
			linkId = ((LinkEnterEvent) event).getLinkId() ;
		} else if ( event instanceof VehicleEntersTrafficEvent ) {
			linkId = ((VehicleEntersTrafficEvent) event).getLinkId() ;
		}
		if ( nVehs.get( linkId ) != null ) {
			double val = nVehs.get( linkId ) ;
			nVehs.put( linkId, val++ ) ;
		} else {
			nVehs.put( linkId, 1. ) ;
		}
		
		String arrivalMode = TransportMode.car ;
		if ( event instanceof LinkLeaveEvent ) {
			linkId = ((LinkLeaveEvent) event).getLinkId() ;
		} else if ( event instanceof PersonArrivalEvent ) {		// yyyyyy there is _still_ no clean vehicle arrival event ?!?!?!
			linkId = ((PersonArrivalEvent)event).getLinkId() ;
			arrivalMode = ((PersonArrivalEvent)event).getLegMode() ;
		}
		if ( arrivalMode.equals( TransportMode.car ) ) {
			if ( nVehs.get( linkId ) != null ) {
				double val = nVehs.get( linkId ) ;
				nVehs.put( linkId,  val-- ) ;
			} else {
				throw new RuntimeException("should not happen; a car should always have to enter a link before leaving it") ;
			}
		}
 	}
	
	double congestionLevel(Id<Link> linkId) {
		final Double nn = nVehs.get( linkId );
		if ( nn == null ) {
			return 0. ;
		}
		final Link link = this.scenario.getNetwork().getLinks().get(linkId);
		double nLanes = link.getNumberOfLanes() ;
		double length = link.getLength() ;
		double estimStorCap = nLanes * length / 7.5 ; // estimated storage capacity
		return nn / estimStorCap ;
	}

	@Override
	public void reset(int iteration) {
	}


}
