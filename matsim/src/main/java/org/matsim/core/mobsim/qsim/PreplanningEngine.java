package org.matsim.core.mobsim.qsim;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.withinday.utils.EditPlans;
import org.matsim.withinday.utils.EditTrips;

import com.google.inject.Inject;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.createStageActivityType;

public final class PreplanningEngine implements MobsimEngine {
	// Could implement this as a generalized version of the bdi-abm implementation: can send notifications to agent, and agent can react.  Similar to the drive-to action.
	// Notifications and corresponding handlers could then be registered. On the other hand, it is easy to add an engine such as this one; how much does it help to have another
	// layer of infrastructure?  Am currently leaning towards the second argument.  kai, mar'19

	private static final Logger log = Logger.getLogger( PreplanningEngine.class );

	public static final StageActivityTypes drtStageActivities = new StageActivityTypesImpl(
		  createStageActivityType(TransportMode.drt), createStageActivityType(TransportMode.walk));

	private final ActivityFacilities facilities;

	private final Map<String, TripInfo.Provider> tripInfoProviders = new LinkedHashMap<>();
	// yyyy Is "linked" enough to be deterministic? kai, mar'19

	private final Population population;
	private final Network network;
	private final Scenario scenario;

	private Map<MobsimAgent, Optional<TripInfo>> tripInfoUpdatesMap = new TreeMap<>( ( o1, o2 ) -> o1.getId().compareTo( o2.getId() ) ) ;
	// yyyy not sure about possible race conditions here! kai, feb'19
	// yyyyyy can't have non-sorted maps here because we will get non-deterministic results. kai, mar'19

	private Map<MobsimAgent, TripInfoRequest> tripInfoRequestMap = new TreeMap<>( ( o1, o2 ) -> o1.getId().compareTo( o2.getId() ) ) ;
	// yyyyyy can't have non-sorted maps here because we will get non-deterministic results. kai, mar'19

	private final EditTrips editTrips;
	private EditPlans editPlans = null;

	private final TripRouter tripRouter;
	private InternalInterface internalInterface;

	@Inject
	PreplanningEngine( TripRouter tripRouter, Scenario scenario ) {
		this.tripRouter = tripRouter;
		this.editTrips = new EditTrips(tripRouter, scenario);
		this.population = scenario.getPopulation();
		this.facilities = scenario.getActivityFacilities() ;
		this.network = scenario.getNetwork() ;
		this.scenario = scenario ;
	}

	@Override
	public void onPrepareSim() {
		for (DepartureHandler departureHandler : internalInterface.getDepartureHandlers()) {
			if (departureHandler instanceof TripInfo.Provider) {
				String mode = ((TripInfo.Provider)departureHandler).getMode();
				log.warn("registering TripInfo.Provider for mode=" + mode ) ;
				this.tripInfoProviders.put(mode, (TripInfo.Provider)departureHandler);
			}
		}
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.editPlans = new EditPlans(internalInterface.getMobsim(), tripRouter, editTrips);
		this.internalInterface = internalInterface ;
	}

	@Override
	public void doSimStep(double time) {
		//first process requests  and then infos --> trips without booking required can be processed in 1 time step
		//booking confirmation always comes later (e.g. next time step)
		processTripInfoRequests();
		processTripInfoUpdates();
	}

	public synchronized final void notifyChangedTripInformation( MobsimAgent agent, Optional<TripInfo> tripInfoUpdate ) {
		// needs to be public because this is what the Drt Passenger Engine uses

		// (we are in the mobsim, so we don't need to play around with IDs)
		tripInfoUpdatesMap.put(agent, tripInfoUpdate);
	}

	synchronized final void notifyTripInfoNeeded( MobsimAgent agent, TripInfoRequest tripInfoRequest ) {
//		log.info("entering notifyTripInfoRequestSent with agentId=" + agent.getId());
		tripInfoRequestMap.put(agent, tripInfoRequest);
	}

