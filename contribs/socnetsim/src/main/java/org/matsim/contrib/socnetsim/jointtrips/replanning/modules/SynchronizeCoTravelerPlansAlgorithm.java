/* *********************************************************************** *
 * project: org.matsim.*
 * SynchronizeCoTravelerPlansAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTravelStructure;
import org.matsim.contrib.socnetsim.jointtrips.JointTravelUtils.JointTrip;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * An algorithm which attempts to synchronize the plans of passengers
 * with the plans of drivers.
 * It relies on the travel times stored in the legs for its correctness,
 * and assumes departure times correspond to activity end times.
 *
 * @author thibautd
 */
public class SynchronizeCoTravelerPlansAlgorithm implements GenericPlanAlgorithm<JointPlan> {
	private static final Logger log =
		LogManager.getLogger(SynchronizeCoTravelerPlansAlgorithm.class);

	private final Set<String> stageTypes;
	private final TimeInterpretation timeInterpretation;

	public SynchronizeCoTravelerPlansAlgorithm(TimeInterpretation timeInterpretation) {
		this.stageTypes = JointActingTypes.JOINT_STAGE_ACTS;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public void run(final JointPlan plan) {
		final JointTravelStructure jointTravelStructure =
			JointTravelUtils.analyseJointTravel(plan);

		for ( JointTrip jt : jointTravelStructure.getJointTrips() ) {
			final double pickUpTime = inferPickUpTime( jt , plan );
			if ( pickUpTime < 0 ) {
				throw new RuntimeException( "got negative PU time for joint trip "+jt+" in plan "+plan );
			}
			setPassengerDepartureTime( jt , plan , pickUpTime );
		}
	}

	private void setPassengerDepartureTime(
			final JointTrip jt,
			final JointPlan plan,
			final double pickUpTime) {
		final List<PlanElement> passengerElements =
			plan.getIndividualPlan( jt.getPassengerId() ).getPlanElements();

		int ind = passengerElements.indexOf( jt.getPassengerLeg() );
		if ( ind < 0 ) throw new RuntimeException( "did not found the passenger leg in the plan" );

		double now = pickUpTime;
		while ( ind > 0 ) {
			final PlanElement pe = passengerElements.get( --ind );

			// assume stage activities have 0 duration
			if ( pe instanceof Activity && 
					!(StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() )  ||
					stageTypes.contains(((Activity) pe).getType())) ){
				((Activity) pe).setMaximumDurationUndefined();
				((Activity) pe).setEndTime( now > 0 ? now : 0 );
				return;
			}

			if ( pe instanceof Leg ) {
				final Leg leg = (Leg) pe;
				final OptionalTime legDur = timeInterpretation.decideOnLegTravelTime(leg);

				if ( legDur.isDefined()) {
					now -= legDur.seconds();
				}
				else {
					log.warn( "no time in leg "+leg );
				}
			}
		}

		throw new RuntimeException( "did not found a passenger departure" );
	}

	private double inferPickUpTime(
			final JointTrip jt,
			final JointPlan plan) {
		final Leg firstDriverLeg = jt.getDriverLegs().get( 0 );
		final List<PlanElement> driverPlanElements = plan.getIndividualPlan( jt.getDriverId() ).getPlanElements();

		int ind = driverPlanElements.indexOf( firstDriverLeg );
		if ( ind < 0 ) throw new RuntimeException( "did not found the driver leg in the plan" );

		// parse plan elements backwards, stop at the first act
		double tt = 0;
		while (ind > 0) {
			final PlanElement pe = driverPlanElements.get( --ind );

			// assume stage activities have 0 duration
			if ( pe instanceof Activity &&
					!(StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() )  ||
					stageTypes.contains(((Activity) pe).getType())) ){
				final OptionalTime endTime = ((Activity)pe).getEndTime();
				return endTime.seconds() + tt;
			}

			if ( pe instanceof Leg ) {
				final Leg leg = (Leg) pe;
				final OptionalTime legDur = timeInterpretation.decideOnLegTravelTime(leg);

				if ( legDur.isDefined()) {
					tt += legDur.seconds();
				}
				else {
					log.warn( "no time in leg "+leg );
				}
			}
		}
		
		throw new RuntimeException( "did not found a driver departure" );
	}
}

