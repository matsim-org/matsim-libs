/**
 * 
 */
package org.matsim.withinday.utils;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.Facility;

/**
 * The methods here should modify trips, i.e. material between two non-stage-activities.
 * 
 * @author kainagel
 */
public final class EditTrips {

	private final TripRouter tripRouter;
	private final PopulationFactory pf;
	private Scenario scenario;

	public EditTrips( TripRouter tripRouter, Scenario scenario ) {
		this.tripRouter = tripRouter;
		this.scenario = scenario;
		this.pf = scenario.getPopulation().getFactory() ;
	}
	public final Trip findCurrentTrip( MobsimAgent agent ) {
		PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		return findTripAtPlanElement(agent, pe);
	}
	public Trip findTripAtPlanElement(MobsimAgent agent, PlanElement pe) {
		List<Trip> trips = TripStructureUtils.getTrips( WithinDayAgentUtils.getModifiablePlan(agent), tripRouter.getStageActivityTypes() ) ;
		for ( Trip trip : trips ) {
			if ( trip.getTripElements().contains(pe) ) {
				return trip ;
			}
		}
		throw new ReplanningException("trip not found") ;
	}
	public Trip findTripAtPlanElementIndex( MobsimAgent agent, int index ) {
		return findTripAtPlanElement( agent, WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(index) ) ;
	}
	// current trip:
	public final void replanCurrentTrip(MobsimAgent agent, double now ) {
		// I will treat that in the way that it will make the trip consistent with the activities.  So if the activity in the
		// plan has changed, the trip will go to a new location.
		
		// what matters is the external interface, which is the method call.  Everything below is internal so it does not matter so much.

		Trip trip = findCurrentTrip( agent ) ;
		final PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(agent) ;
		final List<PlanElement> tripElements = trip.getTripElements();
		int tripElementsIndex = tripElements.indexOf( currentPlanElement ) ;
		final String currentMode = tripRouter.getMainModeIdentifier().identifyMainMode( tripElements ) ;

		if ( currentPlanElement instanceof Activity ) {
			// we are on a stage activity.  Take it from there:
			replanCurrentTripFromStageActivity(tripElements, tripElementsIndex, agent);
		} else {
			// we are on a leg
			replanCurrentTripFromLeg(trip.getDestinationActivity(), currentPlanElement, currentMode, now, agent, scenario);
		}
		WithinDayAgentUtils.resetCaches(agent);
	}
	private void replanCurrentTripFromLeg(Activity newAct, final PlanElement currentPlanElement, final String currentMode, 
			double now, MobsimAgent agent, Scenario scenario) {
		Leg currentLeg = (Leg) currentPlanElement ;
		if ( currentLeg.getRoute() instanceof NetworkRoute ) {
			replanCurrentLegWithNetworkRoute(newAct, currentMode, currentLeg, now, agent);
		} else {
			throw new ReplanningException("not implemented") ;
			// Does not feel so hard: 
			// * with teleported legs, push forward to next facility
			// * with passenger legs, push forward to next possibility of exit
		}
		WithinDayAgentUtils.resetCaches(agent);
	}
	private void replanCurrentLegWithNetworkRoute(Activity newAct, String mainMode, Leg currentLeg, double now, 
			MobsimAgent agent) {

		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;
		Person person = plan.getPerson() ;
		
		// (1) get new trip from current position to new activity:
		List<? extends PlanElement> newTripElements = newTripToNewActivity(newAct, mainMode, now, agent, person, scenario);

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
	private List<? extends PlanElement> newTripToNewActivity(Activity newAct, String mainMode, double now, 
			MobsimAgent agent, Person person, Scenario scenario) {
		Link currentLink = scenario.getNetwork().getLinks().get( agent.getCurrentLinkId() ) ;
		Facility<?> fromFacility = new LinkWrapperFacility( currentLink ) ; 
		Facility<?> toFacility = scenario.getActivityFacilities().getFacilities().get( newAct.getFacilityId() ) ;
		if ( toFacility == null ) {
			toFacility = new ActivityWrapperFacility( newAct ) ;
		}
		List<? extends PlanElement> newTrip = tripRouter.calcRoute(mainMode, fromFacility, toFacility, now, person) ;
		return newTrip;
	}
	// replan from stage activity:
	private void replanCurrentTripFromStageActivity(final List<PlanElement> tripElements, int tripElementsIndex, 
			MobsimAgent agent) {

		String mainMode = tripRouter.getMainModeIdentifier().identifyMainMode(tripElements) ;
		// yyyy I wonder what this will do if we are already at the egress stage.  kai, oct'17
		
		List<PlanElement> subTripPlanElements = tripElements.subList(tripElementsIndex,tripElements.size()-1) ;
		Trip subTrip = TripStructureUtils.getTrips(subTripPlanElements, tripRouter.getStageActivityTypes()).get(0) ;
		final double dpTime = agent.getActivityEndTime() ;
		this.replanFutureTrip(subTrip, WithinDayAgentUtils.getModifiablePlan(agent), mainMode, dpTime ) ;
	}
	// future:
	public final static boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode, PopulationFactory pf ) {
		List<Leg> list = Collections.singletonList( pf.createLeg( mainMode ) ) ;
		TripRouter.insertTrip(plan, fromActivity, list, toActivity ) ;
		return true ;
	}
	public final boolean insertEmptyTrip( Plan plan, Activity fromActivity, Activity toActivity, String mainMode ) {
		List<Leg> list = Collections.singletonList( pf.createLeg( mainMode ) ) ;
		TripRouter.insertTrip(plan, fromActivity, list, toActivity ) ;
		return true ;
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
	public final boolean replanFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime) {
		return replanFutureTrip(trip, plan, mainMode, departureTime, tripRouter);
	}

