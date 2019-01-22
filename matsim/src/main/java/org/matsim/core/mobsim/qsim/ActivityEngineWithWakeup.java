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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntry;
import org.matsim.core.mobsim.qsim.ActivityEngine.AgentEntryComparator;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static org.matsim.core.mobsim.qsim.interfaces.TripInfoProvider.*;

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
				     TripRouter tripRouter, Scenario scenario, QSim qsim, Config config ) {
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
				Plan plan;
				// option 1 - better, because creates a copy 
				plan = WithinDayAgentUtils.getModifiablePlan(agent);
				
				// option 2
				if (agent instanceof PlanAgent) {
					plan = ((PlanAgent) agent).getCurrentPlan();
				}
				
				// TODO: later write ExperiencedPlans (in PlansCalcScoreConfigGroup) as debug output
				
				// TODO: somehow find out for which activity the agent was woken up
				// quick and dirty temporary solution:
				int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
				if ( !(plan.getPlanElements().get(currentIndex) instanceof Activity) || 
						! (plan.getPlanElements().get(currentIndex + 2) instanceof Activity) ) {
					currentIndex++;
				}
				Facility fromFacility = toFacility((Activity) plan.getPlanElements().get(currentIndex));
				Facility toFacility = toFacility((Activity) plan.getPlanElements().get(currentIndex + 2));
				
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
				decide( agent, allTripInfos, editTrips, editPlans, fromFacility, toFacility) ;
			}
		}
		delegate.doSimStep( time );
	}
	
	private Facility toFacility(final Activity act) {
		// use facility first if available i.e. reversing the logic above Amit July'18
		if (	facilities != null &&
				! facilities.getFacilities().isEmpty() &&
				act.getFacilityId() != null ) {
			return facilities.getFacilities().get( act.getFacilityId() );
		}

		return new ActivityWrapperFacility( act );
	}

	private void decide( MobsimAgent agent, Map<TripInfo,DepartureHandler> allTripInfos, EditTrips editTrips, EditPlans editPlans, Facility fromFacility,
				   Facility toFacility ){
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

		TripStructureUtils.Trip trip = editTrips.findTripAfterActivity( plan, currentActivity ) ;

		List<PlanElement> newTrip = new ArrayList<>(  ) ;
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		
        double pickupBeelineDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), tripInfo.getPickupLocation().getCoord());
        double pickupWalkTime = pickupBeelineDistance / beelineWalkSpeed;

		double buffer = 60. ;
		{
			Leg leg = pf.createLeg( TransportMode.access_walk );
			// also add generic route
			leg.setTravelTime( pickupWalkTime );
			newTrip.add( leg ) ;
		}
		{
			Activity activity = pf.createActivityFromCoord( "drt_interaction", tripInfo.getPickupLocation().getCoord() ) ;
//			activity.setFacilityId( tripinfo.getPickupLocation().getId() ); // how is this solved in transit router? -- TransitRouterWrapper.fillWithActivities() -- no facility id is set !
			activity.setLinkId(tripInfo.getPickupLocation().getLinkId());
			activity.setCoord(tripInfo.getPickupLocation().getCoord());
			newTrip.add( activity ) ;
		}
		{
			Leg leg = pf.createLeg( TransportMode.drt ) ;
			// also add generic route
			newTrip.add( leg ) ;
		}
		{
			Activity activity = pf.createActivityFromCoord( "drt_interaction", tripInfo.getDropoffLocation().getCoord() ) ;
//			activity.setFacilityId( tripinfo.getDropoffLocation().getId() ); // how is this solved in transit router? -- no facility id is set !
			activity.setLinkId(tripInfo.getDropoffLocation().getLinkId());
			activity.setCoord(tripInfo.getDropoffLocation().getCoord());
			newTrip.add( activity ) ;
		}
		{
			Leg leg = pf.createLeg( TransportMode.egress_walk ) ;
			// also add generic route
	        double dropoffBeelineDistance = CoordUtils.calcEuclideanDistance(tripInfo.getDropoffLocation().getCoord(), toFacility.getCoord());
	        double dropoffWalkTime = dropoffBeelineDistance / beelineWalkSpeed;
	        leg.setTravelTime(dropoffWalkTime);
			newTrip.add( leg ) ;
		}
		TripRouter.insertTrip( plan, currentActivity, newTrip, trip.getDestinationActivity() ) ;

		Integer index = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );;
		editPlans.rescheduleActivityEndtime( agent, index, tripInfo.getExpectedBoardingTime() - pickupWalkTime - buffer );

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

	private class MyEventHandler implements BasicEventHandler {
		@Override
		public void handleEvent( Event event ){
			if ( event instanceof PersonInformationEvent ) {
				delegate.
			}
		}
	}

}
