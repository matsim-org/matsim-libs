
/* *********************************************************************** *
 * project: org.matsim.*
 * EditTrips.java
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

 /**
 * 
 */
package org.matsim.withinday.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
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

import static org.matsim.core.population.PopulationUtils.createLeg;

/**
 * The methods here should modify trips, i.e. material between two non-stage-activities.
 * 
 * @author kainagel
 */
public final class EditTrips {
	private static final Logger log = Logger.getLogger(EditTrips.class) ;

	private final TripRouter tripRouter;
	private final PopulationFactory pf;
	private final InternalInterface internalInterface;
	private Scenario scenario;
	private TransitStopAgentTracker transitAgentTracker;
	private EventsManager eventsManager;

	public EditTrips( TripRouter tripRouter, Scenario scenario, InternalInterface internalInterface ) {
//		For other log level, find log4j.xml and add something like
//<logger name="org.matsim.withinday.utils.EditTrips">
//	<level value="info"/>
//</logger>
		//  We need InternalInterface to move waiting passengers from a transit stop to the next leg if replanning tells them not to board a bus there.
		if (internalInterface == null) {
			log.warn("InternalInterface is null. Replanning of pt/transit legs will not work properly and will likely fail.");
		} else {
			this.eventsManager = internalInterface.getMobsim().getEventsManager();
			for (AgentTracker tracker : (internalInterface.getMobsim().getAgentTrackers())) {
				if (tracker instanceof TransitStopAgentTracker) {
					transitAgentTracker = (TransitStopAgentTracker) tracker;
					break;
				}
			}
		}
		
		this.tripRouter = tripRouter;
		this.scenario = scenario;
		this.pf = scenario.getPopulation().getFactory() ;
		this.internalInterface = internalInterface;

		if (transitAgentTracker == null) {
			log.warn("no TransitStopAgentTracker found in qsim. Replanning of pt/transit legs will not work properly and will likely fail.");
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

		// I will treat this here in the way that it will make the trip consistent with the activities.  So if the activity in the
		// plan has changed, the trip will go to a new location.
		
		// what matters is the external interface, which is the method call.  Everything below is internal so it does not matter so much.

		Trip trip = findCurrentTrip( agent ) ;
		final PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		final List<PlanElement> tripElements = trip.getTripElements();
		int tripElementsIndex = tripElements.indexOf( currentPlanElement ) ;

		if ( currentPlanElement instanceof Activity ) {
			// we are on a stage activity.  Take it from there:
			// TODO: this method fails with exception
			replanCurrentTripFromStageActivity(trip, tripElementsIndex, routingMode, now, agent);
		} else {
			// we are on a leg
			replanCurrentTripFromLeg(trip.getDestinationActivity(), currentPlanElement, routingMode, now, agent);
		}

		if (eventsManager != null) {
			ReplanningEvent replanningEvent = new ReplanningEvent(now, agent.getId(), "EditTrips.replanCurrentTrip");
			eventsManager.processEvent(replanningEvent);
		}
		WithinDayAgentUtils.resetCaches(agent);
		return true ;
	}
	private void replanCurrentTripFromLeg(Activity newAct, final PlanElement currentPlanElement, final String routingMode, double now, MobsimAgent agent) {
		log.debug("entering replanCurrentTripFromLeg for agent" + agent.getId());
		Leg currentLeg = (Leg) currentPlanElement ;
		if ( currentLeg.getRoute() instanceof NetworkRoute ) {
			replanCurrentLegWithNetworkRoute(newAct, routingMode, currentLeg, now, agent);
		} else if ( currentLeg.getRoute() instanceof ExperimentalTransitRoute ) {
			// public transit leg
			replanCurrentLegWithTransitRoute(newAct, routingMode, currentLeg, now, agent);
		} else if ( currentLeg.getRoute() instanceof GenericRouteImpl ) {
			// teleported leg
			replanCurrentLegWithGenericRoute(newAct, routingMode, currentLeg, now, agent);
		} else {
			throw new ReplanningException("not implemented for the route type of the current leg") ;
		}
		WithinDayAgentUtils.resetCaches(agent);
	}

	private void replanCurrentLegWithNetworkRoute(Activity newAct, String mainMode, Leg currentLeg, double now, MobsimAgent agent) {
		log.debug("entering replanCurrentLegWithNetworkRoute for agent" + agent.getId()) ;
		
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		Person person = plan.getPerson() ;
		
		// (1) get new trip from current position to new activity:
		Link currentLink = scenario.getNetwork().getLinks().get( agent.getCurrentLinkId() ) ;
		Facility currentLocationFacility = FacilitiesUtils.wrapLink( currentLink ) ;
		List<? extends PlanElement> newTripElements = newTripToNewActivity(currentLocationFacility, newAct, mainMode, now, person );

		// (2) there should be no access leg even with access/egress routing
		Gbl.assertIf( ! ((Leg)newTripElements.get(1)).getMode().equals( TransportMode.non_network_walk ) );

		// (3) modify current route within current leg:
		replaceRemainderOfCurrentRoute(currentLeg, newTripElements, agent);

		// (4) remove remainder of old trip after current leg in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;
		while ( !planElements.get(pos).equals(newAct) ) {
			planElements.remove(pos) ;
		}

		// (5) insert new trip after current leg:
		for ( int ijk = 1 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos, newTripElements.get(ijk) ) ;
		}

		WithinDayAgentUtils.resetCaches(agent);
	}

