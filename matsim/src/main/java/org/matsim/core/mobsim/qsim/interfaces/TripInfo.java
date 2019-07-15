
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

import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.facilities.Facility;

public interface TripInfo{
	Facility getPickupLocation() ;
	Facility getDropoffLocation() ;
	// If these need an ID, they need to be ActivityFacilities.  Otherwise, they are ad-hoc facilities, which is probably also ok. kai, mar'19

	double getExpectedBoardingTime() ;
	double getExpectedTravelTime() ;
	double getMonetaryPrice() ;
	Map<String,String> getAdditionalAttributes() ;
	String getMode() ;
	double getLatestDecisionTime() ;
	TripInfoRequest getOriginalRequest() ;

	enum TimeInterpretation { departure, arrival }

	interface Provider{
		List<TripInfo> getTripInfos( TripInfoRequest request ) ;

		String getMode();
		// not sure if I like that, but with current design (where the confirmation goes to the TripInfo.Provider, not to the TripInfo instance that we have) I am not sure
		// if it is possible otherwise.  kai, mar'19

		void bookTrip( MobsimPassengerAgent agent, TripInfoWithRequiredBooking tripInfo );
	}

}
