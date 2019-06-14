/**
 * 
 */
package org.matsim.withinday.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.withinday.events.ReplanningEvent;

/**
 * The methods here should modify trips, i.e. material between two non-stage-activities.
 * 
 * @author kainagel
 */
public final class EditTrips {
	private static final Logger log = Logger.getLogger(EditTrips.class) ;

	private final TripRouter tripRouter;
	private final PopulationFactory pf;
	private Scenario scenario;
	private TransitStopAgentTracker transitAgentTracker;
	private EventsManager eventsManager;

	public EditTrips( QSim qsim, TripRouter tripRouter, Scenario scenario ) {
		log.setLevel( Level.DEBUG);
		this.tripRouter = tripRouter;
		this.scenario = scenario;
		this.pf = scenario.getPopulation().getFactory() ;
		this.eventsManager = qsim.getEventsManager();
		
		for (AgentTracker tracker : (qsim.getAgentTrackers())) {
			if (tracker instanceof TransitStopAgentTracker) {
				transitAgentTracker = (TransitStopAgentTracker) tracker;
				break;
			}
		}
		if (transitAgentTracker == null) {
			throw new RuntimeException("no TransitStopAgentTracker found in qsim");
		}
		
	}
	public final Trip findCurrentTrip( MobsimAgent agent ) {
		PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		return findTripAtPlanElement(agent, pe);
	}
	public Trip findTripAtPlanElement(MobsimAgent agent, PlanElement pe) {
//		log.debug("plan element to be found=" + pe ) ;
		List<Trip> trips = TripStructureUtils.getTrips( WithinDayAgentUtils.getModifiablePlan(agent), tripRouter.getStageActivityTypes() ) ;
		for ( Trip trip : trips ) {
			for ( PlanElement te : trip.getTripElements() ) {
//				log.debug("trip element to be compared with=" + te ) ;
				if ( te==pe ) {
//					log.debug("found trip element") ;
					return trip;
				}
			}
		}
		throw new ReplanningException("trip not found") ;
	}
	public Trip findTripAtPlanElementIndex( MobsimAgent agent, int index ) {
		return findTripAtPlanElement( agent, WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(index) ) ;
	}
	// current trip:
	public final boolean replanCurrentTrip(MobsimAgent agent, double now, String routingMode ) {
		log.debug("entering replanCurrentTrip with routingMode=" + routingMode) ;

		// I will treat that in the way that it will make the trip consistent with the activities.  So if the activity in the
		// plan has changed, the trip will go to a new location.
		
		// what matters is the external interface, which is the method call.  Everything below is internal so it does not matter so much.

		Trip trip = findCurrentTrip( agent ) ;
		final PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		final List<PlanElement> tripElements = trip.getTripElements();
		int tripElementsIndex = tripElements.indexOf( currentPlanElement ) ;
//		final String currentMode = tripRouter.getMainModeIdentifier().identifyMainMode( tripElements ) ;

		if ( currentPlanElement instanceof Activity ) {
			// we are on a stage activity.  Take it from there:
			// TODO: this method fails with exception 
			replanCurrentTripFromStageActivity(trip, tripElementsIndex, routingMode, now, agent);
		} else {
			// we are on a leg
			replanCurrentTripFromLeg(trip.getDestinationActivity(), currentPlanElement, routingMode, now, agent);
		}
		
		if (eventsManager != null) {
			ReplanningEvent replanningEvent = new ReplanningEvent(now, agent.getId(), 
					"EditTrips.replanCurrentTrip");
			eventsManager.processEvent(replanningEvent);
		}
		WithinDayAgentUtils.resetCaches(agent);
		return true ;
	}
	private void replanCurrentTripFromLeg(Activity newAct, final PlanElement currentPlanElement, final String routingMode,
										  double now, MobsimAgent agent) {
		log.debug("entering replanCurrentTripFromLeg for agent" + agent.getId());
		Leg currentLeg = (Leg) currentPlanElement ;
		if ( currentLeg.getRoute() instanceof NetworkRoute ) {
			replanCurrentLegWithNetworkRoute(newAct, routingMode, currentLeg, now, agent);
		} else if ( currentLeg.getRoute() instanceof ExperimentalTransitRoute ) {
			// public transit leg
			
			replanCurrentLegWithTransitRoute(newAct, routingMode, currentLeg, now, agent);
			
			// TODO: replace the short-cut solution below after fixing replanCurrentLegWithTransitRoute
//			replanCurrentLegWithGenericRoute(newAct, routingMode, currentLeg, now, agent);
		} else if ( currentLeg.getRoute() instanceof GenericRouteImpl ) {
			// teleported leg
//			replanCurrentLegWithGenericRoute(newAct, routingMode, currentLeg, now, agent);
		} else {
			throw new ReplanningException("not implemented for the route type of the current leg") ;
			// Does not feel so hard: 
			// * with teleported legs, push forward to next facility
			// * with passenger legs, push forward to next possibility of exit
		}

		WithinDayAgentUtils.resetCaches(agent);
	}
	
