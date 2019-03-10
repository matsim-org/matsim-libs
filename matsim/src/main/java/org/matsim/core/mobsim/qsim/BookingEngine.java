package org.matsim.core.mobsim.qsim;

import static org.matsim.core.router.TripStructureUtils.Trip;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.RequiresBooking;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

final class BookingEngine implements MobsimEngine {

	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	private InternalInterface internalInterface;

	private final Map<String, TripInfo.Provider> tripInfoProviders;

	private Map<MobsimAgent, TripInfo> tripInfoMap = new ConcurrentHashMap<>();
	// yyyy not sure about possible race conditions here! kai, feb'19

	private Map<MobsimAgent, TripInfoRequest> tripInfoRequestMap = new ConcurrentHashMap<>();

	private EditTrips editTrips = null;
	private EditPlans editPlans = null;

	private final TripRouter tripRouter;

	@Inject
	BookingEngine(TripRouter tripRouter, Scenario scenario, Map<String, TripInfo.Provider> tripInfoProviders) {
		this.tripRouter = tripRouter;
		this.editTrips = new EditTrips(tripRouter, scenario);
		this.tripInfoProviders = tripInfoProviders;
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		this.editPlans = new EditPlans(internalInterface.getMobsim(), tripRouter, editTrips);
	}

	@Override
	public void doSimStep(double time) {
		//first process requests then infos --> trips without booking required can be processed in 1 time step
		processTripInfoRequests();
		processTripInfos();
	}

	public synchronized final void notifyChangedTripInformation(MobsimAgent agent, TripInfo tripinfo) {
		// (we are in the mobsim, so we don't need to play around with IDs)
		tripInfoMap.put(agent, tripinfo);
	}

	public synchronized final void notifyTripInfoRequestArrived(MobsimAgent agent, TripInfoRequest tripInfoRequest) {
		tripInfoRequestMap.put(agent, tripInfoRequest);
	}

	private void processTripInfoRequests() {
		for (Map.Entry<MobsimAgent, TripInfoRequest> entry : tripInfoRequestMap.entrySet()) {
			Map<TripInfo, TripInfo.Provider> allTripInfos = new LinkedHashMap<>();
			for (TripInfo.Provider provider : tripInfoProviders.values()) {
				List<TripInfo> tripInfos = provider.getTripInfos(entry.getValue());
				for (TripInfo tripInfo : tripInfos) {
					allTripInfos.put(tripInfo, provider);
				}
			}

			// TODO add info for mode that is in agent plan, if not returned by trip info provider

			decide(entry.getKey(), allTripInfos);
		}

		tripInfoRequestMap.clear();
	}

	private void decide(MobsimAgent agent, Map<TripInfo, TripInfo.Provider> allTripInfos) {

		// to get started, we assume that we are only getting one drt option back.
		// TODO: make complete
		TripInfo tripInfo = allTripInfos.keySet().iterator().next();

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
			editPlans.rescheduleActivityEnd(agent);
		} else {
			notifyChangedTripInformation(agent, tripInfo);//no booking here-> just plan trip
		}
	}

	private void processTripInfos() {
		for (Map.Entry<MobsimAgent, TripInfo> entry : tripInfoMap.entrySet()) {
			MobsimAgent agent = entry.getKey();
			TripInfo tripInfo = entry.getValue();

			boolean bookingRejected = false;// get it from TripInfo ???
			if (bookingRejected) {
				TripInfoRequest request = null;///get it from TripInfo ???
				//TODO agent should adapt trip info request given that the previous one got rejected??
				//TODO or it should skip the rejected option during "accept()"
				notifyTripInfoRequestArrived(agent, request);//start over again in the next time step
			} else {
				updateAgentPlan(agent, tripInfo);
			}
		}

		tripInfoMap.clear();
	}

	private void updateAgentPlan(MobsimAgent agent, TripInfo tripInfo) {
		Gbl.assertIf(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		Trip theTrip = null;
		for (Trip drtTrip : TripStructureUtils.getTrips(plan, ActivityEngineWithWakeup.drtStageActivities)) {
			// recall that we have set the activity end time of the current activity to infinity, so we cannot use that any more.  :-( ?!
			// could instead use some kind of ID.  Not sure if that would really be better.
			if (CoordUtils.calcEuclideanDistance(drtTrip.getOriginActivity().getCoord(),
					tripInfo.getPickupLocation().getCoord()) > 1000.) {
				continue;
			}
			if (CoordUtils.calcEuclideanDistance(drtTrip.getDestinationActivity().getCoord(),
					tripInfo.getDropoffLocation().getCoord()) > 1000.) {
				continue;
			}
			theTrip = drtTrip;
			break;
		}
		Gbl.assertNotNull(theTrip);
		Iterator<Trip> walkTripsIter = TripStructureUtils.getTrips(theTrip.getTripElements(),
				new StageActivityTypesImpl(TransportMode.walk)).iterator();

		// ---

		Gbl.assertIf(walkTripsIter.hasNext());
		Trip accessWalkTrip = walkTripsIter.next();
		accessWalkTrip.getDestinationActivity().setCoord(tripInfo.getPickupLocation().getCoord());
		accessWalkTrip.getDestinationActivity().setLinkId(tripInfo.getPickupLocation().getLinkId());
		accessWalkTrip.getDestinationActivity().setFacilityId(null);

		List<? extends PlanElement> pe = editTrips.replanFutureTrip(accessWalkTrip, plan, TransportMode.walk);
		List<Leg> legs = TripStructureUtils.getLegs(pe);
		double ttime = 0.;
		for (Leg leg : legs) {
			if (leg.getRoute() != null) {
				ttime += leg.getTravelTime();
			} else {
				ttime += leg.getTravelTime();
			}
		}
		double buffer = 300.;
		editPlans.rescheduleCurrentActivityEndtime(agent, tripInfo.getExpectedBoardingTime() - ttime - buffer);

		// ---

		Gbl.assertIf(walkTripsIter.hasNext());
		Trip egressWalkTrip = walkTripsIter.next();
		final Activity egressWalkOriginActivity = egressWalkTrip.getOriginActivity();
		egressWalkOriginActivity.setCoord(tripInfo.getDropoffLocation().getCoord());
		egressWalkOriginActivity.setLinkId(tripInfo.getDropoffLocation().getLinkId());
		egressWalkOriginActivity.setFacilityId(null);

		editTrips.replanFutureTrip(egressWalkTrip, plan, TransportMode.walk);
		// yy maybe better do this at dropoff?

		// ----

		Gbl.assertIf(!walkTripsIter.hasNext());
	}
}
