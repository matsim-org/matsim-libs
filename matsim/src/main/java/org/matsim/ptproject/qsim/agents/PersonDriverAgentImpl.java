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

package org.matsim.ptproject.qsim.agents;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.TravelEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.interfaces.Netsim;

/**
 * @author dgrether, nagel
 * <p/>
 * I think this class is reasonable in terms of what is public and/or final and what not.
 */
public class PersonDriverAgentImpl implements MobsimDriverAgent, HasPerson, PlanAgent {
	// renamed this from DefaultPersonDriverAgent to PersonDriverAgentImpl to mark that people should (in my view) not
	// use this class directly.  kai, nov'10

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	final Person person;
	private MobsimVehicle vehicle;

	Id cachedNextLinkId = null;

	private final Netsim simulation;

	private double activityEndTime = Time.UNDEFINED_TIME;

	private Id currentLinkId = null;

	int currentPlanElementIndex = 0;

	private transient Id cachedDestinationLinkId;

	private Leg currentLeg;
	private List<Id> cachedRouteLinkIds = null;

	int currentLinkIdIndex;

	private MobsimAgent.State state = MobsimAgent.State.ACTIVITY ; 
	// (yy not so great: implicit initialization.  kai, nov'11) 

	// ============================================================================================================================
	// c'tor

