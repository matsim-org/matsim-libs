
/* *********************************************************************** *
 * project: org.matsim.*
 * TripInfoRequest.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import static org.matsim.core.mobsim.qsim.interfaces.TripInfo.*;

public class TripInfoRequest{
	private final Facility fromFacility;
	private final Facility toFacility;
	private final double time;
	private final TimeInterpretation timeInterpretation;
	private final Activity fromActivity;
	private final Activity toActivity;

	private TripInfoRequest( Scenario scenario, Activity fromActivity, Activity toActivity, double time, TimeInterpretation timeInterpretation ){
		this.fromActivity = fromActivity;
		this.toActivity = toActivity;
		this.fromFacility = FacilitiesUtils.toFacility( fromActivity, scenario.getActivityFacilities() ) ;
		this.toFacility = FacilitiesUtils.toFacility( toActivity, scenario.getActivityFacilities() ) ;
		this.time = time;
		this.timeInterpretation = timeInterpretation;
	}

	public Facility getFromFacility(){
		return fromFacility;
	}

	public Facility getToFacility(){
		return toFacility;
	}

	public double getTime(){
		return time;
	}

	public TimeInterpretation getTimeInterpretation(){
		return timeInterpretation;
	}

	public Activity getFromActivity(){
		return fromActivity;
	}

	public Activity getToActivity(){
		return toActivity;
	}

	public static class Builder{
		private final Scenario scenario;
		// this is deliberately a builder and not a constructor so that we can add arguments later without having to add constructors with longer and longer
		// argument lists.  kai, mar'19

		public Builder( Scenario scenario ) {
			this.scenario = scenario ;
		}

		private double time;
		private TimeInterpretation timeInterpretation = TimeInterpretation.departure ;
		private Activity fromActivity;
		private Activity toActivity;

		public Builder setFromActivity( Activity fromActivity ){
			this.fromActivity = fromActivity;
			return this;
		}

		public Builder setToActivity( Activity toActivity ){
			this.toActivity = toActivity;
			return this;
		}

		public Builder setTime( double time ){
			this.time = time;
			return this;
		}

		public Builder setTimeInterpretation( TimeInterpretation timeInterpretation ){
			this.timeInterpretation = timeInterpretation;
			return this;
		}

		public TripInfoRequest createRequest(){
			return new TripInfoRequest( scenario, fromActivity, toActivity, time, timeInterpretation );
		}
	}
}