	private void replanCurrentLegWithNetworkRoute(Activity newAct, String mainMode, Leg currentLeg, double now, 
			MobsimAgent agent) {
		log.debug("entering replanCurrentLegWithNetworkRoute for agent" + agent.getId()) ;
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		Person person = plan.getPerson() ;
		
		// (1) get new trip from current position to new activity:
		Link currentLink = scenario.getNetwork().getLinks().get( agent.getCurrentLinkId() ) ;
		Facility currentLocationFacility = new LinkWrapperFacility( currentLink ) ;
		List<? extends PlanElement> newTripElements = newTripToNewActivity(currentLocationFacility, newAct, mainMode,
				now, agent, person, scenario);

		// (2) prune the new trip up to the current leg:
		pruneUpToCurrentLeg(currentLeg, newTripElements);

		// (2) modify current route:
		replaceRemainderOfCurrentRoute(currentLeg, newTripElements, agent);

		// (3) remove remainder of old trip after current leg in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;

		while ( !planElements.get(pos).equals(newAct) ) {
			planElements.remove(pos) ;
		}

		// (4) insert new trip after current leg:
		for ( int ijk = 1 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos, newTripElements.get(ijk) ) ;
		}
		WithinDayAgentUtils.resetCaches(agent);
	}
	
	private void replanCurrentLegWithTransitRoute(Activity newAct, String routingMode, Leg currentLeg, double now,
			MobsimAgent agent) {
		log.debug("entering replanCurrentLegWithTransitRoute for agent" + agent.getId()) ;
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		Person person = plan.getPerson() ;
		
		// find current (if transit vehicle is at a stop) or next stop
		
		if (! (agent instanceof PTPassengerAgent) ) {
			throw new RuntimeException("transit leg, but agent is not an PTPassengerAgent: not implemented! agent id: " + agent.getId());
		}
		PTPassengerAgent ptPassengerAgent = (PTPassengerAgent) agent;
		
		MobsimVehicle mobsimVehicle = ptPassengerAgent.getVehicle();
		ExperimentalTransitRoute oldPtRoute = (ExperimentalTransitRoute) currentLeg.getRoute();

		/*
		 * In AbstractTransitDriverAgent nextStop is moved forward only at departure, so
		 * while the vehicle stops "nextStop" is the stop where the vehicle stops at.
		 * (see AbstractTransitDriverAgent.depart() and
		 * AbstractTransitDriverAgent.processEventVehicleArrives())
		 * 
		 * So if the vehicle stops at a stop facility, driver.getNextTransitStop() will
		 * return that stop facility and we can replan the agent from that stop facility.
		 * gl may'19
		 */
		
		/*
		 * TODO: Routing with the current vehicle as a start: If the agent is currently
		 * on a delayed bus, the router won't find that bus when looking for shortest
		 * paths from the next stop, because it assumes the bus has already departed. It
		 * will suggest to take another later bus even if staying on the current
		 * (delayed) bus is better. A real-time schedule based router would solve this
		 * apart from transfer times and disutilities (it won't take into account that
		 * staying on the same bus is one transfer less than getting off at the next
		 * stop and taking another bus from there). Otherwise one might route from all
		 * following stop facilities the delayed bus will still serve (high computation
		 * time, maybe less so with some special kind of many-to-one tree) or move only
		 * the corresponding departure in the schedule (high computation time for
		 * preparing the Raptor router's internal data structures, probably worst
		 * option). Not solving this will likely lead to change of routes without a
		 * proper reason / some kind of oscillation between two best or at least
		 * similarly good paths.
		 * 
		 * Idea 1: Use scheduled departure time instead of real current time to keep
		 * (delayed) bus the passenger is sitting on available from the router's
		 * perspective. Idea 2: Use input schedule file which has real (delayed)
		 * departure times as far as known at the time of withinday-replanning.
		 * 
		 * gl may'19
		 */

		boolean wantsToLeaveStop = false ;
		
		TransitStopFacility currentOrNextStop = null;
		List<? extends PlanElement> newTripElements = null;
		// (1) get new trip from current position to new activity:
		if (mobsimVehicle == null) {
			// agent is waiting for the next vehicle to arrive at the access stop
			currentOrNextStop = scenario.getTransitSchedule().getFacilities().get(oldPtRoute.getAccessStopId());
			newTripElements = newTripToNewActivity(currentOrNextStop, newAct, routingMode, now, agent, person, scenario);
			if (newTripElements.get(0) instanceof Leg
					&& ((Leg) newTripElements.get(0)).getRoute() instanceof ExperimentalTransitRoute
					&& ((ExperimentalTransitRoute) ((Leg) newTripElements.get(0)).getRoute()).getAccessStopId()
							.equals(currentOrNextStop.getId())) {
				// The agent will use a transit line departing from the same stop facility,
				// don't remove the agent from the stop tracker
			} else {
				// The agent will not board any bus at the transit stop and will walk away or
				// do something else. We have to remove him from the list of waiting agents.
				wantsToLeaveStop = true ;
			}
		} else {
			// agent is on a vehicle
			// agent is asked whether she intends to leave at each stop, so no need to tell
			// mobsim about changed plan in any particular way
			TransitDriverAgent driver = (TransitDriverAgent) mobsimVehicle.getDriver();
			currentOrNextStop = driver.getNextTransitStop();
			newTripElements = newTripToNewActivity(currentOrNextStop, newAct, routingMode, now, agent, person, scenario);
		}

		//??????????
		// (2) prune the new trip up to the current leg: -> for pt better as one step together with mergeOldAndNewCurrentPtLeg
//		pruneUpToCurrentLeg(currentLeg, newTripElements);

		// (2) prune the new trip up to the current leg and modify current route, return additional PlanElements if necessary for merging:
		newTripElements = mergeOldAndNewCurrentPtLeg(currentLeg, newTripElements, agent, currentOrNextStop);

		// (3) remove remainder of old trip after current leg in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;

		while ( !planElements.get(pos).equals(newAct) ) {
			planElements.remove(pos) ;
		}

		// (4) insert new trip after current leg:
		for ( int ijk = 0 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos + ijk, newTripElements.get(ijk) ) ;
		}
		
		log.debug("agent" + agent.getId() + " new plan: " + planElements.toString());
		WithinDayAgentUtils.resetCaches(agent);

		if ( wantsToLeaveStop ) {
			transitAgentTracker.removeAgentFromStop(ptPassengerAgent, currentOrNextStop.getId());
			((MobsimAgent) ptPassengerAgent).endLegAndComputeNextState( now );
		}


		this.scenario.getPopulation().getPersonAttributes().putAttribute( agent.getId().toString(), AgentSnapshotInfo.marker, true ) ;

	}
	
	/**
	 * This assumes that all generic routes are actually teleported. There is no
	 * apparent way to differentiate teleported legs (like typically walk, bike,
	 * access_walk, egress_walk) from possible other modes using generic routes, but
	 * hypothetically something else than teleportation.
	 */
	private void replanCurrentLegWithGenericRoute(Activity newAct, String routingMode, Leg currentLeg, double now, MobsimAgent agent) {

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		
		/*
		 * Some TransportModes/TripRouters might have no 
		 * StageActivityType registered, so we might need to invent a new one, add 
		 * scoring parameters and register it as StageActivityType.
		 * 
		 * For the time being, we have no elegant way to access the teleportation engine
		 * to redirect current teleportation legs after departure. So we have to let the
		 * teleportation legs end at their originally planned destination and replan from
		 * there.
		 * 
		 * It would be better to break teleportation legs at their current position in 
		 * line with how EditTrips treats NetworkRoutes (re-route from current link 
		 * without stage activity rather than from originally planned destination link).
		 * This could be done by replacing typical teleportation modes with NetworkRoutes
		 * (e.g. bike and walk as network mode or at least teleported along a route on
		 * the network so we would know where on that NetworkRoute they currently are.).
		 * So only short access/egress legs would remain as teleported legs.
		 * 
		 * gl may'19
		 */
		int currPosPlanElements = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Activity nextAct = (Activity) plan.getPlanElements().get(currPosPlanElements + 1);
		
		if ( tripRouter.getStageActivityTypes().isStageActivity(nextAct.getType()) ) {
			Trip trip = findCurrentTrip( agent ) ;
//			final PlanElement nextPlanElement = plan.getPlanElements().get(currPosPlanElements + 1);
			final List<PlanElement> tripElements = trip.getTripElements();
			int tripElementsIndex = tripElements.indexOf( nextAct ) ;
//			replanCurrentTripFromStageActivity(tripElements, tripElementsIndex, agent, routingMode);
			// almost exact copy from replanCurrentTripFromStageActivity, however sublist index different
			// replan from stage activity:
			List<PlanElement> subTripPlanElements = tripElements.subList(tripElementsIndex + 1,tripElements.size()) ;// toIndex is exclusive
			
			//TODO: In the following step no trip is found. TripStructureUtils.getTrips looks
			//as if having a stage activity at start might be the problem. But this means
			//replanCurrentTripFromStageActivity also cannot work and it does fail indeed :-(
			final List<Trip> trips = TripStructureUtils.getTrips( subTripPlanElements, tripRouter.getStageActivityTypes() );
			Trip subTrip = trips.get(0 ) ;
			final double dpTime = agent.getActivityEndTime() ;
			this.replanFutureTrip(subTrip, WithinDayAgentUtils.getModifiablePlan(agent), routingMode, dpTime ) ;
			
		} else {
			/*
			 * The agent is on a teleported leg and the trip will end at a real activity
			 * directly after it. So there is nothing we can replan.
			 */
			return;
		}
		
		WithinDayAgentUtils.resetCaches(agent);
	}
	
	/**
	 * @param fromFacility 
	 */
	private List<? extends PlanElement> newTripToNewActivity(Facility currentLocationFacility, Activity newAct, String mainMode, double now, 
			MobsimAgent agent, Person person, Scenario scenario) {
		log.debug("entering newTripToNewActivity") ;

		Facility toFacility =  FacilitiesUtils.toFacility( newAct, scenario.getActivityFacilities() );

		List<? extends PlanElement> newTrip = tripRouter.calcRoute(mainMode, currentLocationFacility, toFacility, now, person) ;
		return newTrip;
	}
	// replan from stage activity:
	private void replanCurrentTripFromStageActivity(Trip trip, int tripElementsIndex, 
			String mainMode, double now, MobsimAgent agent) {
		
		log.debug("entering replanCurrentTripFromStageActivity for agent" + agent.getId()) ;
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		Person person = plan.getPerson() ;
		
		if ( ! ( trip.getTripElements().get(tripElementsIndex) instanceof Activity) ) {
			throw new RuntimeException("Expected a stage activity as current plan element");
		}
		Activity currentStageActivity = (Activity) trip.getTripElements().get(tripElementsIndex);
		
		// (1) get new trip from current position to new activity:
		Facility currentLocationFacility = FacilitiesUtils.toFacility(currentStageActivity, scenario.getActivityFacilities());
		List<? extends PlanElement> newTripElements = newTripToNewActivity(currentLocationFacility, trip.getDestinationActivity(), mainMode,
				now, agent, person, scenario);

		// (2) prune the new trip up to the current leg:
		// do nothing ?!

		// (2) modify current route:
		// not necessary, we are at an activity ?!

		// (3) remove remainder of old trip after current leg in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;

		while ( !planElements.get(pos).equals(trip.getDestinationActivity()) ) {
			planElements.remove(pos) ;
		}

		// (4) insert new trip after current leg:
		for ( int ijk = 1 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos + ijk, newTripElements.get(ijk) ) ;
		}
		WithinDayAgentUtils.resetCaches(agent);
		
		// old and not working, because no Trip is found due to missing destination activity. 
