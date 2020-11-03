/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.simpleParkingCostHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

final class ParkingCostHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler {
	
	private final Map<Id<Person>, Double> personId2lastLeaveVehicleTime = new HashMap<>();
	private final Map<Id<Person>, String> personId2previousActivity = new HashMap<>();
	private final Map<Id<Person>, Id<Link>> personId2relevantModeLinkId = new HashMap<>();
	private final Set<Id<Person>> ptDrivers = new HashSet<>();
	
	@Inject
	private ParkingCostConfigGroup parkingCostConfigGroup;
	
	@Inject
	private EventsManager events;
	
	@Inject
	private Scenario scenario;
	
	@Override
    public void reset(int iteration) {
       this.personId2lastLeaveVehicleTime.clear();
       this.personId2previousActivity.clear();
       this.personId2relevantModeLinkId.clear();
       this.ptDrivers.clear();
    }
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptDrivers.add(event.getDriverId());
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {		
		if (ptDrivers.contains(event.getPersonId())) {
			// skip pt drivers
		} else {
			if (!(StageActivityTypeIdentifier.isStageActivity(event.getActType()))) {
				
				personId2previousActivity.put(event.getPersonId(), event.getActType());
				
				if (personId2relevantModeLinkId.get(event.getPersonId()) != null) {
					personId2relevantModeLinkId.remove(event.getPersonId());
				}
			}
		}	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			// skip pt drivers
		} else {
			// There might be several departures during a single trip.
			if (event.getLegMode().equals(parkingCostConfigGroup.getMode())) {
				personId2relevantModeLinkId.put(event.getPersonId(), event.getLinkId());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			// skip pt drivers
		} else {
			if (personId2relevantModeLinkId.get(event.getPersonId()) != null
					&& !personId2previousActivity.get(event.getPersonId()).startsWith(parkingCostConfigGroup.getHomeActivityPrefixToBeExcludedFromParkingCost())) {
				
				// compute parking costs
				double parkingStartTime = 0.;
				if (personId2lastLeaveVehicleTime.get(event.getPersonId()) != null) {
					parkingStartTime = personId2lastLeaveVehicleTime.get(event.getPersonId());
				}
				int parkingDurationHrs = (int) Math.ceil((event.getTime() - parkingStartTime) / 3600.);
				
				Link link = scenario.getNetwork().getLinks().get(personId2relevantModeLinkId.get(event.getPersonId()));
				
				double dailyParkingCosts = 0.;
				if (link.getAttributes().getAttribute(parkingCostConfigGroup.getDailyParkingCostLinkAttributeName()) != null) {
					dailyParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getDailyParkingCostLinkAttributeName());
				}
				
				double extraHourParkingCosts = 0.;
				if (link.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()) != null) {
					extraHourParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName());
				}
				
				double firstHourParkingCosts = 0.;
				if (link.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()) != null) {
					firstHourParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName());
				}
				
				double maxDailyParkingCosts = 0.;
				if (link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxDailyParkingCostLinkAttributeName()) != null) {
					maxDailyParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxDailyParkingCostLinkAttributeName());
				}
				
				double costs = 0.;
				if (parkingDurationHrs > 0) {
					costs += firstHourParkingCosts;
					costs += (parkingDurationHrs - 1) * extraHourParkingCosts;
				}
				if (costs > dailyParkingCosts) {
					costs = dailyParkingCosts;
				}
				if (costs > maxDailyParkingCosts) {
					costs = maxDailyParkingCosts;
				}
				
				if (costs > 0.) {
					double amount = -1. * costs;
					events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount, "parking", "city-of-LA"));
				}			
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			// skip pt drivers
		} else {
			personId2lastLeaveVehicleTime.put(event.getPersonId(), event.getTime());
		}
	}

	
}

