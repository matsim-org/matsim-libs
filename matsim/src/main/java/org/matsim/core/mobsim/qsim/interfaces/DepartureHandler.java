/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;

public interface DepartureHandler extends QSimComponent {

	enum TimeInterpretation { departure, arrival } ;

	
	/**
	 * @return <code>true</code> if the departure is handled, <code>false</code> if other DepartureHandlers should be tried as well.
	 */
	boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId);

	default List<TripInfo> getTripInfos( Facility fromFacility, Facility toFacility, double time, TimeInterpretation timeInterpretation, Person person ) {
		// yyyy maybe "Switches" instead of "Person"?  Similar to DB router (fast connections, regional trains only, ...)?
		return null ;
	}

	interface TripInfo {
		Facility getPickupLocation() ;
		double getExpectedBoardingTime() ;
		Facility getDropoffLocation() ;
		double getExpectedTravelTime() ;
		double getMonetaryPrice() ;
		Map<String,String> getAdditionalAttributes() ;
		String getMode() ;
		void accept() ;
		double getLatestDecisionTime() ;
	}


}