	private void processTripInfoRequests() {
		for (Map.Entry<MobsimAgent, TripInfoRequest> entry : tripInfoRequestMap.entrySet()) {
			log.warn("processing tripInfoRequests for agentId=" + entry.getKey().getId());
			Map<TripInfo, TripInfo.Provider> allTripInfos = new LinkedHashMap<>();
			for (TripInfo.Provider provider : tripInfoProviders.values()) {
				log.warn("querying provider of " + provider.getMode() + " for tripInfo") ;
				List<TripInfo> tripInfos = provider.getTripInfos(entry.getValue());
				for (TripInfo tripInfo : tripInfos) {
					log.warn("tripInfo=" + toString( tripInfo ) ) ;
					allTripInfos.put(tripInfo, provider);
				}
			}

			// TODO add info for mode that is in agent plan, if not returned by trip info provider

			decide(entry.getKey(), allTripInfos);
		}

		tripInfoRequestMap.clear();
	}

	private void decide(MobsimAgent agent, Map<TripInfo, TripInfo.Provider> allTripInfos) {
		log.warn("entering decide for agentId=" + agent.getId());

		this.population.getPersons().get(agent.getId()).getAttributes().putAttribute(AgentSnapshotInfo.marker, true);

		if (allTripInfos.isEmpty()) {
			return;
		}

		// to get started, we assume that we are only getting one drt option back.
		// TODO: make complete
		TripInfo tripInfo = allTripInfos.keySet().iterator().next();

		if (tripInfo instanceof TripInfoWithRequiredBooking) {
			log.warn("about to book trip with tripInfo=" + toString( tripInfo ));
			tripInfoProviders.get(tripInfo.getMode()).bookTrip((MobsimPassengerAgent)agent, (TripInfoWithRequiredBooking)tripInfo);
			// yyyy can't we really not use the tripInfo handle directly as I had it before?  We may, e.g., have different providers of the same mode.  kai, mar'19

			//to reduce number of possibilities, I would simply assume that notification always comes later
			//
			// --> yes, with DRT it will always come in the next time step, I adapted code accordingly (michal)

			// wait for notification:
			((Activity)WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.MAX_VALUE);

			// one corner case is that the sim start time is set to later than some agent activity end time.  Then, depending on the order of the engines, it may happen
			// that the agent departs.  It will _then_ attempt to pre-book the trip, and up here, and then evidently cannot be cast into an activity.

			editPlans.rescheduleActivityEnd(agent);

		} else {
			notifyChangedTripInformation(agent, Optional.of(tripInfo));//no booking here
		}
		log.warn("---") ;
	}

	private void processTripInfoUpdates() {
		for (Map.Entry<MobsimAgent, Optional<TripInfo>> entry : tripInfoUpdatesMap.entrySet()) {
			MobsimAgent agent = entry.getKey();
			Optional<TripInfo> tripInfo = entry.getValue();

			if (tripInfo.isPresent()) {
				TripInfo actualTripInfo = tripInfo.get();
				updateAgentPlan(agent, actualTripInfo);
			} else {
				TripInfoRequest request = null;
				//TODO get it from where ??? from TripInfo???
				//TODO agent should adapt trip info request given that the previous one got rejected??
				//TODO or it should skip the rejected option during "accept()"
				notifyTripInfoNeeded(agent, request );//start over again in the next time step
			}
		}

		tripInfoUpdatesMap.clear();
	}

