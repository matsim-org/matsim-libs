package org.matsim.contrib.dvrp.passenger;

import static org.matsim.core.router.TripStructureUtils.Trip;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

public final class BookingEngine implements MobsimEngine {

	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	private final Map<String, TripInfo.Provider> tripInfoProviders;
	private final Map<DvrpMode, PassengerEngine> passengerEngines;

	private Map<MobsimAgent, Optional<TripInfo>> tripInfoUpdatesMap = new ConcurrentHashMap<>();
	// yyyy not sure about possible race conditions here! kai, feb'19

	private Map<MobsimAgent, TripInfoRequest> tripInfoRequestMap = new ConcurrentHashMap<>();

	private final EditTrips editTrips;
	private EditPlans editPlans = null;

	private final TripRouter tripRouter;

	@Inject
	BookingEngine(TripRouter tripRouter, Scenario scenario, Map<String, TripInfo.Provider> tripInfoProviders,
			Map<DvrpMode, PassengerEngine> passengerEngines) {
		this.tripRouter = tripRouter;
		this.editTrips = new EditTrips(tripRouter, scenario);
		this.tripInfoProviders = tripInfoProviders;
		this.passengerEngines = passengerEngines;
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.editPlans = new EditPlans(internalInterface.getMobsim(), tripRouter, editTrips);
		//		PassengerEngine pEngine = internalInterface.getMobsim().getChildInjector().getInstance( PassengerEngine.class );
		//		this.tripInfoProviders.put( TransportMode.drt, pEngine ) ;
		//		this.tripInfoProviders.put( TransportMode.taxi, pEngine ) ;
	}

	@Override
	public void doSimStep(double time) {
		//first process requests  and then infos --> trips without booking required can be processed in 1 time step
		//booking confirmation always comes later (e.g. next time step)
		processTripInfoRequests();
		processTripInfoUpdates();
	}

	public synchronized final void notifyChangedTripInformation(MobsimAgent agent, Optional<TripInfo> tripInfoUpdate) {
		// (we are in the mobsim, so we don't need to play around with IDs)
		tripInfoUpdatesMap.put(agent, tripInfoUpdate);
	}

	public synchronized final void notifyTripInfoRequestSent(MobsimAgent agent, TripInfoRequest tripInfoRequest) {
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

		if (tripInfo instanceof TripInfoWithRequiredBooking) {
			passengerEngines.get(DvrpModes.mode(tripInfo.getMode()))
					.bookTrip((MobsimPassengerAgent)agent, (TripInfoWithRequiredBooking)tripInfo);
			//to reduce number of possibilities, I would simply assume that notification always comes later
			//
			// --> yes, with DRT it will always come in the next time step, I adapted code accordingly (michal)

			// wait for notification:
			((Activity)WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.MAX_VALUE);
			editPlans.rescheduleActivityEnd(agent);
		} else {
			notifyChangedTripInformation(agent, Optional.of(tripInfo));//no booking here
		}
	}

	private void processTripInfoUpdates() {
		for (Map.Entry<MobsimAgent, Optional<TripInfo>> entry : tripInfoUpdatesMap.entrySet()) {
			MobsimAgent agent = entry.getKey();
			Optional<TripInfo> tripInfo = entry.getValue();

			if (tripInfo.isPresent()) {
				updateAgentPlan(agent, tripInfo.get());
			} else {
				TripInfoRequest request = null;
				//TODO get it from where ??? from TripInfo???
				//TODO agent should adapt trip info request given that the previous one got rejected??
				//TODO or it should skip the rejected option during "accept()"
				notifyTripInfoRequestSent(agent, request);//start over again in the next time step
			}
		}

		tripInfoUpdatesMap.clear();
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
