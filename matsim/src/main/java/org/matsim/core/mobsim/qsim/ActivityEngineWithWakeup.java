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

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.createStageActivityType;
import static org.matsim.core.router.TripStructureUtils.Trip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.RequiresBooking;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public class ActivityEngineWithWakeup implements MobsimEngine, ActivityHandler {
	private static final Logger log = Logger.getLogger( ActivityEngine.class ) ;

	private final Map<String, TripInfo.Provider> tripInfoProviders;
	private final ActivityFacilities facilities;
	private final BookingNotificationEngine bookingNotificationEngine;

	private ActivityEngine delegate ;

	static StageActivityTypes drtStageActivities = new StageActivityTypesImpl( createStageActivityType( TransportMode.drt ) , createStageActivityType( TransportMode.walk ) ) ;

	private final Queue<AgentAndLegEntry> wakeUpList = new PriorityBlockingQueue<>(500, (o1, o2) -> {
		int cmp = Double.compare(o1.time, o2.time);
		return cmp != 0 ? cmp : o1.agent.getId().compareTo(o2.agent.getId());
	});

	@Inject
	ActivityEngineWithWakeup(EventsManager eventsManager, Map<String, TripInfo.Provider> tripInfoProviders,
			Scenario scenario, BookingNotificationEngine bookingNotificationEngine) {
		this.delegate = new ActivityEngine(eventsManager);
		this.tripInfoProviders = tripInfoProviders;
		this.facilities = scenario.getActivityFacilities();
		this.bookingNotificationEngine = bookingNotificationEngine;
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
				Plan plan = WithinDayAgentUtils.getModifiablePlan( entry.agent );

				// search for drt trip corresponding to drt leg.  Trick is using our own stage activities.
				Trip drtTrip = TripStructureUtils.findTripAtPlanElement( entry.leg, plan, this.drtStageActivities ) ;
				Gbl.assertNotNull( drtTrip );

				Facility fromFacility = FacilitiesUtils.toFacility( drtTrip.getOriginActivity(), facilities ) ;
				Facility toFacility = FacilitiesUtils.toFacility( drtTrip.getDestinationActivity(), facilities ) ;

				final TripInfoRequest request = new TripInfoRequest.Builder().setFromFacility(fromFacility)
						.setToFacility(toFacility)
						.setTime(drtTrip.getOriginActivity().getEndTime())
						.createRequest();
				Map<TripInfo, TripInfo.Provider> allTripInfos = new LinkedHashMap<>();
				for (TripInfo.Provider provider : tripInfoProviders.values()) {
					List<TripInfo> tripInfos = provider.getTripInfos(request);
					for (TripInfo tripInfo : tripInfos) {
						allTripInfos.put(tripInfo, provider);
					}
				}

				// TODO add info for mode that is in agent plan, if not returned by trip info provider

				decide( entry.agent, allTripInfos ) ;
			}
		}
		delegate.doSimStep( time );
	}

	private void decide( MobsimAgent agent, Map<TripInfo, TripInfo.Provider> allTripInfos ){

		// to get started, we assume that we are only getting one drt option back.
		// TODO: make complete
		TripInfo tripInfo = allTripInfos.keySet().iterator().next() ;

		if (tripInfo instanceof RequiresBooking) {
			((RequiresBooking)tripInfo).bookTrip(); //or: tripinfoProvider.bookTrip((RequiresBooking)tripInfo);
			//to reduce number of possibilities, I would simply assume that notification always comes later
			//
			// --> yes, with DRT it will always come in the next time step, I adapted code accordingly (michal)

			//but what to do if trip gets rejected?
			//
			// --> maybe ActivityEngineWithWakeup should only wake up agents given some conditions and then delegate
			// handling of agents to bookingNotificationEngine (or other handlers - depending on the wake-up condition)
			// Then bookingNotificationEngine should handle the whole process, including re-looping through providers in case a rejection comes
			// (michal)

			// wait for notification:
			((Activity)WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.MAX_VALUE);
			delegate.rescheduleActivityEnd(agent);
		} else {
			bookingNotificationEngine.notifyChangedTripInformation(agent, tripInfo);
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

		for( Leg drtLeg : findLegsWithModeInFuture( agent, TransportMode.drt ) ){
			double prebookingOffset_s = (double) drtLeg.getAttributes().getAttribute( "prebookingOffset_s" );
			final double prebookingTime = drtLeg.getDepartureTime() - prebookingOffset_s;
			if ( prebookingTime < agent.getActivityEndTime() ) {
				// yyyy and here one sees that having this in the activity engine is not very practical
				this.wakeUpList.add( new AgentAndLegEntry( agent, prebookingTime , drtLeg ) ) ;
			}
		}

		return delegate.handleActivity( agent ) ;
	}

	static List<Leg> findLegsWithModeInFuture( MobsimAgent agent, String mode ) {
		List<Leg> retVal = new ArrayList<>() ;
		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;
		for( int ii = WithinDayAgentUtils.getCurrentPlanElementIndex( agent ) ; ii < plan.getPlanElements().size() ; ii++ ){
			PlanElement pe = plan.getPlanElements().get( ii );;
			if ( pe instanceof Leg ) {
				if ( Objects.equals( mode, ((Leg) pe).getMode() ) ) {
					retVal.add( (Leg) pe ) ;
				}
			}
		}
		return retVal ;
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
