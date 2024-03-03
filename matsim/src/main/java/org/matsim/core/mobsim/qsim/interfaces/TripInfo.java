
/* *********************************************************************** *
 * project: org.matsim.*
 * TripInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.interfaces;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.facilities.Facility;

/**
 * Contains the information items that may be returned by, e.g., the Berlkönig or the Deutsche Bahn app.
 */
public interface TripInfo{
	/**
	 * Passenger may have to walk from current location to pickup.
	 */
	Facility getPickupLocation() ;
	/**
	 * Passenger may have to walk from dropoff to final destination.
	 */
	Facility getDropoffLocation() ;
	// If these need an ID, they need to be ActivityFacilities.  Otherwise, they are ad-hoc facilities, which is probably also ok. kai, mar'19

	double getExpectedBoardingTime() ;
	double getExpectedTravelTime() ;
	double getMonetaryPrice() ;
	Map<String,String> getAdditionalAttributes() ;
	String getMode() ;
	/**
	 * @return latest time until which the trip provider will accept a binding booking.
	 */
	double getLatestDecisionTime() ;
	Request getOriginalRequest() ;
	/**
	 * Method that needs to be called to book this {@link TripInfo} option.  Not all options allow booking.
	 */
	void bookTrip( MobsimPassengerAgent agent ) ;


	/**
	 * This is the "thing" that will answer the {@link Request}.  It will return a list of {@link TripInfo} options.
	 */
	interface Provider{
		List<TripInfo> getTripInfos( Request request ) ;
		String getMode();
		// not sure if I like that, but with current design (where the confirmation goes to the TripInfo.Provider, not to the TripInfo instance that we have) I am not sure
		// if it is possible otherwise.  kai, mar'19
		void bookTrip( MobsimPassengerAgent agent, TripInfoWithRequiredBooking tripInfo );
	}


	/**
	 * This is the request that we make, for example, to the Berlkönig App.  The planned {@link Route} comes with it, since important info for the
	 * drt request is stored in it.
	 */
	interface Request{
		enum TimeInterpretation {departure, arrival}
		Facility getFromFacility();
		Facility getToFacility();
		double getTime();
		TimeInterpretation getTimeInterpretation();
		Route getPlannedRoute();
	}
}
