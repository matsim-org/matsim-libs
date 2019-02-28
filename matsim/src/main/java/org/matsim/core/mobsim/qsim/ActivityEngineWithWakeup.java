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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntry;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntryComparator;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.router.*;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;
import org.matsim.withinday.utils.ReplanningException;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.createStageActivityType;
import static org.matsim.core.mobsim.qsim.interfaces.TripInfoProvider.*;
import static org.matsim.core.router.TripStructureUtils.*;

public class ActivityEngineWithWakeup implements MobsimEngine, ActivityHandler {
	private static final Logger log = Logger.getLogger( ActivityEngine.class ) ;
	private final EditTrips editTrips;
	private final EditPlans editPlans;
	// after mobsim the alterations to the plans are deleted (a reset is needed before next iteration in any case)? Don't we need rather something like "executed plan"

	private final Map<String, DepartureHandler> departureHandlers;
	private final ActivityFacilities facilities;
	private final double beelineWalkSpeed;
	private final MainModeIdentifier mainModeIdentifier;

	private ActivityEngine delegate ;

	StageActivityTypes drtStageActivities = new StageActivityTypesImpl( createStageActivityType( TransportMode.drt  ) , createStageActivityType( TransportMode.walk ) ) ;

	private final Queue<AgentAndLegEntry> wakeUpList = new PriorityBlockingQueue<>(500, new AgentEntryComparator() );

	@Inject
	ActivityEngineWithWakeup( EventsManager eventsManager, Map<String, DepartureHandler> departureHandlers,
						   TripRouter tripRouter, Scenario scenario, QSim qsim, Config config ) {
		this.departureHandlers = departureHandlers;
		this.editTrips = new EditTrips( tripRouter, scenario ) ;
		this.editPlans = new EditPlans(qsim, tripRouter, editTrips);
		this.delegate = new ActivityEngine( eventsManager ) ;
		this.facilities = scenario.getActivityFacilities();

		this.mainModeIdentifier = tripRouter.getMainModeIdentifier() ;
		
		PlansCalcRouteConfigGroup pcrConfig = config.plansCalcRoute();
		double beelineDistanceFactor = pcrConfig.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
		this.beelineWalkSpeed = pcrConfig.getTeleportedModeSpeeds().get(TransportMode.walk) / beelineDistanceFactor ;
	}

	@Override
	public void onPrepareSim() {
		delegate.onPrepareSim();
	}

	@Override
	public void doSimStep(double time) {
		while ( !wakeUpList.isEmpty() ) {
			if ( wakeUpList.peek().time <= time ) {
				final AgentAndLegEntry entry = wakeUpList.poll();
				MobsimAgent agent = entry.agent ;
				Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );

				// search for drt trip corresponding to drt leg:
				List<Trip> trips = TripStructureUtils.getTrips( plan, this.drtStageActivities ) ;
				Trip drtTrip = null ;
				for ( Trip trip : trips ){
					if( trip.getTripElements().contains( entry.leg ) ) {
						drtTrip = trip;
						break;
					}
				}
				Gbl.assertNotNull( drtTrip );

				Facility fromFacility = FacilitiesUtils.toFacility( drtTrip.getOriginActivity(), facilities ) ;
				Facility toFacility = FacilitiesUtils.toFacility( drtTrip.getDestinationActivity(), facilities ) ;

				Map<TripInfo,DepartureHandler> allTripInfos  = new LinkedHashMap<>() ;

				Person person = agent instanceof HasPerson ? ((HasPerson) agent).getPerson() : null ;

				for ( DepartureHandler handler : departureHandlers.values() ) {
					if ( handler instanceof TripInfoProvider ){
						List<TripInfo> tripInfos = ((TripInfoProvider) handler).getTripInfos( fromFacility, toFacility, time, TimeInterpretation.departure, person );
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

	private void decide( MobsimAgent agent, Map<TripInfo, DepartureHandler> allTripInfos, Facility fromFacility, Facility toFacility ){
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement( agent );
		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;
		String mode = editPlans.getModeOfCurrentOrNextTrip( agent );
		// yyyyyy will not work when drt is access or ecress mode!
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

		// yyyy following is garbled
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

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;

		// maybe prebookedDrt and drt are two different modes?  At least there needs to be some kind of attribute.  Something like this:

		for( PlanElement pe : plan.getPlanElements() ){
			if ( pe instanceof Leg ) {
				if ( TransportMode.drt.equals(((Leg) pe).getMode()) ) {
					double prebookingOffset_s = (double) pe.getAttributes().getAttribute( "prebookingOffset_s" );
					final double prebookingTime = ((Leg) pe).getDepartureTime() - prebookingOffset_s;
					if ( prebookingTime < agent.getActivityEndTime() ) {
						// yyyy and here one sees that having this in the activity engine is not very practical
						this.wakeUpList.add( new AgentAndLegEntry( agent, prebookingTime , (Leg)pe ) ) ;
					}
				}
			}
		}


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

	/**
	 * Agents cannot be added directly to the activityEndsList since that would
	 * not be thread-safe when within-day replanning is used. There, an agent's
	 * activity end time can be modified. As a result, the agent is located at
	 * the wrong position in the activityEndsList until it is updated by using
	 * rescheduleActivityEnd(...). However, if another agent is added to the list
	 * in the mean time, it might be inserted at the wrong position.
	 * cdobler, apr'12
	 */
	static class AgentAndLegEntry {
		public AgentAndLegEntry(MobsimAgent agent, double time, Leg leg ) {
			this.agent = agent;
			this.time = time;
			this.leg = leg ;
		}
		final MobsimAgent agent;
		final double time;
		final Leg leg ;
	}


}