	public static PersonDriverAgentImpl createAndInsertPersonDriverAgentImpl(Person p, Netsim simulation) {
		return new PersonDriverAgentImpl(p, simulation);
	}
	protected PersonDriverAgentImpl(final Person p, final Netsim simulation) {

		this.person = p;
		this.simulation = simulation;

		List<? extends PlanElement> planElements = p.getSelectedPlan().getPlanElements();
		if (planElements.size() > 0) {
			this.currentPlanElementIndex = 0;
			Activity firstAct = (Activity) planElements.get(0);
			double actEndTime = firstAct.getEndTime();
			
			this.currentLinkId = firstAct.getLinkId();
			if ((actEndTime != Time.UNDEFINED_TIME) && (planElements.size() > 1)) {
				this.activityEndTime = actEndTime ;

//				this.simulation.arrangeActivityStart(this);
				this.state = MobsimAgent.State.ACTIVITY ;
				this.simulation.insertAgentIntoMobsim(this) ; // ini!
				// yyyyyy 000000


				this.simulation.getAgentCounter().incLiving();
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endActivityAndAssumeControl(final double now) {
//		Boolean flag = 
		endActivityAndComputeNextState(now);
		scheduleAgentInMobsim( );
	}
	private void endActivityAndComputeNextState(final double now) {
		Activity act = (Activity) this.getPlanElements().get(this.currentPlanElementIndex);
		this.simulation.getEventsManager().processEvent(
				this.simulation.getEventsManager().getFactory().createActivityEndEvent(
						now, this.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		// note that when we are here we don't know if next is another leg, or an activity.  Therefore, we go to a general method:
//		Boolean flag = 
		advancePlan() ;
		return ;
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endLegAndAssumeControl(final double now) {
//		Boolean ok = 
			endLegAndComputeNextState(now);
		scheduleAgentInMobsim( );
	}
	private void endLegAndComputeNextState(final double now) {
//		Boolean ok = true ;
		
		// creating agent arrival event ... ok
		this.simulation.getEventsManager().processEvent(
				this.simulation.getEventsManager().getFactory().createAgentArrivalEvent(
						now, this.getPerson().getId(), this.getDestinationLinkId(), this.getCurrentLeg().getMode()));

		if(!this.currentLinkId.equals(this.cachedDestinationLinkId)) {
			// yyyyyy needs to throw a stuck/abort event
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.cachedDestinationLinkId
					+ ", but arrived on link " + this.currentLinkId + ". Removing the agent from the simulation.");
//			this.simulation.getAgentCounter().decLiving();
//			this.simulation.getAgentCounter().incLost();
			
//			ok = false ;
			this.state = MobsimAgent.State.ABORT ;
		} else {
			// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
//			ok = 
			advancePlan() ;
		}
		return ;
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	private void scheduleAgentInMobsim( ) {
		
		if ( this.state.equals( MobsimAgent.State.ABORT ) ) {
			log.warn("found agent state of ABORT.  Not re-inserting the agent in the mobsim ...") ;
			this.simulation.getAgentCounter().decLiving();
			this.simulation.getAgentCounter().incLost();
			return ;
		}

//		if ( ok == null ) {
//			throw new RuntimeException("should not be able to get here any more" ) ;
////			throw new RuntimeException("plan has run empty" ) ;
//		}
//		if ( ok == false ) {
//			throw new RuntimeException("should not be able to get here any more" ) ;
////			return ;
//		}

		PlanElement pe = this.getCurrentPlanElement() ;

		if (pe instanceof Activity) {

			if ((this.currentPlanElementIndex+1) < this.getPlanElements().size()) {
				// there is still at least on plan element left

//				this.simulation.arrangeActivityStart(this);
				this.state = MobsimAgent.State.ACTIVITY ;
				this.simulation.reInsertAgentIntoMobsim(this);
				// yyyyyy 000000
				
				return ;
			} else {
				// this is the last activity
				this.simulation.getAgentCounter().decLiving();
				return ;
			}

		} else if (pe instanceof Leg) {

//			if ( ok ) {

//			    this.simulation.arrangeAgentDeparture(this);
			this.state = MobsimAgent.State.LEG ;
			this.simulation.reInsertAgentIntoMobsim(this) ;

			    
			    return ;
//			} else {
//				throw new RuntimeException("should not be able to get here any more" ) ;
////				log.error("The agent " + this.getId() + " returned false from advancePlan.  Removing the ag from the mobsim ...");
////				this.simulation.getAgentCounter().decLiving();
////				this.simulation.getAgentCounter().incLost();
////				return ;
//			}

		} else { // (presumably, this was already caught earlier)
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void notifyTeleportToLink(final Id linkId) {
		this.currentLinkId = linkId;
        double distance = ((Leg) getCurrentPlanElement()).getRoute().getDistance();
        this.simulation.getEventsManager().processEvent(new TravelEventImpl(this.simulation.getSimTimer().getTimeOfDay(), person.getId(), distance));
	}

	@Override
	public final void notifyMoveOverNode(Id newLinkId) {
		if ( this.cachedNextLinkId != newLinkId ) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ...") ;
		}
		this.currentLinkId = this.cachedNextLinkId;
		this.currentLinkIdIndex++;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	@Override
	public Id chooseNextLinkId() {
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
			Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
			Link destinationLink = this.simulation.getScenario().getNetwork().getLinks().get(this.cachedDestinationLinkId);
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

		this.cachedNextLinkId = this.cachedRouteLinkIds.get(this.currentLinkIdIndex); //save time in later calls, if link is congested
		Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
		Link nextLink = this.simulation.getScenario().getNetwork().getLinks().get(this.cachedNextLinkId);
		if (currentLink.getToNode().equals(nextLink.getFromNode())) {
			return this.cachedNextLinkId;
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentLinkIdIndex + " ]");
		// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
		return null;
	}


	// ============================================================================================================================
	// below there only (package-)private methods or setters/getters

	private Boolean advancePlan() {
		this.currentPlanElementIndex++;

		// check if plan has run dry:
		if ( this.currentPlanElementIndex >= this.getPlanElements().size() ) {
			log.error("plan of agent with id = " + this.getId() + " has run empty.  Setting agent state to ABORT\n" +
			"          (but continuing the mobsim).  This used to be an exception ...") ;
			this.state = MobsimAgent.State.ABORT ;
			return null ;
		}

		PlanElement pe = this.getCurrentPlanElement() ;
		if (pe instanceof Activity) {
			this.state = MobsimAgent.State.ACTIVITY ;
			Activity act = (Activity) pe;
			double now = this.getMobsim().getSimTimer().getTimeOfDay() ;
			this.simulation.getEventsManager().processEvent(
					this.simulation.getEventsManager().getFactory().createActivityStartEvent(
							now, this.getId(),  this.currentLinkId, act.getFacilityId(), act.getType()));
			/* schedule a departure if either duration or endtime is set of the activity.
			 * Otherwise, the agent will just stay at this activity for ever...
			 */
			calculateDepartureTime(act);
			return true ;
		} else if (pe instanceof Leg) {
			this.state = MobsimAgent.State.LEG ;
			Leg leg = (Leg) pe;
			Route route = leg.getRoute();
			if (route == null) {
				log.error("The agent " + this.getPerson().getId() + " has no route in its leg.  Setting agent state to ABORT " +
						"(but continuing the mobsim).");
				if ( noRouteWrnCnt < 1 ) {
					log.info( "(Route is needed inside Leg even if you want teleportation since Route carries the start/endLinkId info.)") ;
					noRouteWrnCnt++ ;
				}
				this.state = MobsimAgent.State.ABORT ;
				return false;
			}
			this.cachedDestinationLinkId = route.getEndLinkId();
			
			// set the route according to the next leg
			this.currentLeg = leg;
			this.cachedRouteLinkIds = null;
			this.currentLinkIdIndex = 0;
			this.cachedNextLinkId = null;
			return true ;
		} else {
			this.state = MobsimAgent.State.ABORT ;
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}

	/**
	 * Some data of the currently simulated Leg is cached to speed up
	 * the simulation. If the Leg changes (for example the Route or
	 * the Destination Link), those cached data has to be reseted.
	 *</p>
	 * If the Leg has not changed, calling this method should have no effect
	 * on the Results of the Simulation!
	 */
	void resetCaches() {
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
		PlanElement currentPlanElement = this.getPlanElements().get(this.currentPlanElementIndex);
		if (currentPlanElement instanceof Leg) {
			this.currentLeg  = ((Leg) currentPlanElement);
			this.cachedRouteLinkIds = null;
		}

		Route route = currentLeg.getRoute();
		if (route == null) {
			log.error("The agent " + this.getId() + " has no route in its leg. Removing the agent from the simulation.\n" +
			"          (But as far as I can tell, this will not truly remove the agent???  kai, nov'11)");
			this.simulation.getAgentCounter().decLiving();
			this.simulation.getAgentCounter().incLost();
			return;
		}
		this.cachedDestinationLinkId = route.getEndLinkId();
	}

	/**
	 * If this method is called to update a changed ActivityEndTime please
	 * ensure, that the ActivityEndsList in the {@link QSim} is also updated.
	 */
	void calculateDepartureTime(Activity act) {
		double now = this.getMobsim().getSimTimer().getTimeOfDay() ;

		if ( act.getMaximumDuration() == Time.UNDEFINED_TIME && (act.getEndTime() == Time.UNDEFINED_TIME)) {
			// yyyy does this make sense?  below there is at least one execution path where this should lead to an exception.  kai, oct'10
			this.activityEndTime = Double.POSITIVE_INFINITY ;
			return ;
		}

		double departure = 0;

		if ( this.simulation.getScenario().getConfig().vspExperimental().getActivityDurationInterpretation()
				.equals(VspExperimentalConfigGroup.MIN_OF_DURATION_AND_END_TIME) ) {
			// person stays at the activity either until its duration is over or until its end time, whatever comes first
			if (act.getMaximumDuration() == Time.UNDEFINED_TIME) {
				departure = act.getEndTime();
			} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
				departure = now + act.getMaximumDuration();
			} else {
				departure = Math.min(act.getEndTime(), now + act.getMaximumDuration());
			}
		} else if ( this.simulation.getScenario().getConfig().vspExperimental().getActivityDurationInterpretation()
				.equals(VspExperimentalConfigGroup.END_TIME_ONLY ) ) {
			if (act.getEndTime() != Time.UNDEFINED_TIME) {
				departure = act.getEndTime() ;
			} else {
				throw new IllegalStateException("activity end time not set and using something else not allowed. personId: " + this.getPerson().getId());
			}
		} else if ( this.simulation.getScenario().getConfig().vspExperimental().getActivityDurationInterpretation()
				.equals(VspExperimentalConfigGroup.TRY_END_TIME_THEN_DURATION ) ) {
			// In fact, as of now I think that _this_ should be the default behavior.  kai, aug'10
			if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
				departure = act.getEndTime();
			} else if ( act.getMaximumDuration() != Time.UNDEFINED_TIME ) {
				departure = now + act.getMaximumDuration() ;
			} else {
				throw new IllegalStateException("neither activity end time nor activity duration defined; don't know what to do. personId: " + this.getPerson().getId());
			}
		} else {
			throw new IllegalStateException("should not happen") ;
		}

		if (departure < now) {
			// we cannot depart before we arrived, thus change the time so the time stamp in events will be right
			//			[[how can events not use the simulation time?  kai, aug'10]]
			departure = now;
			// actually, we will depart in (now+1) because we already missed the departing in this time step
		}

		if ( this.currentPlanElementIndex == this.getPlanElements().size()-1 ) {
			if ( finalActHasDpTimeWrnCnt < 1 && departure!=Double.POSITIVE_INFINITY ) {
				log.error( "last activity has end time < infty; setting it to infty") ;
				log.error( Gbl.ONLYONCE ) ;
				finalActHasDpTimeWrnCnt++ ;
			}
			departure = Double.POSITIVE_INFINITY ;
		}
		this.activityEndTime = departure ;
	}
	private static int finalActHasDpTimeWrnCnt = 0 ;


	private static int noRouteWrnCnt = 0 ;

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link Activity}s and {@link Leg}s of this agent's plan
	 */
	private final List<PlanElement> getPlanElements() {
		return Collections.unmodifiableList( this.getSelectedPlan().getPlanElements() ) ;
	}

	public final Netsim getMobsim(){
		return this.simulation;
	}

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.getPlanElements().get(this.currentPlanElementIndex);
	}

	@Override
	public final PlanElement getNextPlanElement() {
		if ( this.currentPlanElementIndex < this.getPlanElements().size() ) {
			return this.getPlanElements().get( this.currentPlanElementIndex+1 ) ;
		} else {
			return null ;
		}
	}

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
	public final Id getCurrentLinkId() {
		// note: the method is really only defined for DriverAgent!  kai, oct'10
		return this.currentLinkId;
	}

	protected Leg getCurrentLeg() {
		// used by TransitAgent.  IMO, should go into same package so we can make this package-private.  kai, jun'11
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return (Leg) currentPlanElement;
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
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return ((Leg) currentPlanElement).getMode() ;
	}
	
	@Override
	public final Id getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		Route route = ((Leg) currentPlanElement).getRoute() ;
		if ( !(route instanceof NetworkRoute) ) {
			return null ;
		}
		return ((NetworkRoute)route).getVehicleId() ;
	}

	@Override
	public final Id getDestinationLinkId() {
		return this.cachedDestinationLinkId;
	}

	@Override
	public final Person getPerson() {
		return this.person;
	}

	@Override
	public final Id getId() {
		return this.person.getId();
	}

	@Override
	public Plan getSelectedPlan() {
		return PopulationUtils.unmodifiablePlan(this.person.getSelectedPlan());
	}
	public MobsimAgent.State getState() {
		return state;
	}

}
