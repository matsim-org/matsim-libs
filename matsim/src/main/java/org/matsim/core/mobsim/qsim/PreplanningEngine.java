
/* *********************************************************************** *
 * project: org.matsim.*
 * PreplanningEngine.java
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

package org.matsim.core.mobsim.qsim;

import static java.util.Comparator.comparing;
import static org.matsim.core.config.groups.ScoringConfigGroup.createStageActivityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

public final class PreplanningEngine implements MobsimEngine {
	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	public static final String PREBOOKING_OFFSET_ATTRIBUTE_NAME = "prebookingOffset_s";

	private static final Logger log = LogManager.getLogger(PreplanningEngine.class);

	private final ActivityFacilities facilities;

	private final Map<String, TripInfo.Provider> tripInfoProviders = new LinkedHashMap<>();
	// yyyy Is "linked" enough to be deterministic? kai, mar'19

	private final Population population;
	private final Network network;
	private final Scenario scenario;

	// (we are in the mobsim, so we don't need to play around with IDs)
	private final Map<MobsimAgent, Optional<TripInfo>> tripInfoUpdatesMap = new TreeMap<>(comparing(Identifiable::getId ));
	// yyyy not sure about possible race conditions here! kai, feb'19
	// yyyyyy can't have non-sorted maps here because we will get non-deterministic results. kai, mar'19

	private final Map<MobsimAgent, TripInfo.Request> tripInfoRequestMap = new TreeMap<>(comparing(Identifiable::getId ));
	// yyyyyy can't have non-sorted maps here because we will get non-deterministic results. kai, mar'19

	private EditPlans editPlans;

	private final TripRouter tripRouter;
	private InternalInterface internalInterface;
	private final TimeInterpretation timeInterpretation;

	@Inject PreplanningEngine(TripRouter tripRouter, Scenario scenario, TimeInterpretation timeInterpretation) {
		this.tripRouter = tripRouter;
		this.population = scenario.getPopulation();
		this.facilities = scenario.getActivityFacilities();
		this.network = scenario.getNetwork();
		this.scenario = scenario;
		this.timeInterpretation = timeInterpretation;
	}

	@Override public void onPrepareSim() {
		for (DepartureHandler departureHandler : internalInterface.getDepartureHandlers()) {
			if (departureHandler instanceof TripInfo.Provider) {
				String mode = ((TripInfo.Provider)departureHandler).getMode();
				log.warn("registering TripInfo.Provider for mode=" + mode);
				this.tripInfoProviders.put(mode, (TripInfo.Provider)departureHandler);
			}
		}
	}

	@Override public void afterSim() { }

	@Override public void setInternalInterface(InternalInterface internalInterface) {
		this.editPlans = new EditPlans(internalInterface.getMobsim(), new EditTrips( tripRouter, scenario, internalInterface, timeInterpretation ) );
		this.internalInterface = internalInterface;
	}

	@Override public void doSimStep(double time) {
		//first process requests and then infos --> trips without booking required can be processed in 1 time step
		//booking confirmation always comes later (e.g. next time step)

		// (I have inlined the below methods since I find this for the time being easier to read.  Can be extracted again at some later point in time .
		// kai, jan'20)

		// process all (initial) requests:
		for (Map.Entry<MobsimAgent, TripInfo.Request> entry : tripInfoRequestMap.entrySet()) {
			final MobsimAgent mobsimAgent = entry.getKey();
			final TripInfo.Request request = entry.getValue();

			List<TripInfo> allTripInfos = new ArrayList<>();
			for (TripInfo.Provider provider : tripInfoProviders.values()) {
				allTripInfos.addAll( provider.getTripInfos( request ) );
			}

			// TODO add info for mode that is in agent plan, if not returned by trip info provider
			// not sure if that is needed. kai, jan'20

			decide( mobsimAgent, allTripInfos );
		}
		tripInfoRequestMap.clear();

		// process all updates:
		for (Map.Entry<MobsimAgent, Optional<TripInfo>> entry : tripInfoUpdatesMap.entrySet()) {
			MobsimAgent agent = entry.getKey();

			Optional<TripInfo> tripInfo = entry.getValue();

			if (tripInfo.isPresent()) {
				TripInfo actualTripInfo = tripInfo.get();
				updateAgentPlan(agent, actualTripInfo);
			} else {
				TripInfo.Request request = null;
				//TODO get it from where ??? from TripInfo???
				//TODO agent should adapt trip info request given that the previous one got rejected??
				//TODO or it should skip the rejected option during "accept()"
				notifyTripInfoNeeded(agent, request);//start over again in the next time step
			}
		}
		tripInfoUpdatesMap.clear();

		// (I have inlined the above methods since I find this for the time being easier to read.  Can be extracted again at some later point in time . kai, jan'20)
	}

	public synchronized final void notifyChangedTripInformation(MobsimAgent agent, Optional<TripInfo> tripInfoUpdate) {
		// yyyy My IDE complains about "Optional" in method signatures.  kai, jan'20

		tripInfoUpdatesMap.put(agent, tripInfoUpdate);
	}

	private synchronized void notifyTripInfoNeeded( MobsimAgent agent, TripInfo.Request tripInfoRequest ) {
		tripInfoRequestMap.put(agent, tripInfoRequest);
	}

	private void decide(MobsimAgent agent, List<TripInfo> allTripInfos) {

		// for otfvis:
		this.population.getPersons().get(agent.getId()).getAttributes().putAttribute(AgentSnapshotInfo.marker, true);

		if (allTripInfos.isEmpty()) {
			return;
		}

		// to get started, we assume that we are only getting one drt option back.
		// TODO: make complete
		TripInfo tripInfo = allTripInfos.iterator().next();

		if (tripInfo instanceof TripInfoWithRequiredBooking) {
//			tripInfoProviders.get(tripInfo.getMode())
//					.bookTrip((MobsimPassengerAgent)agent, (TripInfoWithRequiredBooking)tripInfo);
			// yyyy can't we really not use the tripInfo handle directly as I had it before?  We may, e.g., have different providers of the same mode.  kai, mar'19

			tripInfo.bookTrip( (MobsimPassengerAgent) agent );

			//to reduce number of possibilities, I would simply assume that notification always comes later
			//
			// --> yes, with DRT it will always come in the next time step, I adapted code accordingly (michal)

			// wait for notification:
			((Activity)WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.MAX_VALUE);
//			TripInfoRequestWithActivities tripInfoRequest = (TripInfoRequestWithActivities)tripInfo.getOriginalRequest();
//			tripInfoRequest.getFromActivity().setEndTime(Double.MAX_VALUE);
			// There is no guarantee that the activity that is in the tripInfoRequest is still the behavioral object of the agent.  kai, jan'20

			// one corner case is that the sim start time is set to later than some agent activity end time.  Then, depending on the order of the engines, it may happen
			// that the agent departs.  It will _then_ attempt to pre-book the trip, and up here, and then evidently cannot be cast into an activity.

			editPlans.rescheduleActivityEnd(agent);

		} else {
			notifyChangedTripInformation(agent, Optional.of(tripInfo));//no booking here
		}
		log.warn("---");
	}

	List<ActivityEngineWithWakeup.AgentEntry> generateWakeups( MobsimAgent agent, double now ) {
		if (!(agent instanceof HasModifiablePlan)) {
			// (we don't want to treat DvrpAgents, CarrierAgents, TransitVehicleDrivers etc. here)
			return Collections.emptyList();
		}


		Double prebookingOffset_s = (Double)((PlanAgent)agent).getCurrentPlan().getAttributes().getAttribute(PREBOOKING_OFFSET_ATTRIBUTE_NAME);
		// yyyy prebooking info needs to be in plan since it will not survive in the leg.  :-(  kai, jan'20

		if (prebookingOffset_s == null) {
			log.warn("The " + PREBOOKING_OFFSET_ATTRIBUTE_NAME + " is not set in the agent.  No wakeup for prebooking will be generated.");
			return Collections.emptyList();
		}

		List<ActivityEngineWithWakeup.AgentEntry> wakeups = new ArrayList<>();

		for (String mode : new String[] { TransportMode.drt, TransportMode.taxi } ) {
			// (only do the following for drt and taxi yyyy which means it may fail for, say, "drt2".  kai, apr'23)

			for (Leg drtLeg : EditPlans.findLegsWithModeInFuture(agent, mode )) {
				// (find the corresponding legs)

				final double prebookingTime = drtLeg.getDepartureTime().seconds() - prebookingOffset_s;
				if (prebookingTime < agent.getActivityEndTime()) {
					// yyyy and here one sees that having this in the activity engine is not very practical
					log.info("generating wakeup entry");
					wakeups.add(new ActivityEngineWithWakeup.AgentEntry(agent, prebookingTime, (agent1, then) -> preplanLeg(agent1, then, drtLeg )) );
				}

				Activity originActivity = EditTrips.findTripAtPlanElement(agent, drtLeg ).getOriginActivity();
				if (originActivity.getEndTime().seconds() < now + 2.) {
					originActivity.setEndTime(now + 2.);
					WithinDayAgentUtils.resetCaches(agent); // !!!!!!!!
				}

				// (we are still before "handleActivity" so we don't want to reschedule because then it will end
				// up twice in the wakeup queue)

				// yyyyyy a possibly terrible quick fix to avoid that agents depart while they are thinking about pre-booking. A
				// problem is that setting the activity end time to Double.MAX_VALUE means that the agent is forgetting the
				// originally planned activity end time.  Yes, we could leave it in the leg departure time, but this is really not
				// very self-explanatory.  kai, mar'19
				// (Technically, we could re-schedule the wakeup time but keep the planned activity end time!)

			}
		}

		return wakeups;
	}

	private void preplanLeg( MobsimAgent agent, double now, Leg leg ) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		// () search for drt trip corresponding to drt leg.  Trick is using our own stage activities (drtStageActivities).
		// () existing tests pass, but probably it is currently wrong after removing stage activity types
		// and thereby losing the ability to only consider drtStageActivities as stage activities and nothing else
		// () I think that this is now fixed.  yyyy But there should also be a test for it.  kai, jan'20
		TripStructureUtils.Trip drtTrip = TripStructureUtils.findTripAtPlanElement(leg, plan, TripStructureUtils.createStageActivityType(leg.getMode())::equals );
		Gbl.assertNotNull(drtTrip);

		final double expectedEndTimeOfOriginActivity = timeInterpretation.decideOnActivityEndTime( drtTrip.getOriginActivity(), now ).seconds();

		final TripInfo.Request request = new TripInfoRequestWithActivities.Builder(scenario)
								 .setFromActivity( drtTrip.getOriginActivity() )
								 .setToActivity(drtTrip.getDestinationActivity())
								 .setTime( expectedEndTimeOfOriginActivity )
								 .setPlannedRoute( leg.getRoute() )
								 .createRequest();

		//first simulate ActivityEngineWithWakeup and then PreplanningEngine --> decision process
		//in the same time step
		this.notifyTripInfoNeeded(agent, request);
	}

	private void updateAgentPlan(MobsimAgent agent, TripInfo tripInfo) {
		//		Gbl.assertIf(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		TripStructureUtils.Trip inputTrip = null;
		Coord pickupCoord = FacilitiesUtils.decideOnCoord(tripInfo.getPickupLocation(), network, scenario.getConfig());
		Coord dropoffCoord = FacilitiesUtils.decideOnCoord(tripInfo.getDropoffLocation(), network, scenario.getConfig());
		// TODO: check: was PreplanningEngine.drtStageActivities, so drt* interaction only?
		for (TripStructureUtils.Trip drtTrip : TripStructureUtils.getTrips(plan)) {
			// recall that we have set the activity end time of the current activity to infinity, so we cannot use that any more.  :-( ?!
			// could instead use some kind of ID.  Not sure if that would really be better.
			// So here we are looking for a trip where origin and destination are close to pickup and dropoff:
			Coord coordOrigin = PopulationUtils.decideOnCoordForActivity(drtTrip.getOriginActivity(), scenario);
			if (CoordUtils.calcEuclideanDistance(coordOrigin, pickupCoord) > 1000.) {
				continue;
			}
			Coord coordDestination = PopulationUtils.decideOnCoordForActivity(drtTrip.getDestinationActivity(), scenario);
			if (CoordUtils.calcEuclideanDistance(coordDestination, dropoffCoord) > 1000.) {
				continue;
			}
			inputTrip = drtTrip;
			break;
		}
		Gbl.assertNotNull(inputTrip);

		List<PlanElement> result = new ArrayList<>();

		inputTrip.getOriginActivity().setEndTime(tripInfo.getExpectedBoardingTime() - 900);
		// yyyy means for time being we always depart 15min before pickup.  kai, mar'19
		WithinDayAgentUtils.resetCaches(agent);

		log.warn("agentId=" + agent.getId() + " | newActEndTime=" + inputTrip.getOriginActivity()
				.getEndTime()
				.seconds());

		//		result.add( inputTrip.getOriginActivity() ) ;
		// ---

		PopulationFactory pf = population.getFactory();
		{
			Facility fromFacility = FacilitiesUtils.toFacility(inputTrip.getOriginActivity(), facilities);
			Facility toFacility = tripInfo.getPickupLocation();
			double departureTime = tripInfo.getExpectedBoardingTime() - 900.; // always depart 15min before pickup
			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.walk, fromFacility,
					toFacility, departureTime, null, inputTrip.getTripAttributes());
			// not sure if this works for walk, but it should ...

			result.addAll(planElements);
		}
		{
			Activity act = pf.createActivityFromLinkId(createStageActivityType(TransportMode.taxi),
					tripInfo.getPickupLocation().getLinkId());
			act.setMaximumDuration(0.);
			result.add(act);
		}
		{
			Leg leg = pf.createLeg(TransportMode.taxi);
			result.add(leg);
			Route route = pf.getRouteFactories()
					.createRoute(GenericRouteImpl.class, tripInfo.getPickupLocation().getLinkId(),
							tripInfo.getDropoffLocation().getLinkId());
			leg.setRoute(route);
		}
		{
			Activity act = pf.createActivityFromLinkId(createStageActivityType(TransportMode.taxi),
					tripInfo.getDropoffLocation().getLinkId());
			act.setMaximumDuration(0.);
			result.add(act);
		}
		{
			Facility fromFacility = tripInfo.getDropoffLocation();
			Facility toFacility = FacilitiesUtils.toFacility(inputTrip.getDestinationActivity(), facilities);
			double expectedTravelTime;
			try {
				expectedTravelTime = tripInfo.getExpectedTravelTime();
			} catch (Exception ee) {
				expectedTravelTime = 15.
						* 60; // using 15min as quick fix since dvrp refuses to provide this. kai, mar'19
			}
			double departureTime = tripInfo.getExpectedBoardingTime() + expectedTravelTime;
			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.walk, fromFacility,
					toFacility, departureTime, null, inputTrip.getOriginActivity().getAttributes());

			result.addAll(planElements);
		}

		//		result.add( inputTrip.getDestinationActivity() ) ;

		TripRouter.insertTrip(plan, inputTrip.getOriginActivity(), result, inputTrip.getDestinationActivity());

		editPlans.rescheduleActivityEnd(agent);
		// I don't think that this can ever do damage.

		log.warn("new plan for agentId=" + agent.getId());
		for (PlanElement planElement : plan.getPlanElements()) {
			log.warn(planElement.toString());
		}
		log.warn("---");

	}

	static String toString(TripInfo info) {
		StringBuilder strb = new StringBuilder();
		strb.append("[ ");
		strb.append("mode=").append(info.getMode());
		strb.append(" | ");
		strb.append("des/expBoardingTime=").append(info.getExpectedBoardingTime());
		//		strb.append(" | ") ;
		//		strb.append( "pickupLoc=" ).append( info.getPickupLocation() ) ;
		//		strb.append(" | ") ;
		//		strb.append( "dropoffLoc=" ).append( info.getDropoffLocation() ) ;
		strb.append(" ]");
		return strb.toString();
	}

}
