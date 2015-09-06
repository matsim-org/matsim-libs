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

package playground.anhorni.choiceSetGeneration.helper;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacility;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;

public class ReaderTripHandler {

	private final static Logger log = Logger.getLogger(ReaderTripHandler.class);
	private Trip trip;
	private double travelTimeBudget;
	private Id<ActivityFacility> chosenFacilityId;


	public void constructTrip(String [] entries, NetworkImpl network, ZHFacilities facilities,
			MZTrip mzTrip, int tripNr) {

		Coord beforeShoppingCoord = new Coord(Double.parseDouble(entries[38].trim()), Double.parseDouble(entries[39].trim()));
		ActivityImpl beforeShoppingAct = new org.matsim.core.population.ActivityImpl("start", beforeShoppingCoord);
		// in seconds after midnight
		double endTimeBeforeShoppingAct = 60.0 * Double.parseDouble(entries[12].trim());
		beforeShoppingAct.setEndTime(endTimeBeforeShoppingAct);
		beforeShoppingAct.setLinkId(NetworkUtils.getNearestLink(network, beforeShoppingCoord).getId());

		Id<ActivityFacility> chosenFacilityId = Id.create(entries[2].trim(), ActivityFacility.class);

		ZHFacility chosenFacility = facilities.getZhFacilities().get(chosenFacilityId);
		Link link = network.getLinks().get(chosenFacility.getLinkId());

		ActivityImpl shoppingAct = new org.matsim.core.population.ActivityImpl("shop", link.getId());
		shoppingAct.setCoord(link.getCoord());

		double startTimeShoppingAct = 60.0 * Double.parseDouble(entries[15].trim());
		shoppingAct.setStartTime(startTimeShoppingAct);
		double endTimeShoppingAct = mzTrip.getStartTime();
		shoppingAct.setEndTime(endTimeShoppingAct);

		this.chosenFacilityId = chosenFacilityId;

		Coord afterShoppingCoord = mzTrip.getCoordEnd();
		ActivityImpl afterShoppingAct = new org.matsim.core.population.ActivityImpl("end", afterShoppingCoord);
		afterShoppingAct.setLinkId(NetworkUtils.getNearestLink(network, afterShoppingCoord).getId());

		if (!(mzTrip.getEndTime() > 0.0)) {
			log.error("No end time found for person : " /*+ mzTrip.getPersonId()*/);
		}
		double startTimeAfterShoppingAct = mzTrip.getEndTime();
		afterShoppingAct.setStartTime(startTimeAfterShoppingAct);

		this.travelTimeBudget = (startTimeAfterShoppingAct- endTimeBeforeShoppingAct) - (endTimeShoppingAct - startTimeShoppingAct);

		Trip trip = new Trip(tripNr, beforeShoppingAct, shoppingAct, afterShoppingAct);
		this.trip = trip;
	}

	public Trip getTrip() {
		return trip;
	}
	public void setTrip(Trip trip) {
		this.trip = trip;
	}
	public double getTravelTimeBudget() {
		return travelTimeBudget;
	}
	public void setTravelTimeBudget(double travelTimeBudget) {
		this.travelTimeBudget = travelTimeBudget;
	}

	public Id<ActivityFacility> getChosenFacilityId() {
		return chosenFacilityId;
	}
}