	private void replanCurrentLegWithTransitRoute(Activity newAct, String routingMode, Leg currentLeg, double now, MobsimAgent agent) {
		log.debug("entering replanCurrentLegWithTransitRoute for agentId=" + agent.getId()) ;

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
			currentOrNextStop = scenario.getTransitSchedule().getFacilities().get(oldPtRoute.getAccessStopId());
			log.debug( "agent with ID=" + agent.getId() + " is waiting at a stop=" + currentOrNextStop ) ;
			newTripElements = newTripToNewActivity(currentOrNextStop, newAct, routingMode, now, person );
			Gbl.assertIf( newTripElements.get(0) instanceof Leg ); // that is what TripRouter should do.  kai, jul'19
			if (	 ((Leg) newTripElements.get(0)).getRoute() instanceof ExperimentalTransitRoute
					&& ((ExperimentalTransitRoute) ((Leg) newTripElements.get(0)).getRoute()).getAccessStopId().equals(currentOrNextStop.getId())) {
				log.debug( "agent with ID=" + agent.getId() + " will wait for vehicle departing at the same stop facility." ) ;
				// don't remove the agent from the stop tracker
			} else {
				log.debug("agent with ID=" + agent.getId() + " will leave the stop facility.") ;
				// The agent will not board any bus at the transit stop and will walk away or
				// do something else. We have to remove him from the list of waiting agents.
				wantsToLeaveStop = true ;
			}
		} else {
			log.debug( "agent with ID=" + agent.getId() + " is on a vehicle" );
			// agent is asked whether she intends to leave at each stop, so no need to tell
			// mobsim about changed plan in any particular way
			TransitDriverAgent driver = (TransitDriverAgent) mobsimVehicle.getDriver();
			currentOrNextStop = driver.getNextTransitStop();
			/*
			 *  Look up scheduled arrival time at next transit stop and replan from that time.
			 *  Use planned arrival time instead of some kind of real arrival including delay in order to allow the router
			 *  to find the transit route the agent is currently staying on. Otherwise the router would assume that this bus has
			 *  already departed and cannot be reached by the agent even though the agent is currently located on that very same
			 *  bus (which the router does not know :-( )
			 *  We have no router or schedule data in "real time", i.e. considering delays, so lets assume that all pt departures
			 *  are punctual. If we would route from the current time (= "now") instead, it is unclear how long it will take to 
			 *  arrive at the next stop and we risk that the agent misses the bus he is already riding on. - gl jul'19 
			 */
			double reRoutingTime;
			if (driver instanceof TransitDriverAgentImpl) { // this is ugly, but there seems to be no other way to find out the scheduled arrival time. Maybe add to interface?
				TransitDriverAgentImpl driverImpl = (TransitDriverAgentImpl) driver;
				double departureFirstTransitRouteStop = driverImpl.getDeparture().getDepartureTime();
				double arrivalOffsetNextTransitRouteStop = driverImpl.getTransitRoute().getStop(currentOrNextStop).getArrivalOffset();
				reRoutingTime = departureFirstTransitRouteStop + arrivalOffsetNextTransitRouteStop;
			} else {
				throw new RuntimeException("transit driver is not a TransitDriverAgentImpl, not implemented!");
			}
			log.debug( "agent with ID=" + agent.getId() + " is re-routed from next stop " + currentOrNextStop.getId() + " scheduled arrival at " + reRoutingTime);
			newTripElements = newTripToNewActivity(currentOrNextStop, newAct, routingMode, reRoutingTime, person );
			// yyyy as discussed elswhere, would make more sense to compute this once arrived at that stop.
		}

		log.debug("") ;
		log.debug("newTrip for agentId=" + agent.getId() );
		for( PlanElement planElement : newTripElements ){
			log.debug(planElement) ;
		}
		log.debug("") ;

		//??????????
		// (2) prune the new trip up to the current leg: -> for pt better as one step together with mergeOldAndNewCurrentPtLeg
//		pruneUpToCurrentLeg(currentLeg, newTripElements);