//
////		String mainMode = tripRouter.getMainModeIdentifier().identifyMainMode(tripElements) ;
//		// yyyy I wonder what this will do if we are already at the egress stage.  kai, oct'17
//		
//		// subList is inclusive for the fromIndex, but exclusive for the toIndex
//		List<PlanElement> subTripPlanElements = trip.getTripElements().subList(tripElementsIndex, trip.getTripElements().size()) ;
////		Trip subTrip = new Trip( (Activity) trip.getTripElements().get(tripElementsIndex),
////				subTripPlanElements,
////				trip.getDestinationActivity());
//		List<PlanElement> subTripElementsWithDestination = new ArrayList<> (subTripPlanElements);
//		subTripElementsWithDestination.add(trip.getDestinationActivity());
//		Trip subTrip = TripStructureUtils.getTrips(subTripElementsWithDestination, tripRouter.getStageActivityTypes()).get(0) ;
//		final double dpTime = agent.getActivityEndTime() ;
//		this.replanFutureTrip(subTrip, WithinDayAgentUtils.getModifiablePlan(agent), mainMode, dpTime ) ;
	}
	// future:
	public final static boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode, PopulationFactory pf ) {
//		if ( true ) {
//			throw new RuntimeException(" this currently does not work, since the EvacAgents do not have the on-demand "
//					+ "replanning switched on.  kai, nov'17" ) ;
//		}
		List<Leg> list = Collections.singletonList( pf.createLeg( mainMode ) ) ;
		TripRouter.insertTrip(plan, fromActivity, list, toActivity ) ;
		return true ;
	}
	public final boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode ) {
		return insertEmptyTrip( plan, fromActivity, toActivity, mainMode, this.pf ) ;
	}
	/**
	 * Convenience method that estimates the trip departure time rather than explicitly requesting it.
	 * 
	 * @param trip
	 * @param plan
	 * @param mainMode
	 * @return
	 */
	public final boolean replanFutureTrip(Trip trip, Plan plan, String mainMode) {
		double departureTime = PlanRouter.calcEndOfActivity( trip.getOriginActivity(), plan, tripRouter.getConfig() ) ;
		return replanFutureTrip( trip, plan, mainMode, departureTime ) ;
	}
	public final boolean replanFutureTrip(Trip trip, Plan plan, String routingMode, double departureTime) {
		return replanFutureTrip(trip, plan, routingMode, departureTime, tripRouter, scenario );
	}

	// utility methods (plans splicing):
	private static void replaceRemainderOfCurrentRoute(Leg currentLeg, List<? extends PlanElement> newTrip, MobsimAgent agent) {
		Leg newCurrentLeg = (Leg) newTrip.get(0) ;
		Gbl.assertNotNull(newCurrentLeg);

		// prune remaining route from current route:
		NetworkRoute oldNWRoute = (NetworkRoute) currentLeg.getRoute();
		
		final Integer currentRouteLinkIdIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);
		
		final NetworkRoute newNWRoute = (NetworkRoute)newCurrentLeg.getRoute();
		
		final List<Id<Link>> newLinksIds = newNWRoute.getLinkIds().subList(0,newNWRoute.getLinkIds().size()) ;
		EditRoutes.spliceNewPathIntoOldRoute(currentRouteLinkIdIndex, newNWRoute.getEndLinkId(),
				oldNWRoute, newLinksIds, agent.getCurrentLinkId()) ;
		
