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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

/**
 * @author dgrether
 * @author nagel
 * <p/>
 * I think this class is reasonable in terms of what is public and/or final and what not.
 */
public class PersonDriverAgentImpl implements PersonDriverAgent {
	// renamed this from DefaultPersonDriverAgent to PersonDriverAgentImpl to mark that people should (in my view) not
	// use this class directly.  kai, nov'10

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	final Person person;
	private QVehicle vehicle;

	Id cachedNextLinkId = null;

	private final Mobsim simulation;

	private double activityEndTime = Time.UNDEFINED_TIME;

	private Id currentLinkId = null;

	int currentPlanElementIndex = 0;

	private transient Id cachedDestinationLinkId;

	private Leg currentLeg;
	private List<Id> cachedRouteLinkIds = null;

	int currentLinkIdIndex;
	
	// ============================================================================================================================
	// c'tor

	public PersonDriverAgentImpl(final Person p, final Mobsim simulation) {
		// yyyy this should, in my opinion, be protected since there is an interface.  kai, oct'10
		
		this.person = p;
		this.simulation = simulation;
	}

	// ============================================================================================================================
	// other

	@Override
	public final boolean initializeAndCheckIfAlive() {
		List<? extends PlanElement> planElements = this.getPlanElements();
		this.currentPlanElementIndex = 0;
		Activity firstAct = (Activity) planElements.get(0);
		double actEndTime = firstAct.getEndTime();

		this.currentLinkId = firstAct.getLinkId();
		if ((actEndTime != Time.UNDEFINED_TIME) && (planElements.size() > 1)) {
			this.activityEndTime = actEndTime ;
			this.simulation.scheduleActivityEnd(this);
			this.simulation.getAgentCounter().incLiving();
			return true;
		}
		return false; // the agent has no leg, so nothing more to do
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------
	
	@Override
	public final void endActivityAndAssumeControl(final double now) {
		Activity act = (Activity) this.getPlanElements().get(this.currentPlanElementIndex);
		this.simulation.getEventsManager().processEvent(new ActivityEndEventImpl(now, this.getPerson().getId(), act.getLinkId(), act.getFacilityId(), act.getType()));
		// note that when we are here we don't know if next is another leg, or an activity.  Therefore, we go to a general method:
		Boolean flag = advancePlan() ;
		scheduleAgentInMobsim( flag );
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------

	@Override
	public final void endLegAndAssumeControl(final double now) {

//		this.simulation.handleAgentArrival(now, this);
		this.simulation.getEventsManager().processEvent(
				this.simulation.getEventsManager().getFactory().createAgentArrivalEvent(
						now, this.getPerson().getId(), this.getDestinationLinkId(), this.getCurrentLeg().getMode()));

		if(!this.currentLinkId.equals(this.cachedDestinationLinkId)) {
			// yyyyyy needs to throw a stuck/abort event
			log.error("The agent " + this.getPerson().getId() + " has destination link " + this.cachedDestinationLinkId
					+ ", but arrived on link " + this.currentLinkId + ". Removing the agent from the simulation.");
			this.simulation.getAgentCounter().decLiving();
			this.simulation.getAgentCounter().incLost();
			return;
		}
		// note that when we are here we don't know if next is another leg, or an activity  Therefore, we go to a general method:
		Boolean flag = advancePlan() ;
		scheduleAgentInMobsim( flag );
	}

	// -----------------------------------------------------------------------------------------------------------------------------

	private Boolean scheduleAgentInMobsim( Boolean flag ) {
		
		if ( flag == null ) {
			throw new RuntimeException("plan has run empty" ) ;
		}
		
		PlanElement pe = this.getCurrentPlanElement() ;

		if (pe instanceof Activity) {

			if ((this.currentPlanElementIndex+1) < this.getPlanElements().size()) {
				// there is still at least on plan element left
				this.simulation.scheduleActivityEnd(this);
				return true ;
			} else {
				// this is the last activity
				this.simulation.getAgentCounter().decLiving();
				return false ;
			}

		} else if (pe instanceof Leg) {

			if ( flag ) {
				this.simulation.arrangeAgentDeparture(this, this.currentLinkId);
				return true ;
			} else {
				log.error("The agent " + this.getId() + " returned false from advancePlan.  Removing the ag from the mobsim ...");
				this.simulation.getAgentCounter().decLiving();
				this.simulation.getAgentCounter().incLost();
				return false ;
			}

		} else { // (presumably, this was already caught earlier)
			throw new RuntimeException("Unknown PlanElement of type: " + pe.getClass().getName());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------

	public final void notifyTeleportToLink(final Id linkId) {
		this.currentLinkId = linkId;
	}

	public final void notifyMoveOverNode() {
		this.currentLinkId = this.cachedNextLinkId;
		this.currentLinkIdIndex++;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Id chooseNextLinkId() {
		if (this.cachedNextLinkId != null) {
			return this.cachedNextLinkId;
		}
		if (this.cachedRouteLinkIds == null) {
			this.cachedRouteLinkIds = ((NetworkRoute) this.currentLeg.getRoute()).getLinkIds();
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
			return null ;
		} 
		
		PlanElement pe = this.getCurrentPlanElement() ;
		if (pe instanceof Activity) {
			initNextActivity((Activity) pe);
			return true ;
		} else if (pe instanceof Leg) {

			return initNextLeg( (Leg) pe ) ;
		} else {
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
			log.error("The agent " + this.getPerson().getId() + " has no route in its leg. Removing the agent from the simulation.");
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
	void calculateDepartureTime(Activity tmpAct) {
		double now = this.getMobsim().getSimTimer().getTimeOfDay() ;
		ActivityImpl act = (ActivityImpl) tmpAct ; // since we need the duration.  kai, aug'10

		if ( act.getDuration() == Time.UNDEFINED_TIME && (act.getEndTime() == Time.UNDEFINED_TIME)) {
			// yyyy does this make sense?  below there is at least one execution path where this should lead to an exception.  kai, oct'10
			this.activityEndTime = Double.POSITIVE_INFINITY ;
			return ;
		}
		
		double departure = 0;

		if ( this.simulation.getScenario().getConfig().vspExperimental().getActivityDurationInterpretation()
				.equals(VspExperimentalConfigGroup.MIN_OF_DURATION_AND_END_TIME) ) {
			// person stays at the activity either until its duration is over or until its end time, whatever comes first
			if (act.getDuration() == Time.UNDEFINED_TIME) {
				departure = act.getEndTime();
			} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
				departure = now + act.getDuration();
			} else {
				departure = Math.min(act.getEndTime(), now + act.getDuration());
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
			} else if ( act.getDuration() != Time.UNDEFINED_TIME ) {
				departure = now + act.getDuration() ;
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

	
	private boolean initNextLeg(final Leg leg) {
		Route route = leg.getRoute();
		if (route == null) {
			log.error("The agent " + this.getPerson().getId() + " has no route in its leg.  Returning false from initNextLeg ...");
			if ( noRouteWrnCnt < 1 ) {
				log.info( "(Route is needed inside Leg even if you want teleportation since Route carries the start/endLinkId info.)") ;
				noRouteWrnCnt++ ;
			}
			return false;
		}
		this.cachedDestinationLinkId = route.getEndLinkId();

		// set the route according to the next leg
		this.currentLeg = leg;
		this.cachedRouteLinkIds = null;
		this.currentLinkIdIndex = 0;
		this.cachedNextLinkId = null;
		return true ;
	}
	private static int noRouteWrnCnt = 0 ;
	
	private void initNextActivity(final Activity act) {
		double now = this.getMobsim().getSimTimer().getTimeOfDay() ;
		this.simulation.getEventsManager().processEvent(new ActivityStartEventImpl(now, this.getPerson().getId(),  this.currentLinkId, act.getFacilityId(), act.getType()));
		/* schedule a departure if either duration or endtime is set of the activity.
		 * Otherwise, the agent will just stay at this activity for ever...
		 */
		calculateDepartureTime(act);
	}

	/**
	 * Convenience method delegating to person's selected plan
	 * @return list of {@link ActivityImpl}s and {@link LegImpl}s of this agent's plan
	 */
	public final List<PlanElement> getPlanElements() {
		return Collections.unmodifiableList( this.person.getSelectedPlan().getPlanElements() );
	}
	
	public final Mobsim getMobsim(){
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
	public final void setVehicle(final QVehicle veh) {
		// yyyy something like this makes sense but does it need to be "Q"Vehicle?  kai, oct'10
		this.vehicle = veh;
	}

	@Override
	public final QVehicle getVehicle() {
		// yyyy something like this makes sense but does it need to be "Q"Vehicle?  kai, oct'10
		return this.vehicle;
	}

	@Override
	public final double getActivityEndTime() {
		// yyyyyy I don't think there is any guarantee that this entry is correct after an activity end re-scheduling.  kai, oct'10
		return this.activityEndTime;
	}

//	private void setDepartureTime(final double seconds) {
//		this.activityDepartureTime = seconds;
//	}

	@Override
	public final Id getCurrentLinkId() {
		// note: the method is really only defined for DriverAgent!  kai, oct'10
		return this.currentLinkId;
	}

	@Override
	public final Leg getCurrentLeg() {
		PlanElement currentPlanElement = this.getCurrentPlanElement(); 
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return (Leg) currentPlanElement;
	}

	public final Activity getCurrentActivity() {
		PlanElement currentPlanElement = this.getCurrentPlanElement(); 
		if (!(currentPlanElement instanceof Activity)) {
			return null;
		}
		return (Activity) currentPlanElement;
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

}
