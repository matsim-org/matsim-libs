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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntry;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntryComparator;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.router.*;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static org.matsim.core.mobsim.qsim.interfaces.TripInfoProvider.*;
import static org.matsim.core.router.TripStructureUtils.*;

public class ActivityEngineWithWakeup implements MobsimEngine, ActivityHandler {
	private static final Logger log = Logger.getLogger( ActivityEngine.class ) ;
	private final EditTrips editTrips;
	private final EditPlans editPlans;
	// after mobsim the alterations to the plans are deleted (a reset is needed before next iteration in any case)? Don't we need rather something like "executed plan"

	private final Map<String, DepartureHandler> departureHandlers;
	private final Scenario scenario;
	private final ActivityFacilities facilities;
	private final double beelineWalkSpeed;

	private ActivityEngine delegate ;
	private TimeInterpretation departure;

	@Inject
	public ActivityEngineWithWakeup( EventsManager eventsManager, Map<String, DepartureHandler> departureHandlers,
						   TripRouter tripRouter, Scenario scenario, QSim qsim, Config config, MessageQueue messageQueue ) {
		this.departureHandlers = departureHandlers;
		this.editTrips = new EditTrips( tripRouter, scenario ) ;
		this.editPlans = new EditPlans(qsim, tripRouter, editTrips);
		this.delegate = new ActivityEngine( eventsManager ) ;
		this.scenario = scenario ;
		this.facilities = scenario.getActivityFacilities();
		
		PlansCalcRouteConfigGroup pcrConfig = config.plansCalcRoute();
		double beelineDistanceFactor = pcrConfig.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
		this.beelineWalkSpeed = pcrConfig.getTeleportedModeSpeeds().get(TransportMode.walk)
				/ beelineDistanceFactor ;
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
				Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );
				Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );;

				Activity currAct = (Activity) plan.getPlanElements().get(index ); // at this point, we are trying to get this up and running while being at an activity

				Trip trip = editTrips.findTripAfterActivity( plan, currAct ) ;

				String abc = "drt interaction" ; // yyyy find global constant
				StageActivityTypesImpl drtStageActivities = new StageActivityTypesImpl( abc ) ;
				Trip drtTrip = TripStructureUtils.getTrips( trip.getTripElements(), drtStageActivities ).get( 0 );; // we are assuming that the drt trip is the _next_ such trip

				Facility fromFacility = toFacility( drtTrip.getOriginActivity() ) ;
				Facility toFacility = toFacility( drtTrip.getDestinationActivity() ) ;

				Map<TripInfo,DepartureHandler> allTripInfos  = new LinkedHashMap<>() ;

				Person person = null ;
				if ( agent instanceof HasPerson  ){
					person = ((HasPerson) agent).getPerson();
				}
				for ( DepartureHandler handler : departureHandlers.values() ) {
					if ( handler instanceof TripInfoProvider ){
						List<TripInfo> tripInfos = ((TripInfoProvider) handler).getTripInfos( fromFacility, toFacility, time, departure, person );
						for( TripInfo tripInfo : tripInfos ){
							allTripInfos.put( tripInfo, handler ) ;
						}
					}
				}
				// add info for mode that is in agent plan, if not returned by departure handler.
				decide( agent, allTripInfos, fromFacility, toFacility ) ;
			}
		}
		delegate.doSimStep( time );
	}

	@Deprecated // needs to be centrlalized
	private Facility toFacility(final Activity act) {
		// use facility first if available i.e. reversing the logic above Amit July'18
		// yyyyyy these things need to be centralized!
		if (	facilities != null &&
				! facilities.getFacilities().isEmpty() &&
				act.getFacilityId() != null ) {
			return facilities.getFacilities().get( act.getFacilityId() );
		}

		return new ActivityWrapperFacility( act );
	}

	private void decide( MobsimAgent agent, Map<TripInfo, DepartureHandler> allTripInfos, Facility fromFacility, Facility toFacility ){
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement( agent );
		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;
		String mode = editPlans.getModeOfCurrentOrNextTrip( agent );;
		if ( !TransportMode.drt.equals( mode ) ) {
			return ;
		}
		
		// TODO: Decision logic between multiple offers / tripInfos --> some simple score prognosis?
		TripInfo tripInfo = allTripInfos.keySet().iterator().next() ;

//		tripInfo.accept();

//		DepartureHandler handler = allTripInfos.get(tripInfo) ;
//
//		if ( handler instanceof Prebookable ) {
//			((Prebookable) handler).prebookTrip(now, agent, fromLinkId, toLinkId, departureTime) ;
//		}

		TripInfo confirmation = null ;
		if ( tripInfo instanceof CanBeAccepted ) {
			confirmation = ((CanBeAccepted)tripInfo).accept() ;
			if( confirmation==null ) {
				confirmation = tripInfo ;
			}
		}

		BookingNotificationEngine be = null ; // get from somewhere

		if ( confirmation.getPickupLocation()!=null ) {
			be.notifyChangedTripInformation( agent, confirmation );
		} else{
			// schedule activity end time of activity before drt walk to infinity so agent does not start drt walk before position is known.
		}
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

		this.wakeUpList.add( new AgentEntry( agent, agent.getActivityEndTime() - 900. ) ) ;

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
