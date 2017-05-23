/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package playground.dgrether.events.filters;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * @author tthunig
 *
 */
public class FirstTripPerPersonEventFilter implements EventFilter {

	private static final Logger LOG = Logger.getLogger(FirstTripPerPersonEventFilter.class);
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private List<Id<Person>> firstTripFinishedPersons = new LinkedList<>();
	private double lastTime = 0;
	
	@Override
	public boolean doProcessEvent(Event event) {
		if (event instanceof HasPersonId){
			if (lastTime > event.getTime()){
				// reset list when iteration starts again (first interesting event is HasPersonId, that is why this test can be made here - saves time)
				firstTripFinishedPersons.clear();
				delegate.reset(0);
			}
			lastTime = event.getTime();
			
			if (event instanceof PersonArrivalEvent){
				if (firstTripFinishedPersons.contains(((HasPersonId) event).getPersonId())){
					return false;
				} else {
					firstTripFinishedPersons.add(((PersonArrivalEvent) event).getPersonId());
					return true;
				}
			} else if (event instanceof VehicleEntersTrafficEvent){
				delegate.handleEvent((VehicleEntersTrafficEvent)event);
			} else if (event instanceof VehicleLeavesTrafficEvent){
				delegate.handleEvent((VehicleLeavesTrafficEvent)event);
			}
			return checkFirstTripFinished(((HasPersonId)event).getPersonId());
		} else if (event instanceof LinkEnterEvent){
			return checkFirstTripFinished(delegate.getDriverOfVehicle(((LinkEnterEvent) event).getVehicleId()));
		} else if (event instanceof LinkLeaveEvent){
			return checkFirstTripFinished(delegate.getDriverOfVehicle(((LinkLeaveEvent) event).getVehicleId()));
		} else if (event instanceof VehicleAbortsEvent){
			return checkFirstTripFinished(delegate.getDriverOfVehicle(((VehicleAbortsEvent) event).getVehicleId()));
		} else {
			return false;
		}
	}

	private boolean checkFirstTripFinished(Id<Person> person) {
		if (firstTripFinishedPersons.contains(person)){
			return false;
		} else {
			return true;
		}
	}

}
