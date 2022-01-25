
/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

class TripInfoRequestWithActivities implements TripInfo.Request{
	// this is a pure data class and so one could decide the not have it behind an interface.  However, having it behind the interface means that only
	// hte getters are public, which leaves us more room to refactor.  kai, jan'20

	private final Facility fromFacility;
	private final Facility toFacility;
	private final double time;
	private final TimeInterpretation timeInterpretation;
	private final Route route;
//	private final Activity fromActivity;
//	private final Activity toActivity;

	private TripInfoRequestWithActivities( Scenario scenario, Activity fromActivity, Activity toActivity, double time,
					       TimeInterpretation timeInterpretation, Route route ) {
		this.route = route;
//		this.fromActivity = fromActivity;
//		this.toActivity = toActivity;
		this.fromFacility = FacilitiesUtils.toFacility(fromActivity, scenario.getActivityFacilities());
		this.toFacility = FacilitiesUtils.toFacility(toActivity, scenario.getActivityFacilities());
		this.time = time;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public Facility getFromFacility() {
		return fromFacility;
	}

	@Override
	public Facility getToFacility() {
		return toFacility;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public TimeInterpretation getTimeInterpretation() {
		return timeInterpretation;
	}

	@Override
	public Route getPlannedRoute(){
		return route;
	}

//	Activity getFromActivity() {
//		return fromActivity;
//	}
//
//	Activity getToActivity() {
//		return toActivity;
//	}
	// these were used in the code to actually change the activity end time.  There is, however, no guarantee that these are still the behavioral objects
	// .  kai, jan'20

	static class Builder {
		private final Scenario scenario;
		private Route route;
		// this is deliberately a builder and not a constructor so that we can add arguments later without having to add constructors with longer and longer
		// argument lists.  kai, mar'19

		Builder(Scenario scenario) {
			this.scenario = scenario;
		}

		private double time;
		private TimeInterpretation timeInterpretation = TimeInterpretation.departure;
		private Activity fromActivity;
		private Activity toActivity;

		Builder setFromActivity(Activity fromActivity) {
			this.fromActivity = fromActivity;
			return this;
		}

		Builder setToActivity(Activity toActivity) {
			this.toActivity = toActivity;
			return this;
		}

		Builder setTime(double time) {
			this.time = time;
			return this;
		}

		Builder setTimeInterpretation(TimeInterpretation timeInterpretation) {
			this.timeInterpretation = timeInterpretation;
			return this;
		}

		Builder setPlannedRoute( Route route ) {
			this.route = route ;
			return this ;
		}

		TripInfo.Request createRequest() {
			return new TripInfoRequestWithActivities(scenario, fromActivity, toActivity, time, timeInterpretation, route);
		}
	}
}
