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
package playground.kai.usecases.spatialConsistencyHandler;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author nagel
 *
 */
public class SpatialConsistencyEventHandler implements 
ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler 
{
	private static final class ActivityEndEventInfo {
		final double time ;
		final Coord coord ;
		public ActivityEndEventInfo(double time, Coord coord) {
			this.time = time ;
			this.coord = coord ;
		}

	}

	@Inject private ActivityFacilities facilities ;
	@Inject private Network network ;
	@Inject private Provider<TripRouter> tripRouterProvider;
	private final StageActivityTypes stageTypes;
	
	@Inject
	public SpatialConsistencyEventHandler() {
		stageTypes = tripRouterProvider.get().getStageActivityTypes() ;
	}

	
	private final Map<Id<Person>, ActivityEndEventInfo > tracker = new HashMap<>() ;
	
	

	@Override
	public void reset(int iteration) {
		tracker.clear() ;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Coord coord ;
		ActivityFacility fac = facilities.getFacilities().get( event.getFacilityId() ) ;
		if ( fac != null ) {
			coord = fac.getCoord() ;
		} else {
			Link link = network.getLinks().get( event.getLinkId() ) ;
			coord = link.getCoord() ; // yy debatable
		}
		tracker.put( event.getPersonId(), new ActivityEndEventInfo( event.getTime(), coord ) ) ;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Coord coord ;
		ActivityFacility fac = facilities.getFacilities().get( event.getFacilityId() ) ;
		if ( fac != null ) {
			coord = fac.getCoord() ;
		} else {
			Link link = network.getLinks().get( event.getLinkId() ) ;
			coord = link.getCoord() ; // yy debatable
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	
}