	public List<ActivityEngineWithWakeup.AgentEntry> generateWakeups( MobsimAgent agent, double now ) {
		if (!(agent instanceof HasModifiablePlan)) {
			// (we don't want to treat DvrpAgents, CarrierAgents, TransitVehicleDrivers etc. here)
			return Collections.emptyList();
		}

		List<ActivityEngineWithWakeup.AgentEntry> wakeups = new ArrayList<>() ;

		Double prebookingOffset_s = (Double)((PlanAgent)agent).getCurrentPlan()
											.getAttributes()
											.getAttribute( ActivityEngineWithWakeup.PREBOOKING_OFFSET_ATTRIBUTE_NAME );

		if ( prebookingOffset_s == null ) {
			log.warn("not prebooking") ;
			return wakeups ;
		}

		for( String mode : new String[]{TransportMode.drt, TransportMode.taxi} ){
			for ( Leg drtLeg : findLegsWithModeInFuture(agent, mode)) {
				//				Double prebookingOffset_s = (Double)drtLeg.getAttributes().getAttribute( PREBOOKING_OFFSET_ATTRIBUTE_NAME );
				// yyyyyy the info will not survive in the leg!
				//			if (prebookingOffset_s == null) {
				//				log.warn("not prebooking");
				//				continue;
				//			}
				final double prebookingTime = drtLeg.getDepartureTime() - prebookingOffset_s;
				if (prebookingTime < agent.getActivityEndTime()){
					// yyyy and here one sees that having this in the activity engine is not very practical
					log.info( "adding agent to wakeup list" );
					wakeups.add( new ActivityEngineWithWakeup.AgentEntry( agent, prebookingTime, ( agent1, then ) -> wakeUpAgent( agent1, then, drtLeg ) ) ) ;
				}

				Activity originActivity = editTrips.findTripAtPlanElement( agent, drtLeg ).getOriginActivity() ;
				PlanElement currentPe = WithinDayAgentUtils.getCurrentPlanElement( agent );

				if ( currentPe == originActivity ){
					// I think that "==" is ok here, since we really want to know if this is the same object.  kai, mar'19

					if ( originActivity.getEndTime() <= now ){
//						int index = WithinDayAgentUtils.indexOfPlanElement( agent, originActivity );;
//						this.editPlans.rescheduleActivityEndtime( agent, index, now+1 );
						log.warn("currentEndTimeBefore=" + originActivity.getEndTime() ) ;
						log.warn("agentEndTimeBefore=" + agent.getActivityEndTime() ) ;
						originActivity.setEndTime( now+2 );
						WithinDayAgentUtils.resetCaches( agent );
						log.warn("currentEndTimeAfter=" + originActivity.getEndTime() ) ;
						log.warn("agentEndTimeAfter=" + agent.getActivityEndTime() ) ;
						// (we are still before "handleActivity" so we don't want to reschedule because then it will end
						// up twice in the wakeup queue)
					}
					// yyyyyy a possibly terrible quick fix to avoid that agents depart while they are thinking about pre-booking. A
					// problem is that setting the activity end time to Double.MAX_VALUE means that the agent is forgetting the
					// originally planned activity end time.  Yes, we could leave it in the leg departure time, but this is really not
					// very self-explanatory.  kai, mar'19
					// (Technically, we could re-schedule the wakeup time but keep the planned activity end time!)
				}

			}
		}

		return wakeups;
	}

