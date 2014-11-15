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
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
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
	private final PlanAgentImpl planAgentDelegate ; 
	// yy ...Impl. :-(
	
	// indirect delegates:
	private final Person person;
	private MobsimVehicle vehicle;

	private Id<Link> cachedNextLinkId = null;

	// This agent never seriously calls the simulation back! (That's good.)
	// It is only held to get to the EventManager and to the Scenario, and, 
	// in a special case, to the AgentCounter (still necessary?)  michaz 01-2012
//	private final Netsim simulation;
	// gone. kai, nov'14

	private double activityEndTime = Time.UNDEFINED_TIME;

	private Id<Link> currentLinkId = null;

	private transient Id<Link> cachedDestinationLinkId;

	private Leg currentLeg;

	private List<Id<Link>> cachedRouteLinkIds = null;

	int currentLinkIdIndex;

	private MobsimAgent.State state = MobsimAgent.State.ABORT;

	private final EventsManager events;
	private final Scenario scenario;
	private final MobsimTimer simTimer;

	// ============================================================================================================================
	// c'tor

	public PersonDriverAgentImpl(final Plan plan, final Netsim simulation) {
		this.planAgentDelegate = new PlanAgentImpl(plan) ;
		this.person = plan.getPerson();
		this.events = simulation.getEventsManager() ;
		this.scenario = simulation.getScenario() ;
		this.simTimer = simulation.getSimTimer() ;
		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
		// yy should we keep the back pointer to the simTimer?  Without it, we need to pass more simulation times around but it might be nice to do it
		// in that way.

		List<? extends PlanElement> planElements = this.getCurrentPlan().getPlanElements();
		if (planElements.size() > 0) {
			Activity firstAct = (Activity) planElements.get(0);				
			this.currentLinkId = firstAct.getLinkId();
			this.state = MobsimAgent.State.ACTIVITY ;
			calculateAndSetDepartureTime(firstAct);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endActivityAndComputeNextState(final double now) {
//		Activity act = (Activity) this.getPlanElements().get(this.getCurrentPlanElementIndex());
		Activity act = (Activity) this.planAgentDelegate.getCurrentPlanElement() ;
		this.events.processEvent(
				new ActivityEndEvent(now, this.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));

		// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
		advancePlan();
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endLegAndComputeNextState(final double now) {
		this.events.processEvent(new PersonArrivalEvent( now, this.getId(), this.getDestinationLinkId(), currentLeg.getMode()));
		if( (!(this.currentLinkId == null && this.cachedDestinationLinkId == null)) 
				&& !this.currentLinkId.equals(this.cachedDestinationLinkId)) {
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.cachedDestinationLinkId
					+ ", but arrived on link " + this.currentLinkId + ". Removing the agent from the simulation.");
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
		this.currentLinkId = linkId;
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		if (expectedLinkWarnCount < 10 && !newLinkId.equals(this.cachedNextLinkId)) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ... This warning is suppressed after the first 10 warnings.") ;
			expectedLinkWarnCount++;
		}
		this.currentLinkId = newLinkId;
		this.currentLinkIdIndex++;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	@Override
	public Id<Link> chooseNextLinkId() {

		// Please, let's try, amidst all checking and caching, to have this method return the same thing
		// if it is called several times in a row. Otherwise, you get Heisenbugs.
		// I just fixed a situation where this method would give a warning about a bad route and return null
		// the first time it is called, and happily return a link id when called the second time.

		// michaz 2013-08

		if (this.cachedNextLinkId != null) {
			return this.cachedNextLinkId;
		}
		if (this.cachedRouteLinkIds == null) {
			if ( this.currentLeg.getRoute() instanceof NetworkRoute ) {
				this.cachedRouteLinkIds = ((NetworkRoute) this.currentLeg.getRoute()).getLinkIds();
			} else {
				// (seems that this can happen if an agent is a DriverAgent, but wants to start a pt leg. 
				// A situation where Marcel's ``wrapping approach'' may have an advantage.  On the other hand,
				// DriverAgent should be a NetworkAgent, i.e. including pedestrians, and then this function
				// should always be answerable.  kai, nov'11)
				return null ;
			}
		}

		if (this.currentLinkIdIndex >= this.cachedRouteLinkIds.size() ) {
			// we have no more information for the route, so the next link should be the destination link
			Link currentLink = this.scenario.getNetwork().getLinks().get(this.currentLinkId);
			Link destinationLink = this.scenario.getNetwork().getLinks().get(this.cachedDestinationLinkId);
			if (currentLink == destinationLink && this.currentLinkIdIndex > this.cachedRouteLinkIds.size()) {
				// this can happen if the last link in a route is a loop link. Don't ask, it can happen in special transit simulation cases... mrieser/jan2014
				return null;
			}
			if (currentLink.getToNode().equals(destinationLink.getFromNode())) {
				this.cachedNextLinkId = destinationLink.getId();
				return this.cachedNextLinkId;
			}
			if (!(this.currentLinkId.equals(this.cachedDestinationLinkId))) {
				// there must be something wrong. Maybe the route is too short, or something else, we don't know...
				log.error("The vehicle with driver " + this.getPerson().getId() + ", currently on link " + this.currentLinkId.toString()
						+ ", is at the end of its route, but has not yet reached its destination link " + this.cachedDestinationLinkId.toString());
				// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
			}
			return null; // vehicle is at the end of its route
		}


		Id<Link> nextLinkId = this.cachedRouteLinkIds.get(this.currentLinkIdIndex);
		Link currentLink = this.scenario.getNetwork().getLinks().get(this.currentLinkId);
		Link nextLink = this.scenario.getNetwork().getLinks().get(nextLinkId);
		if (currentLink.getToNode().equals(nextLink.getFromNode())) {
			this.cachedNextLinkId = nextLinkId; //save time in later calls, if link is congested
			return this.cachedNextLinkId;
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentLinkIdIndex + " ]");
		// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
		return null;
	}


	// ============================================================================================================================
	// below there only (package-)private methods or setters/getters

	private void advancePlan() {
//		this.planAgentDelegate.setCurrentPlanElementIndex(this.planAgentDelegate.getCurrentPlanElementIndex() + 1);
		this.planAgentDelegate.advancePlan() ;

		// check if plan has run dry:
		if ( this.planAgentDelegate.getCurrentPlanElementIndex() >= this.planAgentDelegate.getCurrentPlan().getPlanElements().size() ) {
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
			this.cachedDestinationLinkId = route.getEndLinkId();

			// set the route according to the next leg
			this.currentLeg = leg;
			this.cachedRouteLinkIds = null;
			this.currentLinkIdIndex = 0;
			this.cachedNextLinkId = null;
        }
	}

	private void initializeActivity(Activity act) {
		this.state = MobsimAgent.State.ACTIVITY ;

		double now = this.simTimer.getTimeOfDay() ;
		this.events.processEvent(
				new ActivityStartEvent(now, this.getId(), this.currentLinkId, act.getFacilityId(), act.getType()));
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
		this.cachedRouteLinkIds = null;
		this.cachedDestinationLinkId = null;

		/*
		 * The Leg may have been exchanged in the Person's Plan, so
		 * we update the Reference to the currentLeg Object.
		 */
		PlanElement currentPlanElement = this.planAgentDelegate.getCurrentPlanElement() ;
		if (currentPlanElement instanceof Leg) {
			this.currentLeg  = ((Leg) currentPlanElement);
			this.cachedRouteLinkIds = null;

			Route route = currentLeg.getRoute();
			if (route == null) {
				log.error("The agent " + this.getId() + " has no route in its leg. Setting agent state to abort." );
				this.state = MobsimAgent.State.ABORT ;
				return;
			}
			this.cachedDestinationLinkId = route.getEndLinkId();
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
		double now = this.simTimer.getTimeOfDay() ;
		ActivityDurationInterpretation activityDurationInterpretation =
				(this.scenario.getConfig().plans().getActivityDurationInterpretation());
		double departure = ActivityDurationUtils.calculateDepartureTime(act, now, activityDurationInterpretation);

		if ( this.planAgentDelegate.getCurrentPlanElementIndex() == this.planAgentDelegate.getCurrentPlan().getPlanElements().size()-1 ) {
			if ( finalActHasDpTimeWrnCnt < 1 && departure!=Double.POSITIVE_INFINITY ) {
				log.error( "last activity of person driver agent id " + this.person.getId() + " has end time < infty; setting it to infty") ;
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
	public final void setVehicle(final MobsimVehicle veh) {
		this.vehicle = veh;
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public final double getActivityEndTime() {
		// yyyyyy I don't think there is any guarantee that this entry is correct after an activity end re-scheduling.  kai, oct'10
		return this.activityEndTime;
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		// note: the method is really only defined for DriverAgent!  kai, oct'10
		return this.currentLinkId;
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
		if( this.planAgentDelegate.getCurrentPlanElementIndex() >= this.getCurrentPlan().getPlanElements().size() ) {
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

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) {
			return route.getVehicleId();
		} else {
            if (!this.scenario.getConfig().qsim().getUsePersonIdForMissingVehicleId()) {
                throw new IllegalStateException("NetworkRoute without a specified vehicle id.");
            }
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.
		}
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.cachedDestinationLinkId;
	}

	@Override
	public final Person getPerson() {
		return this.person;
	}

	@Override
	public final Id<Person> getId() {
		return this.person.getId();
	}

	@Override
	public MobsimAgent.State getState() {
		return state;
	}
	@Override
	public PlanElement getCurrentPlanElement() {
		return planAgentDelegate.getCurrentPlanElement();
	}
	@Override
	public PlanElement getNextPlanElement() {
		return planAgentDelegate.getNextPlanElement();
	}
	@Override
	public final Plan getCurrentPlan() {
		return planAgentDelegate.getCurrentPlan();
	}

	final Plan getModifiablePlan() {
		return planAgentDelegate.getModifiablePlan() ;
	}
	
	final int getCurrentPlanElementIndex() {
		return planAgentDelegate.getCurrentPlanElementIndex() ;
	}



}
