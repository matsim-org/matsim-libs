/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgent.java
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

package org.matsim.core.mobsim.qsim.agents;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether, nagel
 * <p/>
 * I think this class is reasonable in terms of what is public and/or final and what not.
 */
public class PersonDriverAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent {
	// renamed this from DefaultPersonDriverAgent to PersonDriverAgentImpl to mark that people should (in my view) not
	// use this class directly.  kai, nov'10

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private static int expectedLinkWarnCount = 0;

	// direct delegates:
	final BasicPlanAgentImpl basicAgentDelegate ; 
	// yy ...Impl.  Not sure if this is a problem; can say that we have pluggable implementations, and they fulfill 
	// (also) part of a public interface. kai, nov'14

	// other:
	private Id<Link> cachedNextLinkId = null;

	private double activityEndTime = Time.UNDEFINED_TIME;

	private Leg currentLeg;

	/* package, because of withinday */ int currentLinkIdIndex;

	private MobsimAgent.State state = MobsimAgent.State.ABORT;

	// ============================================================================================================================
	// c'tor

	public PersonDriverAgentImpl(final Plan plan, final Netsim simulation) {
		this.basicAgentDelegate = new BasicPlanAgentImpl(plan, simulation.getScenario(), simulation.getEventsManager(), simulation.getSimTimer()) ;
		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
		// yy should we keep the back pointer to the simTimer?  Without it, we need to pass more simulation times around but it might be nice to do it
		// in that way. kai, nov'14

		// I am moving the VehicleUsingAgent functionality into the basicAgentDelegate since a separate VehicleUsingAgentImpl to only
		// pass into DriverAgent and PassengerAgent seems overkill.  kai, nov'14

		List<? extends PlanElement> planElements = this.getCurrentPlan().getPlanElements();
		if (planElements.size() > 0) {
			Activity firstAct = (Activity) planElements.get(0);				
			this.basicAgentDelegate.setCurrentLinkId( firstAct.getLinkId() ) ;
			this.state = MobsimAgent.State.ACTIVITY ;
			calculateAndSetDepartureTime(firstAct);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endActivityAndComputeNextState(final double now) {
		//		Activity act = (Activity) this.getPlanElements().get(this.getCurrentPlanElementIndex());
		Activity act = (Activity) this.basicAgentDelegate.getCurrentPlanElement() ;
		this.basicAgentDelegate.getEvents().processEvent(
				new ActivityEndEvent(now, this.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));

		// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
		advancePlan();
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endLegAndComputeNextState(final double now) {
		this.basicAgentDelegate.getEvents().processEvent(new PersonArrivalEvent( now, this.getId(), this.getDestinationLinkId(), currentLeg.getMode()));
		if( (!(this.getCurrentLinkId() == null && this.getDestinationLinkId() == null)) 
				&& !this.getCurrentLinkId().equals(this.getDestinationLinkId())) {
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.getDestinationLinkId()
					+ ", but arrived on link " + this.getCurrentLinkId() + ". Removing the agent from the simulation.");
			this.state = MobsimAgent.State.ABORT ;
		} else {
			// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
			advancePlan() ;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void setStateToAbort(final double now) {
		this.state = MobsimAgent.State.ABORT ;
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.basicAgentDelegate.setCurrentLinkId( linkId ) ;
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		if (expectedLinkWarnCount < 10 && !newLinkId.equals(this.cachedNextLinkId)) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ... This warning is suppressed after the first 10 warnings.") ;
			expectedLinkWarnCount++;
		}
		this.basicAgentDelegate.setCurrentLinkId( newLinkId ) ;
		this.currentLinkIdIndex++;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	@Override
	public final Id<Link> chooseNextLinkId() {
		// To note: there is something really stupid going on here: A vehicle that is at the end of its route and on the destination link will arrive.
		// However, a vehicle that is on the destination link BUT NOT AT THE END OF ITS ROUTE will NOT arrive.  This makes the whole thing
		// very extremely messy.  kai, nov'14

		// Please, let's try, amidst all checking and caching, to have this method return the same thing
		// if it is called several times in a row. Otherwise, you get Heisenbugs.
		// I just fixed a situation where this method would give a warning about a bad route and return null
		// the first time it is called, and happily return a link id when called the second time.  michaz 2013-08

		// Agreed.  One should also not assume that anything here is the result of one consistent design process.  Rather, many people added
		// and removed material as they needed it for their own studies.  Making the whole code more consistent would be highly
		// desirable.  kai, nov'14

		if (this.cachedNextLinkId != null) {
			return this.cachedNextLinkId;
		}

		if ( ! ( this.currentLeg.getRoute() instanceof NetworkRoute ) ) {
			return null ;
		}

		List<Id<Link>> routeLinkIds = ((NetworkRoute) this.currentLeg.getRoute()).getLinkIds();

		if (this.currentLinkIdIndex >= routeLinkIds.size() ) {
			// we have no more information from the routeLinkIds

			Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(this.getCurrentLinkId());
			Link destinationLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(this.getDestinationLinkId());

			// special case:
			if (currentLink == destinationLink && this.currentLinkIdIndex > routeLinkIds.size()) {
				// this can happen if the last link in a route is a loop link. Don't ask, it can happen in special transit simulation cases... mrieser/jan2014

				// the standard condition for arrival is "route has run dry AND destination link not attached to current link".  now with loop links,
				// this condition is never triggered.  So no wonder that for such cases currently a special condition is needed.  kai, nob'14

				return null;
			}

			// destination is is attached to intersection ahead:
			if (currentLink.getToNode().equals(destinationLink.getFromNode())) {
				this.cachedNextLinkId = destinationLink.getId();
				return this.cachedNextLinkId;
			}

			// else return null.  
			// yy this is also used when driving by the arrival point and checking if this is truly the arrival link
			// (i.e. plan has run out AND this is the destination link ... so we can drive past the ultimate destination if we want to)

			if (!(this.getCurrentLinkId().equals(this.getDestinationLinkId()))) {
				// normally, we should be here when on the destination link.  Otherwise, something has gone wrong.
				log.error("The vehicle with driver " + this.getPerson().getId() + ", currently on link " + this.getCurrentLinkId().toString()
						+ ", is at the end of its route, but has not yet reached its destination link " + this.getDestinationLinkId().toString());
				// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
				// (Also no abort is initiated by mobsim. kai, nov'14)
			}
			return null;
		}


		Id<Link> nextLinkId = routeLinkIds.get(this.currentLinkIdIndex);
		Link currentLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(this.getCurrentLinkId());
		Link nextLink = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(nextLinkId);
		if (currentLink.getToNode().equals(nextLink.getFromNode())) {
			this.cachedNextLinkId = nextLinkId; //save time in later calls, if link is congested
			return this.cachedNextLinkId;
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentLinkIdIndex + " ]");
		// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
		return null;
	}

	//	@Override
	//	public final boolean isArrivingOnCurrentLink( ) {
	//		// this is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
	//		// not sure if there was an error eventually. kai, nov'14
	//		if ( this.chooseNextLinkId()==null ) {
	//			return true ;
	//		} else {
	//			return false ;
	//		}
	//	}

	@Override
	public final boolean isArrivingOnCurrentLink( ) {
		if ( ! ( this.currentLeg.getRoute() instanceof NetworkRoute ) ) {
			// non-network links in the past have always returned true (i.e. "null" to the chooseNextLink question):
			return true ;
		}

		List<Id<Link>> routeLinkIds = ((NetworkRoute) this.currentLeg.getRoute()).getLinkIds();

		// the standard condition is "route has run dry AND destination link not attached to current link":
		// 2nd condition essentially means "destination link EQUALS current link" but really stupid way of stating this.  Thus
		// changing the second condition for the time being to "being at destination"
		if ( this.currentLinkIdIndex >= routeLinkIds.size() && this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {
			return true ;
		} else {
			return false ;
		}

	}


	// ============================================================================================================================
	// below there only (package-)private methods or setters/getters

	private void advancePlan() {
		//		this.planAgentDelegate.setCurrentPlanElementIndex(this.planAgentDelegate.getCurrentPlanElementIndex() + 1);
		this.basicAgentDelegate.advancePlan() ;

		// check if plan has run dry:
		if ( this.basicAgentDelegate.getCurrentPlanElementIndex() >= this.basicAgentDelegate.getCurrentPlan().getPlanElements().size() ) {
			log.error("plan of agent with id = " + this.getId() + " has run empty.  Setting agent state to ABORT\n" +
					"          (but continuing the mobsim).  This used to be an exception ...") ;
			this.state = MobsimAgent.State.ABORT ;
			return;
		}

		PlanElement pe = this.getCurrentPlanElement() ;
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			initializeActivity(act);
		} else if (pe instanceof Leg) {
			Leg leg = (Leg) pe;
			initializeLeg(leg);
		} else {
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}

	private void initializeLeg(Leg leg) {
		this.state = MobsimAgent.State.LEG ;			
		Route route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + this.getPerson().getId() + " has no route in its leg.  Setting agent state to ABORT " +
					"(but continuing the mobsim).");
			if ( noRouteWrnCnt < 1 ) {
				log.info( "(Route is needed inside Leg even if you want teleportation since Route carries the start/endLinkId info.)") ;
				noRouteWrnCnt++ ;
			}
			this.state = MobsimAgent.State.ABORT ;
		} else {
			//			this.cachedDestinationLinkId = route.getEndLinkId();
			this.basicAgentDelegate.setDestinationLinkId( route.getEndLinkId() );

			// set the route according to the next leg
			this.currentLeg = leg;
			this.currentLinkIdIndex = 0;
			this.cachedNextLinkId = null;
		}
	}

	private void initializeActivity(Activity act) {
		this.state = MobsimAgent.State.ACTIVITY ;

		double now = this.basicAgentDelegate.getSimTimer().getTimeOfDay() ;
		this.basicAgentDelegate.getEvents().processEvent(
				new ActivityStartEvent(now, this.getId(), this.getCurrentLinkId(), act.getFacilityId(), act.getType()));
		/* schedule a departure if either duration or endtime is set of the activity.
		 * Otherwise, the agent will just stay at this activity for ever...
		 */
		calculateAndSetDepartureTime(act);
	}

	/**
	 * Some data of the currently simulated Leg is cached to speed up
	 * the simulation. If the Leg changes (for example the Route or
	 * the Destination Link), those cached data has to be reseted.
	 *</p>
	 * If the Leg has not changed, calling this method should have no effect
	 * on the Results of the Simulation!
	 */
	/* package */ final void resetCaches() {

		// moving this method not to WithinDay for the time being since it seems to make some sense to keep this where the internal are
		// known best.  kai, oct'10
		// Compromise: package-private here; making it public in the Withinday class.  kai, nov'10

		this.cachedNextLinkId = null;
		this.basicAgentDelegate.setDestinationLinkId(null);

		/*
		 * The Leg may have been exchanged in the Person's Plan, so
		 * we update the Reference to the currentLeg Object.
		 */
		PlanElement currentPlanElement = this.basicAgentDelegate.getCurrentPlanElement() ;
		if (currentPlanElement instanceof Leg) {
			this.currentLeg  = ((Leg) currentPlanElement);

			Route route = currentLeg.getRoute();
			if (route == null) {
				log.error("The agent " + this.getId() + " has no route in its leg. Setting agent state to abort." );
				this.state = MobsimAgent.State.ABORT ;
				return;
			}
			//			this.cachedDestinationLinkId = route.getEndLinkId();
			this.basicAgentDelegate.setDestinationLinkId(route.getEndLinkId());
		} else {			
			// If an activity is performed, update its current activity.
			this.calculateAndSetDepartureTime((Activity) this.getCurrentPlanElement());
		}
	}

	/**
	 * If this method is called to update a changed ActivityEndTime please
	 * ensure, that the ActivityEndsList in the {@link QSim} is also updated.
	 */
	/* package */final void calculateAndSetDepartureTime(Activity act) {
		double now = this.basicAgentDelegate.getSimTimer().getTimeOfDay() ;
		ActivityDurationInterpretation activityDurationInterpretation =
				(this.basicAgentDelegate.getScenario().getConfig().plans().getActivityDurationInterpretation());
		double departure = ActivityDurationUtils.calculateDepartureTime(act, now, activityDurationInterpretation);

		if ( this.basicAgentDelegate.getCurrentPlanElementIndex() == this.basicAgentDelegate.getCurrentPlan().getPlanElements().size()-1 ) {
			if ( finalActHasDpTimeWrnCnt < 1 && departure!=Double.POSITIVE_INFINITY ) {
				log.error( "last activity of person driver agent id " + this.basicAgentDelegate.getId() + " has end time < infty; setting it to infty") ;
				log.error( Gbl.ONLYONCE ) ;
				finalActHasDpTimeWrnCnt++ ;
			}
			departure = Double.POSITIVE_INFINITY ;
		}

		this.activityEndTime = departure ;
	}

	private static int finalActHasDpTimeWrnCnt = 0 ;


	private static int noRouteWrnCnt = 0 ;

	@Override
	public final double getActivityEndTime() {
		// yyyyyy I don't think there is any guarantee that this entry is correct after an activity end re-scheduling.  kai, oct'10
		return this.activityEndTime;
	}

	@Override
	public final Double getExpectedTravelTime() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return ((Leg) currentPlanElement).getTravelTime();
	}

	@Override
	public final String getMode() {
		if( this.basicAgentDelegate.getCurrentPlanElementIndex() >= this.getCurrentPlan().getPlanElements().size() ) {
			// just having run out of plan elements it not an argument for not being able to answer the "mode?" question.
			// this is in most cases called in "abort".  kai, mar'12

			return null ;
		}
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return ((Leg) currentPlanElement).getMode() ;
	}

	// ==============================================
	// only delegate methods below here

	@Override
	public final void setVehicle(final MobsimVehicle veh) {
		this.basicAgentDelegate.setVehicle(veh);
	}
	@Override
	public final MobsimVehicle getVehicle() {
		return this.basicAgentDelegate.getVehicle() ;
	}
	@Override
	public final Id<Link> getCurrentLinkId() {
		return basicAgentDelegate.getCurrentLinkId();
	}
	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		return basicAgentDelegate.getPlannedVehicleId();
	}
	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.basicAgentDelegate.getDestinationLinkId();
	}
	@Override
	public final Person getPerson() {
		return this.basicAgentDelegate.getPerson() ;
	}
	@Override
	public final Id<Person> getId() {
		return this.basicAgentDelegate.getId() ;
	}
	@Override
	public final MobsimAgent.State getState() {
		return state;
	}
	@Override
	public final PlanElement getCurrentPlanElement() {
		return basicAgentDelegate.getCurrentPlanElement();
	}
	@Override
	public final PlanElement getNextPlanElement() {
		return basicAgentDelegate.getNextPlanElement();
	}
	@Override
	public final Plan getCurrentPlan() {
		return basicAgentDelegate.getCurrentPlan();
	}

	final Plan getModifiablePlan() {
		return basicAgentDelegate.getModifiablePlan() ;
	}

	final int getCurrentPlanElementIndex() {
		return basicAgentDelegate.getCurrentPlanElementIndex() ;
	}



}