	public static List<Leg> findLegsWithModeInFuture(MobsimAgent agent, String mode) {
		List<Leg> retVal = new ArrayList<>();
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent );
		for (int ii = WithinDayAgentUtils.getCurrentPlanElementIndex(agent); ii < plan.getPlanElements().size(); ii++) {
			PlanElement pe = plan.getPlanElements().get(ii);
			if (pe instanceof Leg) {
				if (Objects.equals(mode, ((Leg)pe).getMode())) {
					retVal.add((Leg)pe);
				}
			}
		}
		return retVal;
	}

	private void wakeUpAgent(MobsimAgent agent, double now, Leg leg) {
		//		log.warn("entering wakeUpAgent with agentId=" + agent.getId() ) ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		// search for drt trip corresponding to drt leg.  Trick is using our own stage activities.
		TripStructureUtils.Trip drtTrip = TripStructureUtils.findTripAtPlanElement(leg, plan, drtStageActivities );
		Gbl.assertNotNull(drtTrip );

		Facility fromFacility = FacilitiesUtils.toFacility(drtTrip.getOriginActivity(), facilities );
		Facility toFacility = FacilitiesUtils.toFacility(drtTrip.getDestinationActivity(), facilities);

		final TripInfoRequest request = new TripInfoRequest.Builder().setFromFacility(fromFacility)
												 .setToFacility(toFacility)
												 .setTime(drtTrip.getOriginActivity().getEndTime())
												 .createRequest();

		//first simulate ActivityEngineWithWakeup and then BookingEngine --> decision process
		//in the same time step
		this.notifyTripInfoNeeded(agent, request );
	}


	private void updateAgentPlan(MobsimAgent agent, TripInfo tripInfo) {
		Gbl.assertIf(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity);

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		TripStructureUtils.Trip theTrip = null;
		Coord pickupCoord = tripInfo.getPickupLocation().getCoord();
		if ( pickupCoord==null ) {
			Link link = this.network.getLinks().get( tripInfo.getPickupLocation().getLinkId() ) ;
			Gbl.assertNotNull( link );
			pickupCoord = link.getCoord() ;
		}
		Coord dropoffCoord = tripInfo.getDropoffLocation().getCoord();
		if ( dropoffCoord==null ) {
			Link link = this.network.getLinks().get( tripInfo.getDropoffLocation().getLinkId() ) ;
			Gbl.assertNotNull( link );
			dropoffCoord = link.getCoord() ;
		}
		for (TripStructureUtils.Trip drtTrip : TripStructureUtils.getTrips(plan, PreplanningEngine.drtStageActivities )) {
			// recall that we have set the activity end time of the current activity to infinity, so we cannot use that any more.  :-( ?!
			// could instead use some kind of ID.  Not sure if that would really be better.
			Coord coordOrigin = PopulationUtils.decideOnCoordForActivity( drtTrip.getOriginActivity(), scenario ) ;
			if ( CoordUtils.calcEuclideanDistance( coordOrigin, pickupCoord ) > 1000.) {
				continue;
			}
			Coord coordDestination = PopulationUtils.decideOnCoordForActivity( drtTrip.getDestinationActivity(), scenario ) ;
			if (CoordUtils.calcEuclideanDistance( coordDestination, dropoffCoord ) > 1000.) {
				continue;
			}
			theTrip = drtTrip;
			break;
		}
		Gbl.assertNotNull(theTrip);
		Iterator<TripStructureUtils.Trip> walkTripsIter = TripStructureUtils.getTrips(theTrip.getTripElements(),
			  new StageActivityTypesImpl(TransportMode.walk)).iterator();

		// ---

		Gbl.assertIf(walkTripsIter.hasNext());
		TripStructureUtils.Trip accessWalkTrip = walkTripsIter.next();
		accessWalkTrip.getDestinationActivity().setCoord( pickupCoord );
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
		TripStructureUtils.Trip egressWalkTrip = walkTripsIter.next();
		final Activity egressWalkOriginActivity = egressWalkTrip.getOriginActivity();
		egressWalkOriginActivity.setCoord( dropoffCoord );
		egressWalkOriginActivity.setLinkId(tripInfo.getDropoffLocation().getLinkId());
		egressWalkOriginActivity.setFacilityId(null);

		log.warn("---") ;

		for( PlanElement planElement : plan.getPlanElements() ){
			log.warn( pe.toString() ) ;
		}

		editTrips.replanFutureTrip(egressWalkTrip, plan, TransportMode.walk);
		// yy maybe better do this at dropoff?

		for( PlanElement planElement : plan.getPlanElements() ){
			log.warn( pe.toString() ) ;
		}

		log.warn("---") ;

		// ----

		Gbl.assertIf(!walkTripsIter.hasNext());
	}


	static String toString( TripInfo info ) {
		StringBuilder strb = new StringBuilder(  ) ;
		strb.append("[ ") ;
		strb.append( "mode=" ).append( info.getMode() ) ;
		strb.append(" | ") ;
		strb.append( "des/expBoardingTime=").append( info.getExpectedBoardingTime() ) ;
//		strb.append(" | ") ;
//		strb.append( "pickupLoc=" ).append( info.getPickupLocation() ) ;
//		strb.append(" | ") ;
//		strb.append( "dropoffLoc=" ).append( info.getDropoffLocation() ) ;
		strb.append( " ]" ) ;
		return strb.toString() ;
	}


}