	// utility methods (plans splicing):
	private static void replaceRemainderOfCurrentRoute(Leg currentLeg, List<? extends PlanElement> newTrip, MobsimAgent agent) {
		Leg newCurrentLeg = (Leg) newTrip.get(0) ;
		Gbl.assertNotNull(newCurrentLeg);

		// prune remaining route from current route:
		NetworkRoute currentNWRoute = (NetworkRoute) currentLeg.getRoute();
		for ( int ii = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ; ii<currentNWRoute.getLinkIds().size() ; ii++ ) {
			currentNWRoute.getLinkIds().remove(ii) ;
		}

		// now add the new route (yyyyyy not sure if it starts with correct link)
		final NetworkRoute newNWRoute = (NetworkRoute)newCurrentLeg.getRoute();
		currentNWRoute.getLinkIds().addAll( newNWRoute.getLinkIds() ) ;

		// also change the arrival link id:
		currentNWRoute.setEndLinkId( newNWRoute.getEndLinkId() ) ;
		WithinDayAgentUtils.resetCaches(agent);
	}
	private static void pruneUpToCurrentLeg(Leg currentLeg, List<? extends PlanElement> newTrip) {
		while ( newTrip.get(0) instanceof Leg && !((Leg)newTrip.get(0)).getMode().equals( currentLeg.getMode()) ) {
			newTrip.remove(0) ;
		}
	}

	// static methods:
	/**
	 * In contrast to the other replanFutureLegRoute(...) method, the leg at the given index is replaced
	 * by a new one. This is e.g. necessary when replacing a pt trip which might consists of multiple legs
	 * and pt_interaction activities.  
	 */
	public static boolean replanFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime, TripRouter tripRouter) {
		Person person = plan.getPerson();

		Facility<?> fromFacility = new ActivityWrapperFacility( trip.getOriginActivity() ) ;
		Facility<?> toFacility = new ActivityWrapperFacility( trip.getDestinationActivity() ) ;

		final List<? extends PlanElement> newTrip = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

		TripRouter.insertTrip(plan, trip.getOriginActivity(), newTrip, trip.getDestinationActivity());

		return true;
	}

	/** Convenience method, to be consistent with earlier syntax.  kai, may'16
	 * @param trip
	 * @param plan
	 * @param mainMode
	 * @param departureTime
	 * @param network
	 * @param tripRouter
	 * @deprecated Use {@link EditTrips#relocateFutureTrip(Trip,Plan,String,double)} instead.  kai, << oct'17
	 */
	public static boolean relocateFutureTrip(Trip trip, Plan plan, String mainMode, double departureTime, Network network, TripRouter tripRouter) {
		return replanFutureTrip(trip, plan, mainMode, departureTime, tripRouter );
	}

}
