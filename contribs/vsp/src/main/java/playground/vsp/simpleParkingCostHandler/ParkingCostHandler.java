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

import java.util.*;

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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
* @author ikaddoura
*/

final class ParkingCostHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonLeavesVehicleEventHandler, PersonEntersVehicleEventHandler {

	private final Map<Id<Person>, Double> personId2lastLeaveVehicleTime = new HashMap<>();
	private final Map<Id<Person>, String> personId2previousActivity = new HashMap<>();
	private final Map<Id<Person>, Id<Link>> personId2relevantModeLinkId = new HashMap<>();
	private final Map<Id<Person>, Id<Link>> personId2homeLinkId = new HashMap<>();
	private final Set<Id<Person>> ptDrivers = new HashSet<>();
	private final Set<Id<Person>> hasAlreadyPaidDailyResidentialParkingCosts = new HashSet<>();
	private double compensationTime = Double.NaN;

	@Inject
	private ParkingCostConfigGroup parkingCostConfigGroup;

	@Inject
	private EventsManager events;

	@Inject
	private Scenario scenario;

	@Inject
	private QSimConfigGroup qSimConfigGroup;


	@Override
    public void reset(int iteration) {
       this.personId2lastLeaveVehicleTime.clear();
       this.personId2previousActivity.clear();
       this.personId2relevantModeLinkId.clear();
       this.hasAlreadyPaidDailyResidentialParkingCosts.clear();
       this.ptDrivers.clear();
    }


	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptDrivers.add(event.getDriverId());
	}


	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			return;
		}
		if ((StageActivityTypeIdentifier.isStageActivity(event.getActType()))) {
			return;
		}

		personId2previousActivity.put(event.getPersonId(), event.getActType());
		personId2relevantModeLinkId.remove(event.getPersonId());
	}


	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			return;
		}
		// There might be several departures during a single trip.
		if (!event.getLegMode().equals(parkingCostConfigGroup.getMode())) {
			return;
		}
		personId2relevantModeLinkId.put(event.getPersonId(), event.getLinkId());
	}


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// Preliminaries
		if (ptDrivers.contains(event.getPersonId())) {
			// drives public transport -> doesn't pay for parking
			return;
		}
		if (personId2relevantModeLinkId.get(event.getPersonId()) == null) {
			//
			return;
		}

		if (parkingCostConfigGroup.getActivityPrefixesToBeExcludedFromParkingCost().stream()
				.anyMatch(s -> personId2previousActivity.get(event.getPersonId()).startsWith(s))) {
			return;
		}

		if (hasAlreadyPaidDailyResidentialParkingCosts.contains(event.getPersonId())){
			// has already paid daily residential parking costs
			return;
		}

		Link link = scenario.getNetwork().getLinks().get(personId2relevantModeLinkId.get(event.getPersonId()));
		Attributes attributes = link.getAttributes();

		if (personId2previousActivity.get(event.getPersonId()).startsWith(parkingCostConfigGroup.getActivityPrefixForDailyParkingCosts())) {
			// daily residential parking costs
			hasAlreadyPaidDailyResidentialParkingCosts.add(event.getPersonId());

			double residentialParkingFeePerDay = (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName())).orElse(0.);

			if (residentialParkingFeePerDay > 0.) {
				double amount = -1. * residentialParkingFeePerDay;
				events.processEvent(createPersonMoneyEvent(event, amount, link, true));
			}
			return;
		}

		// other parking cost types

		double costs = 0.;

		int parkingDurationHrs = (int) Math.ceil((event.getTime() - calcParkingStartTime(event)) / 3600.);
		double firstHourParkingCosts = calcFirstHourParkingCosts(attributes);
		double extraHourParkingCosts = calcExtraHourParkingCosts(attributes);

		if (parkingDurationHrs > 0) {
			costs += firstHourParkingCosts;
			costs += (parkingDurationHrs - 1) * extraHourParkingCosts;
		}

		double dailyParkingCosts = calcDailyParkingCosts(attributes, firstHourParkingCosts, extraHourParkingCosts);

		if (costs > dailyParkingCosts) {
			costs = dailyParkingCosts;
		}

		double maxDailyParkingCosts = calcMaxDailyParkingCosts(attributes, dailyParkingCosts);

		if (costs > maxDailyParkingCosts) {
			costs = maxDailyParkingCosts;
		}

		double maxParkingDurationHrs = calcMaxParkingDurationHrs(attributes);
		double parkingPenalty = calcParkingPenalty(attributes);

		if ((parkingDurationHrs > maxParkingDurationHrs) & (costs < parkingPenalty)) {
			costs = parkingPenalty;
		}

		if (costs <= 0.) {
			return;
		}
		double amount = -1. * costs;
		events.processEvent(createPersonMoneyEvent(event, amount, link, false));
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (ptDrivers.contains(event.getPersonId())) {
			return;
		}
		personId2lastLeaveVehicleTime.put(event.getPersonId(), event.getTime());
	}


	// private methods
	private double calcParkingStartTime(PersonEntersVehicleEvent event) {
		return Optional.ofNullable(personId2lastLeaveVehicleTime.get(event.getPersonId())).orElse(0.);
	}
	private double calcExtraHourParkingCosts(Attributes attributes) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName())).orElse(0.);
	}
	private double calcFirstHourParkingCosts(Attributes attributes) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName())).orElse(0.);
	}
	private double calcDailyParkingCosts(Attributes attributes, double firstHourParkingCosts, double extraHourParkingCosts) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getDailyParkingCostLinkAttributeName())).orElse(firstHourParkingCosts + 29 * extraHourParkingCosts);
	}
	private double calcMaxDailyParkingCosts(Attributes attributes, double dailyParkingCosts) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getMaxDailyParkingCostLinkAttributeName())).orElse(dailyParkingCosts);
	}
	private double calcMaxParkingDurationHrs(Attributes attributes) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getMaxParkingDurationAttributeName())).orElse(30.);
	}
	private double calcParkingPenalty(Attributes attributes) {
		return (double) Optional.ofNullable(attributes.getAttribute(parkingCostConfigGroup.getParkingPenaltyAttributeName())).orElse(0.);
	}
	private PersonMoneyEvent createPersonMoneyEvent(PersonEntersVehicleEvent event, double amount, Link link, boolean isResidentialParking) {
		String purpose;
		if (isResidentialParking) {
			purpose = "residential parking";
		} else {
			purpose = "non-residential parking";
		}
		return new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount, purpose, "city", "link " + link.getId());
	}
}

