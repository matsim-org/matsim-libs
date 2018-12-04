/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntry;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntryComparator;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;
import org.opengis.filter.spatial.Within;
import sun.management.resources.agent;

import javax.inject.Inject;

public class ActivityEngineWithWakeup implements MobsimEngine, ActivityHandler {
	private static final Logger log = Logger.getLogger( ActivityEngine.class ) ;
	private final EditTrips editTrips;

	private final Map<String, DepartureHandler> departureHandlers;
	private final Scenario scenario;

	private ActivityEngine delegate ;

	@Inject
	public ActivityEngineWithWakeup( EventsManager eventsManager, Map<String, DepartureHandler> departureHandlers,
				     TripRouter tripRouter, Scenario scenario ) {
		this.departureHandlers = departureHandlers;
		this.editTrips = new EditTrips( tripRouter, scenario ) ;
		this.delegate = new ActivityEngine( eventsManager ) ;
		this.scenario = scenario ;
	}

	private final Queue<AgentEntry> wakeUpList = new PriorityBlockingQueue<>(500, new AgentEntryComparator() );

	@Override
	public void onPrepareSim() {
		delegate.onPrepareSim();
	}

	@Override
	public void doSimStep(double time) {
		while ( !wakeUpList.isEmpty() ) {
			if ( wakeUpList.peek().time <= time ) {
				MobsimAgent agent = wakeUpList.poll().agent ;
				Collection<TripInfo> allTripInfos  = new ArrayList<>() ;
				for ( DepartureHandler handler : departureHandlers.values() ) {
					allTripInfos.addAll( handler.getTripInfos() ) ;
				}
				decide( agent, allTripInfos, editTrips ) ;
			}
		}
		delegate.doSimStep( time );
	}

	private void decide( MobsimAgent agent, Collection<TripInfo> allTripInfos, EditTrips editTrips, EditPlans editPlans ){
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement( agent );
		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;
		String mode = editPlans.getModeOfCurrentOrNextTrip( agent );;
		if ( !TransportMode.drt.equals( mode ) ) {
			return ;
		}

		TripInfo tripinfo = null;

		tripinfo.accept();

		TripStructureUtils.Trip trip = editTrips.findTripAfterActivity( plan, currentActivity ) ;

		List<PlanElement> newTrip = new ArrayList<>(  ) ;
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		double walkTime = 60. ;
		{
			Leg leg = pf.createLeg( TransportMode.access_walk );
			// also add generic route
			leg.setTravelTime( walkTime );
			newTrip.add( leg ) ;
		}
		{
			Activity activity = pf.createActivityFromCoord( "drt_interaction", tripinfo.getPickupLocation().getCoord() ) ;
			activity.setFacilityId( tripinfo.getPickupLocation().getId() ); // how is this solved in transit router?
			newTrip.add( activity ) ;
		}
		{
			Leg leg = pf.createLeg( TransportMode.drt ) ;
			// also add generic route
			newTrip.add( leg ) ;
		}
		{
			Activity activity = pf.createActivityFromCoord( "drt_interaction", tripinfo.getDropoffLocation().getCoord() ) ;
			activity.setFacilityId( tripinfo.getDropoffLocation().getId() ); // how is this solved in transit router?
			newTrip.add( activity ) ;
		}
		{
			Leg leg = pf.createLeg( TransportMode.egress_walk ) ;
			// also add generic route
			newTrip.add( leg ) ;
		}
		TripRouter.insertTrip( plan, currentActivity, newTrip, trip.getDestinationActivity() ) ;

		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );;
		editPlans.rescheduleActivityEndtime( agent, index, tripinfo.getExpectedBoardingTime() - walkTime - buffer );

		rescheduleActivityEnd( agent ); // not sure if this is necessary; might be contained in the above
	}

	@Override
	public void afterSim() {
		delegate.afterSim( );
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		delegate.setInternalInterface( internalInterface );
	}

	
	/**
	 * 
	 * This method is called by QSim to pass in agents which then "live" in the activity layer until they are handed out again
	 * through the internalInterface.
	 * 
	 * It is called not before onPrepareSim() and not after afterSim(), but it may be called before, after, or from doSimStep(),
	 * and even from itself (i.e. it must be reentrant), since internalInterface.arrangeNextAgentState() may trigger
	 * the next Activity.
	 * 
	 */
	@Override
	public boolean handleActivity(MobsimAgent agent) {
		return delegate.handleActivity( agent ) ;
	}

	/**
	 * For within-day replanning. Tells this engine that the activityEndTime the agent reports may have changed since
	 * the agent was added to this engine through handleActivity.
	 * May be merged with handleActivity, since this engine can know by itself if it was called the first time
	 * or not.
	 *
	 * @param agent The agent.
	 */
	@Override
	public void rescheduleActivityEnd(final MobsimAgent agent) {
		delegate.rescheduleActivityEnd( agent );
	}

}
