
/* *********************************************************************** *
 * project: org.matsim.*
 * EditPlans.java
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

 package org.matsim.withinday.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

public final class EditPlans {
	private static final Logger log = LogManager.getLogger( EditPlans.class ) ;

	private final QSim mobsim;
	private final EditTrips editTrips;
	private final PopulationFactory pf;
	@Deprecated // population factory argument not needed.  kai, apr'18
	public EditPlans( QSim mobsim, TripRouter tripRouter, EditTrips editTrips, PopulationFactory pf ) {
		this( mobsim, editTrips ) ;
	}
	@Deprecated // scenario argument not needed.  kai, apr'18
	public EditPlans( QSim mobsim, TripRouter tripRouter, EditTrips editTrips, Scenario sc) {
		this( mobsim, editTrips ) ;
	}
	@Deprecated // tripRouter argument not needed. gleich-oct'19
	public EditPlans( QSim mobsim, TripRouter tripRouter, EditTrips editTrips ) {
		this( mobsim, editTrips ) ;
	}
	public EditPlans( QSim mobsim, EditTrips editTrips ) {
		Gbl.assertNotNull( this.mobsim = mobsim );
		Gbl.assertNotNull( this.editTrips = editTrips ) ;
		Gbl.assertNotNull( this.pf = mobsim.getScenario().getPopulation().getFactory() ) ;
	}
	public boolean addActivityAtEnd(MobsimAgent agent, Activity activity, String routingMode) {
		log.debug("entering addActivityAtEnd with routingMode=" + routingMode) ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		List<PlanElement> planElements = plan.getPlanElements();

		boolean retVal1 = false;

		if (isAtRealActivity(agent)) {
			retVal1 = planElements.add(pf.createLeg(routingMode));
		}

		final boolean retVal = planElements.add(activity);
		// (need the terminating activity in order to find the current trip. kai, nov'17)

		if (!isAtRealActivity(agent)) {
			retVal1 = editTrips.replanCurrentTrip(agent,mobsim.getSimTimer().getTimeOfDay(),routingMode);
		}


		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return (retVal1 && retVal);
	}
	public PlanElement removeActivity(MobsimAgent agent, int index, String mode) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		checkIfNotInPastOrCurrent(agent, index);

		final Trip tripBefore = TripStructureUtils.findTripEndingAtActivity( (Activity) planElements.get(index),plan );
		final Trip tripAfter = TripStructureUtils.findTripStartingAtActivity( (Activity)planElements.get(index),plan );
		if ( mode==null ) {
			final String mainModeBefore = TripStructureUtils.identifyMainMode( tripBefore.getTripElements() );
			final String mainModeAfter = TripStructureUtils.identifyMainMode( tripAfter.getTripElements() );
			if ( mainModeBefore.equals( mainModeAfter ) ) {
				mode = mainModeBefore ;
			} else {
				throw new ReplanningException("mode not given and mode before removed activity != mode after removed activity; don't know which mode to use") ;
			}
		}
		PlanElement pe = planElements.remove(index) ;
		if ( checkIfTripHasAlreadyStarted( agent, tripBefore.getTripElements() ) ) {
			editTrips.replanCurrentTrip(agent, mobsim.getSimTimer().getTimeOfDay() , mode);
		} else {
			editTrips.insertEmptyTrip(plan, tripBefore.getOriginActivity(), tripAfter.getDestinationActivity(), mode ) ;
		}
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return pe ;
	}
	public final void rescheduleActivityEndtime( MobsimAgent agent, int index, double newEndTime ) {
		Activity activity = (Activity) WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(index) ;
		activity.setEndTime(newEndTime);
		WithinDayAgentUtils.resetCaches(agent);
		WithinDayAgentUtils.rescheduleActivityEnd(agent, mobsim);
	}
	public final Activity replaceActivity(MobsimAgent agent, int index, Activity newAct, String upstreamMode, String downstreamMode ) {
		System.err.println("here310");
		WithinDayAgentUtils.printPlan(agent) ;
		System.err.println("here320");
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		WithinDayAgentUtils.printPlan(plan) ;
		System.err.println("here330");


		List<PlanElement> planElements = plan.getPlanElements() ;

		// make sure we have indeed an activity position:
		if ( ! ( planElements.get(index) instanceof Activity ) ) {
			throw new ReplanningException("trying to replace a non-activity in the plan by an activity; this is not possible") ;
		}
		Activity origAct = (Activity) planElements.get(index) ;

		checkIfNotStageActivity(origAct);

		checkIfNotInPastOrCurrent(agent, index);

		// set the new activity:
		planElements.set(index, newAct) ;
		System.err.println("here340");
		WithinDayAgentUtils.printPlan(plan) ;

		// trip before (if any):
		if ( index > 0 ) {
			Trip tripBeforeAct = TripStructureUtils.findTripEndingAtActivity( newAct,plan );
			Gbl.assertNotNull( tripBeforeAct );  // there could also just be a sequence of activities?!

			final List<PlanElement> currentTripElements = tripBeforeAct.getTripElements();
			final String currentMode = TripStructureUtils.identifyMainMode( currentTripElements ) ;

			if ( checkIfTripHasAlreadyStarted(agent, currentTripElements) ) {
				// trip has already started
				checkIfSameMode(upstreamMode, currentMode);
				this.editTrips.replanCurrentTrip(agent, this.mobsim.getSimTimer().getTimeOfDay(), currentMode );
			} else {
				// trip has not yet started
				if ( upstreamMode == null ) {
					upstreamMode = currentMode ;
				}
				this.editTrips.insertEmptyTrip( plan, tripBeforeAct.getOriginActivity(), newAct, upstreamMode );
			}
		}
		// trip after (if any):
		if ( index < planElements.size()-1 ) {
			Trip tripAfterAct = TripStructureUtils.findTripStartingAtActivity(origAct,plan);
			Gbl.assertIf( tripAfterAct!=null ); // there could also just be a sequence of activities?!
			if ( downstreamMode==null ) {
				final String currentMainMode = TripStructureUtils.identifyMainMode( tripAfterAct.getTripElements() );
				EditTrips.insertEmptyTrip(plan, newAct, tripAfterAct.getDestinationActivity(), currentMainMode, pf);
			} else {
				EditTrips.insertEmptyTrip(plan, newAct, tripAfterAct.getDestinationActivity(), downstreamMode, pf);
			}
		}
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
		return origAct ;
	}
	public void insertActivity(MobsimAgent agent, int index, Activity activity, String upstreamMode, String downstreamMode ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		checkIfNotInPastOrCurrent(agent, index) ;
		planElements.add( index, activity ) ;
		{
			// activity before:
			Activity actBefore = findRealActBefore(agent, index);
			if ( actBefore != null ) {
				if ( WithinDayAgentUtils.indexOfPlanElement(agent, actBefore) < WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ) {
					// we are already under way
					editTrips.replanCurrentTrip(agent, this.mobsim.getSimTimer().getTimeOfDay(), upstreamMode );
				} else {
					// we are not yet under way; inserting empty trip:
					EditTrips.insertEmptyTrip(plan, actBefore, activity, upstreamMode, pf ) ;
				}
			}
		}
		{
			// activity after:
			Activity actAfter = findRealActAfter(agent, index);
			if ( actAfter != null ) {
				EditTrips.insertEmptyTrip(plan, activity, actAfter, downstreamMode, pf ) ;
			}
		}
		WithinDayAgentUtils.resetCaches(agent);
		this.mobsim.rescheduleActivityEnd(agent);
	}

	// === convenience methods: ===
	/**
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public PlanElement removeActivity(MobsimAgent agent, int index) {
		return removeActivity( agent, index, null ) ;
	}
	/**
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public final Activity replaceActivity(MobsimAgent agent, int index, Activity newAct) {
		return replaceActivity( agent, index, newAct, null, null ) ;
	}
	/**
	 * Convenience method, clarifying that this can be called without giving the mode.
	 */
	public void insertActivity(MobsimAgent agent, int index, Activity activity ) {
		String mode = TripStructureUtils.identifyMainMode( EditTrips.findCurrentTrip(agent ).getTripElements() ) ;
		insertActivity( agent, index, activity, mode, mode ) ;
	}

	// === internal utility methods: ===
	private void checkIfNotStageActivity(Activity origAct) {
		if( StageActivityTypeIdentifier.isStageActivity(origAct.getType()) ){
			throw new ReplanningException("trying to replace a helper activity (stage activity) by a real activity; this is not possible") ;
		}
	}
	private static boolean checkIfTripHasAlreadyStarted(MobsimAgent agent, final List<PlanElement> currentTripElements) {
		return currentTripElements.contains( ((PlanAgent)agent).getCurrentPlanElement() ) ;
	}
	private static void checkIfNotInPastOrCurrent(MobsimAgent agent, int index) {
		final Integer currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// make sure we are not yet beyond the to-be-replanned activity:
		if ( index <= currentIndex ) {
			throw new ReplanningException("trying to replace an activity that lies in the past or is current; this is not possible") ;
		}
	}
	private static void checkIfSameMode(String upstreamMode, final String currentMode) {
		if ( upstreamMode!=null && !upstreamMode.equals(currentMode) ) {
			throw new ReplanningException( "cannot change mode in trip that has already started.  Don't set the mode in the request, "
					+ "or somehow make the agent to abort the current trip first." ) ;
		}
	}
	//	private static boolean locationsAreSame(Activity origAct, Activity newAct) {
	//		if ( origAct.getFacilityId() != null && newAct.getFacilityId() != null ) {
	//			return origAct.getFacilityId().equals( newAct.getFacilityId() ) ;
	//		}
	//		if ( origAct.getCoord() != null && newAct.getCoord() != null ) {
	//			return origAct.getCoord().equals( newAct.getCoord() ) ;
	//		}
	//		return false ;
	//	}
	public static Activity findRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		return (Activity) planElements.get( findIndexOfRealActAfter(agent, index) ) ;
	}
	public static int findIndexOfRealActAfter(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		int theIndex = -1 ;
		for ( int ii=planElements.size()-1 ; ii>index; ii-- ) {
			if ( planElements.get(ii) instanceof Activity ) {
				Activity act = (Activity) planElements.get(ii) ;
				if ( !StageActivityTypeIdentifier.isStageActivity( act.getType() ) ) {
					theIndex = ii ;
				}
			}
		}
		return theIndex ;
	}
	public static Activity findRealActBefore(MobsimAgent agent, int index) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		Activity prevAct = null ;
		for ( int ii=0 ; ii<index ; ii++ ) {
			if ( planElements.get(ii) instanceof Activity ) {
				Activity act = (Activity) planElements.get(ii) ;
				if ( !StageActivityTypeIdentifier.isStageActivity( act.getType() ) ) {
					prevAct = act ;
				}
			}
		}
		return prevAct;
	}
	//	private Facility asFacility(Activity activity) {
	//		Facility hereFacility = new ActivityWrapperFacility( activity ) ;
	//		if ( activity.getFacilityId()!=null ) {
	//			ActivityFacility facility = this.mobsim.getScenario().getActivityFacilities().getFacilities().get( activity.getFacilityId() ) ;
	//			if ( facility != null ) {
	//				hereFacility = facility ;
	//			}
	//		}
	//		return hereFacility;
	//	}

	public void rescheduleActivityEnd(MobsimAgent agent) {
		// this is mostly for retrofitting existing code.  but maybe also useful by itself
		this.mobsim.rescheduleActivityEnd(agent);
	}
	public boolean isAtRealActivity(MobsimAgent agent) {
		PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		if ( isRealActivity(pe) ) {
			return true ;
		} else {
			return false ;
		}
	}
	public boolean isRealActivity(PlanElement pe) {
		return pe instanceof Activity && ! ( StageActivityTypeIdentifier.isStageActivity( ((Activity)pe).getType() ) );
	}

	public String getModeOfCurrentOrNextTrip(MobsimAgent agent) {
		Trip trip ;
		if ( isAtRealActivity( agent ) ) {
			Activity activity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(agent) ;
			trip = editTrips.findTripAfterActivity(WithinDayAgentUtils.getModifiablePlan(agent), activity) ;
		} else {
			trip = EditTrips.findCurrentTrip(agent ) ;
		}
		return TripStructureUtils.identifyMainMode(trip.getTripElements()) ;
	}
	public void flushEverythingBeyondCurrent(MobsimAgent agent) {
		List<PlanElement> pes = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements() ;
		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
		for ( int ii=pes.size()-1 ; ii>index ; ii-- ) {
			pes.remove(ii) ;
		}
	}
	public void rescheduleCurrentActivityEndtime(MobsimAgent agent, double newEndTime) {
		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;
		this.rescheduleActivityEndtime(agent, index, newEndTime);
	}

	public Activity createFinalActivity(String type, Id<Link> newLinkId) {
		Activity newAct = this.pf.createActivityFromLinkId(type, newLinkId);
		newAct.setEndTime( Double.POSITIVE_INFINITY ) ;
		return newAct ;
	}
//	public Activity createAgentThatKeepsMatsimAlive( String type, Id<Link> newLinkId) {
//		Activity newAct = this.pf.createActivityFromLinkId( type, newLinkId);;
//		newAct.setEndTime( Double.MAX_VALUE ) ;
//		return newAct ;
//	}
	public static Integer getCurrentPlanElementIndex( MobsimAgent agent ) {
		return WithinDayAgentUtils.getCurrentPlanElementIndex( agent ) ;
	}

	public static List<Leg> findLegsWithModeInFuture( MobsimAgent agent, String mode ) {
		List<Leg> retVal = new ArrayList<>();
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		for (int ii = WithinDayAgentUtils.getCurrentPlanElementIndex(agent); ii < plan.getPlanElements().size(); ii++) {
			PlanElement pe = plan.getPlanElements().get(ii);
			if (pe instanceof Leg) {
				if ( Objects.equals(mode, ((Leg)pe).getMode() )) {
					retVal.add((Leg)pe);
				}
			}
		}
		return retVal;
	}

}