//		for (int ii = currentRouteLinkIdIndex; ii<oldNWRoute.getLinkIds().size() ; ii++ ) {
//			oldNWRoute.getLinkIds().remove(ii) ;
//		}
//
//		// now add the new route (yyyyyy not sure if it starts with correct link)
//		oldNWRoute.getLinkIds().addAll( newNWRoute.getLinkIds() ) ;
//
//		// also change the arrival link id:
//		oldNWRoute.setEndLinkId( newNWRoute.getEndLinkId() ) ;
		WithinDayAgentUtils.resetCaches(agent);
	}

	private List<PlanElement> mergeOldAndNewCurrentPtLeg(Leg currentLeg, List<? extends PlanElement> newTrip,
			MobsimAgent agent, TransitStopFacility nextStop) {
		Leg newCurrentLeg = (Leg) newTrip.get(0);
		Gbl.assertNotNull(newCurrentLeg);

		List<PlanElement> newPlanElementsAfterMerge = new ArrayList<>();

		// prune remaining route from current route:
		ExperimentalTransitRoute oldPtRoute = (ExperimentalTransitRoute) currentLeg.getRoute();

		final Integer currentRouteLinkIdIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

		ExperimentalTransitRoute nextPtRoute = null;
		int indexNextPtRoute = Integer.MIN_VALUE;
		if (newCurrentLeg.getRoute() instanceof ExperimentalTransitRoute) {
			// not sure whether this can actually happen
			log.debug("new trip PlanElement 0 is pt leg " + newTrip);
			nextPtRoute = (ExperimentalTransitRoute) newCurrentLeg.getRoute();
			indexNextPtRoute = 0;
		} else if (newCurrentLeg.getRoute() instanceof GenericRouteImpl) {
			log.debug("new trip PlanElement 0 is GenericRouteImpl (= walk/bike or similar) " + newTrip);
		} else if (newTrip.size() > 1 && newTrip.get(1) instanceof Leg
				&& ((Leg) newTrip.get(1)).getRoute() instanceof ExperimentalTransitRoute) {
			// not sure whether this can actually happen
			log.debug("new trip PlanElement 1 is pt leg " + newTrip.get(0) + " --- " + newTrip.get(1) + " --- "
					+ newTrip.get(2));
			nextPtRoute = (ExperimentalTransitRoute) ((Leg) newTrip.get(1)).getRoute();
			indexNextPtRoute = 1;
		} else if (newTrip.size() > 2 && newTrip.get(2) instanceof Leg
				&& ((Leg) newTrip.get(2)).getRoute() instanceof ExperimentalTransitRoute) {
			// transit_walk + pt act + pt leg
			log.debug("agent" + agent.getId() + " currentLeg" + currentLeg + " ----- new trip PlanElement 2 is pt leg " + newTrip.get(0) + " --- " + newTrip.get(1) + " --- "
					+ newTrip.get(2));
			nextPtRoute = (ExperimentalTransitRoute) ((Leg) newTrip.get(2)).getRoute();
			indexNextPtRoute = 2;
		} else {
			// other mode before maybe another pt leg might follow
			log.debug("4" + newTrip);
		}

		if (nextPtRoute == null) {
			// -> Agent shall disembark at the next stop, then use another mode
			currentLeg.setRoute(new ExperimentalTransitRoute(
					scenario.getTransitSchedule().getFacilities().get(oldPtRoute.getAccessStopId()), nextStop,
					oldPtRoute.getLineId(), oldPtRoute.getRouteId()));
			// add pt interaction activities. transfer walk?
			Activity act = PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE,
					nextStop.getCoord(), nextStop.getLinkId());
			act.setMaximumDuration(0.0);
			newPlanElementsAfterMerge.add(act);
			// it is not clear which mode a walk leg should have here in between two modes
			
			// TODO infill leg from pt stop to start of other mode necessary? Or automatically produced while routing new mode from pt stop facility?
			
			
			for (int ijk = 0; ijk < newTrip.size(); ijk++) {
				newPlanElementsAfterMerge.add(newPlanElementsAfterMerge.size(), newTrip.get(ijk));
			}
			WithinDayAgentUtils.resetCaches(agent);
			return newPlanElementsAfterMerge;

		} else {
			if (oldPtRoute.getLineId().equals(nextPtRoute.getLineId())) {
				if (oldPtRoute.getRouteId().equals(nextPtRoute.getRouteId())
						|| transitRouteLaterStopsAt(oldPtRoute.getLineId(), oldPtRoute.getRouteId(), nextStop.getId(),
								nextPtRoute.getEgressStopId())) {
					// Same TransitRoute or other TransitRoute which also serves the new egress stop
					// -> case 1: Agent can stay on the same vehicle
					currentLeg.setRoute(new ExperimentalTransitRoute(
							scenario.getTransitSchedule().getFacilities().get(oldPtRoute.getAccessStopId()),
							scenario.getTransitSchedule().getFacilities().get(nextPtRoute.getEgressStopId()),
							oldPtRoute.getLineId(), oldPtRoute.getRouteId()));
					// Is there an access_walk leg in the new trip (router assumes the trip begins
					// here) ? If yes, we should remove the access_walk leg and the pt interaction
					// (by not copying it into newPlanElementsAfterMerge).
					for (int ijk = indexNextPtRoute; ijk < newTrip.size(); ijk++) {
						newPlanElementsAfterMerge.add(newPlanElementsAfterMerge.size(), newTrip.get(ijk));
					}
					WithinDayAgentUtils.resetCaches(agent);
					return newPlanElementsAfterMerge;
				}
			}
			// if different TransitRoute that does not serve desired egress stop
			// -> case 2: Agent shall disembark at the next stop
			currentLeg.setRoute(new ExperimentalTransitRoute(
					scenario.getTransitSchedule().getFacilities().get(oldPtRoute.getAccessStopId()), nextStop,
					oldPtRoute.getLineId(), oldPtRoute.getRouteId()));
			// add pt interaction activities. transfer walk
			Activity act = PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE,
					nextStop.getCoord(), nextStop.getLinkId());
			act.setMaximumDuration(0.0);
			newPlanElementsAfterMerge.add(act);
			// new currentLeg is otherwise not added (but replaced by the modified old
			// currentLeg)
			if (newCurrentLeg.getMode().equals(TransportMode.access_walk)) {
				// The router did not know that we come from a pt leg, but walking to another
				// TransitStopFacility should be a transit_walk and not an access_walk
				newCurrentLeg.setMode(TransportMode.transit_walk);
			}
			for (int ijk = 0; ijk < newTrip.size(); ijk++) {
				newPlanElementsAfterMerge.add(newPlanElementsAfterMerge.size(), newTrip.get(ijk));
			}
			WithinDayAgentUtils.resetCaches(agent);
			return newPlanElementsAfterMerge;
		}
	}
	
	private boolean transitRouteLaterStopsAt(Id<TransitLine> lineId, Id<TransitRoute> routeId,
			Id<TransitStopFacility> currentStopId, Id<TransitStopFacility> egressStopId) {
		boolean afterCurrentStop = false;
		for (TransitRouteStop stop : scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId).getStops()) {
			if ( currentStopId.equals(stop.getStopFacility().getId()) ) {
				afterCurrentStop = true;
			}
			if ( afterCurrentStop && egressStopId.equals(stop.getStopFacility().getId()) ) {
				return true;
			}
		}
		return false;
	}
	
	// utility methods (plans splicing):
	private static void pruneUpToCurrentLeg(Leg currentLeg, List<? extends PlanElement> newTrip) {
//		while ( newTrip.get(0) instanceof Leg && !((Leg)newTrip.get(0)).getMode().equals( currentLeg.getMode()) ) {
//			newTrip.remove(0) ;
//		}
		// yyyyyy do nothing for time being and hope for the best.
		log.warn("yyyyyy pruneUpToCurrentLeg needs to be fixed for multimodal trips & for access/egress routing.") ;
	}

	// static methods:
	/**
	 * In contrast to the other replanFutureLegRoute(...) method, the leg at the given index is replaced
	 * by a new one. This is e.g. necessary when replacing a pt trip which might consists of multiple legs
	 * and pt_interaction activities.  
	 */
	@Deprecated // prefer the non-static methods
	public static boolean replanFutureTrip( Trip trip, Plan plan, String routingMode, double departureTime, TripRouter tripRouter, Scenario scenario) {
		Person person = plan.getPerson();

		Facility fromFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), scenario.getActivityFacilities());
		Facility toFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(),
				scenario.getActivityFacilities());

		final List<? extends PlanElement> newTrip = tripRouter.calcRoute(routingMode, fromFacility, toFacility,
				departureTime, person);

//		log.debug("new trip:" + newTrip);
		for (PlanElement pe : newTrip) {
//			log.debug(pe);
		}

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());

		return true;
	}

	/**
	 * Convenience method, to be consistent with earlier syntax. kai, may'16
	 * 
	 * @param trip
	 * @param plan
	 * @param mainMode
	 * @param departureTime
	 * @param tripRouter
	 * @param scenario
	 */
	@Deprecated // prefer the non-static methods
	public static boolean relocateFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime,
			TripRouter tripRouter, Scenario scenario) {
		return replanFutureTrip(trip, plan, mainMode, departureTime, tripRouter, scenario);
	}

	public StageActivityTypes getStageActivities() {
		return tripRouter.getStageActivityTypes();
	}

	public Trip findTripAfterActivity(Plan plan, Activity activity) {
		return TripStructureUtils.findTripStartingAtActivity(activity, plan, tripRouter.getStageActivityTypes());
	}

}