		// (2) prune the new trip up to the current leg and modify current route, return additional PlanElements if necessary for merging:
		newTripElements = mergeOldAndNewCurrentPtLeg(currentLeg, newTripElements, agent, currentOrNextStop);
		// yyyyyy I would do the different infill in the case differentiation above (and not try to autosense
		// which case we have inside mergeOldAndNewCurrentPtLeg). See fix-edittrips branch.  kai, jul'19

		// (3) remove remainder of old trip after current leg in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;

		while ( !planElements.get(pos).equals(newAct) ) {
			planElements.remove(pos) ;
		}

		// (4) insert new trip after current leg:
		for ( int ijk = 0 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos + ijk, newTripElements.get(ijk) ) ;
		}

		{
			// some simplified output:
			StringBuilder stb = new StringBuilder();
			boolean first = true;
			for( PlanElement planElement : planElements ){
				if( first ){
					first = false;
				} else{
					stb.append( "---" );
				}
				if( planElement instanceof Leg ){
					stb.append( ((Leg) planElement).getMode() );
				} else if( planElement instanceof Activity ){
					stb.append( ((Activity) planElement).getType() );
				}
			}
			log.debug("") ;
			log.debug( "agent" + agent.getId() + " new plan: " + stb.toString() );
		}
		log.debug("") ;
		log.debug("agent" + agent.getId() + " new plan: " ) ;
		for( PlanElement planElement : planElements ){
			log.debug(planElement) ;
		}
		log.debug("") ;

		WithinDayAgentUtils.resetCaches(agent);

		if ( wantsToLeaveStop ) {
			if (transitAgentTracker == null) {
				log.error("Replanning a pt/transit leg, but there is no TransitStopAgentTracker found in qsim. Failing...");
				throw new RuntimeException("no TransitStopAgentTracker found in qsim");
			}

			transitAgentTracker.removeAgentFromStop(ptPassengerAgent, currentOrNextStop.getId());
			((MobsimAgent) ptPassengerAgent).endLegAndComputeNextState( now );
			this.internalInterface.arrangeNextAgentState( (MobsimAgent) ptPassengerAgent );
		}

		PopulationUtils.putPersonAttribute( person, AgentSnapshotInfo.marker, true );
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
			Facility fromFacility = FacilitiesUtils.toFacility(nextAct, scenario.getActivityFacilities());
			Facility toFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), scenario.getActivityFacilities());
			/*
			 * PopulationUtils.decideOnActivityEndTime() does not take into account that the agent might be on a PlanElement
			 * before the activity and assumes instead that the agent starts the activity at the time passed as parameter and
			 * calculates a suitable activity end time based on that start time. This is not useful here :-(
			 * The end time of the previous activity is not updated when the agent departs for the leg.
			 * Leg departure time exists as variable but is -Infinity.... 
			 * So we have no clear and reliable information when the agent will finish its teleport leg.
			 * Do some estimation and hope for a TODO better solution in the future :-(
			 * - gl jul'19 
			 */
			//			double departureTime = PopulationUtils.decideOnActivityEndTime( nextAct, now, scenario.getConfig() );
			Activity previousActivity = (Activity) plan.getPlanElements().get(currPosPlanElements - 1);
			// We don't know where the agent is located on its teleport leg and when it will arrive. Let's assume the agent is 
			// located half way between origin and destination of the teleport leg.
			double departureTime = now + 0.5 * currentLeg.getTravelTime();
			// Check whether looking into previousActivity.getEndTime() gives plausible estimation results (potentially more precise)
			// Not clear whether this is more precise than using now. If agents end their activities on time it is, otherwise unclear.
			if (Double.isFinite(previousActivity.getEndTime()) && previousActivity.getEndTime() < now) {
				// the last activity has a planned end time defined, hope that the end time is close to the real end time:
				double departureTimeAccordingToPlannedActivityEnd = previousActivity.getEndTime() + currentLeg.getTravelTime();
				// plausibility check: The agent can only arrive after the current time
				if (departureTimeAccordingToPlannedActivityEnd > now) {
					departureTime = departureTimeAccordingToPlannedActivityEnd;
				}
			}
			final List<? extends PlanElement> newTrip = tripRouter.calcRoute(routingMode, fromFacility, toFacility, departureTime, plan.getPerson() );
			TripRouter.insertTrip(plan, nextAct, newTrip, trip.getDestinationActivity() ) ;
		} else {
			/*
			 * The agent is on a teleported leg and the trip will end at a real activity
			 * directly after it. So there is nothing we can replan.
			 */
			return;
		}

		WithinDayAgentUtils.resetCaches(agent);
	}

	private List<? extends PlanElement> newTripToNewActivity( Facility currentLocationFacility, Activity newAct, String mainMode, double now, Person person ) {
		log.debug("entering newTripToNewActivity") ;

		Facility toFacility =  FacilitiesUtils.toFacility( newAct, scenario.getActivityFacilities() );

		return tripRouter.calcRoute(mainMode, currentLocationFacility, toFacility, now, person );
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
				now, person );

		// (2) prune the new trip up to the current leg:
		// do nothing ?!

		// (2) modify current route:
		// not necessary, we are at an activity ?!

		// (3) remove remainder of old trip after current stage activity in plan:
		int pos = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) + 1 ;

		while ( !planElements.get(pos).equals(trip.getDestinationActivity()) ) {
			planElements.remove(pos) ;
		}

		// (4) insert new trip after current activity:
		for ( int ijk = 1 ; ijk < newTripElements.size() ; ijk++ ) {
			planElements.add( pos + ijk, newTripElements.get(ijk) ) ;
		}
		WithinDayAgentUtils.resetCaches(agent);

	}
	public static boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode, PopulationFactory pf ) {
		List<Leg> list = Collections.singletonList( pf.createLeg( mainMode ) ) ;
		TripRouter.insertTrip(plan, fromActivity, list, toActivity ) ;
		return true ;
	}
	public final boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode ) {
		return insertEmptyTrip( plan, fromActivity, toActivity, mainMode, this.pf ) ;
	}
	/**
	 * Convenience method that estimates the trip departure time rather than explicitly requesting it.
	 */
	public final List<? extends PlanElement> replanFutureTrip( Trip trip, Plan plan, String mainMode ) {
		double departureTime = PlanRouter.calcEndOfActivity( trip.getOriginActivity(), plan, tripRouter.getConfig() ) ;
		return replanFutureTrip( trip, plan, mainMode, departureTime ) ;
	}

	public final List<? extends PlanElement> replanFutureTrip(Trip trip, Plan plan, String routingMode,
			double departureTime) {
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

		if (newCurrentLeg.getRoute() instanceof ExperimentalTransitRoute) {
			// not sure whether this can actually happen
			log.debug("new trip PlanElement 0 is a pt leg " + newTrip);
			ExperimentalTransitRoute nextPtRoute = (ExperimentalTransitRoute) newCurrentLeg.getRoute();
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
					for (int ijk = 0; ijk < newTrip.size(); ijk++) {
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
			if (newCurrentLeg.getMode().equals(TransportMode.non_network_walk )) {
				// The router did not know that we come from a pt leg, but walking to another
				// TransitStopFacility should be a transit_walk and not an access_walk
				newCurrentLeg.setMode(TransportMode.transit_walk);
			}
			for (int ijk = 0; ijk < newTrip.size(); ijk++) {
				newPlanElementsAfterMerge.add(newPlanElementsAfterMerge.size(), newTrip.get(ijk));
			}
			WithinDayAgentUtils.resetCaches(agent);
			return newPlanElementsAfterMerge;
		} else {
			log.debug("new trip PlanElement 0 is not a pt leg (= walk/bike or similar) " + newTrip);
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

	// static methods:
	/**
	 * In contrast to the other replanFutureLegRoute(...) method, the leg at the given index is replaced
	 * by a new one. This is e.g. necessary when replacing a pt trip which might consists of multiple legs
	 * and pt_interaction activities.  
	 */
	@Deprecated // prefer the non-static methods
	public static List<? extends PlanElement> replanFutureTrip(Trip trip, Plan plan, String routingMode,
			double departureTime, TripRouter tripRouter, Scenario scenario) {
		log.debug( "entering replanFutureTrip for agentid=" + plan.getPerson().getId() ) ;

		Person person = plan.getPerson();

		Facility fromFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), scenario.getActivityFacilities());
		Facility toFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), scenario.getActivityFacilities());

		final List<? extends PlanElement> newTrip = tripRouter.calcRoute(routingMode, fromFacility, toFacility, departureTime, person);

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());

		return newTrip ;
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
	public static List<? extends PlanElement> relocateFutureTrip(Trip trip, Plan plan, String mainMode,
			double departureTime, TripRouter tripRouter, Scenario scenario) {
		return replanFutureTrip(trip, plan, mainMode, departureTime, tripRouter, scenario );
	}

	public StageActivityTypes getStageActivities() {
		return tripRouter.getStageActivityTypes();
	}

	public Trip findTripAfterActivity(Plan plan, Activity activity) {
		return TripStructureUtils.findTripStartingAtActivity(activity, plan, tripRouter.getStageActivityTypes());
	}

}
