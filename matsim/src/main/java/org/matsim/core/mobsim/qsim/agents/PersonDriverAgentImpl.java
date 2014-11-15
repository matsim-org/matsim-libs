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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author dgrether, nagel
 * <p/>
 * I think this class is reasonable in terms of what is public and/or final and what not.
 */
public class PersonDriverAgentImpl extends BasicPlanAgentImpl implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent {
	// yy cannot make this final since it is overridden at 65 locations
	// (but since all methods are final, it seems that all of these can be solved by delegation).
	// kai, nov'14

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private static int expectedLinkWarnCount = 0;

	private Id<Link> cachedNextLinkId = null;

	private int currentLinkIdIndex = 0 ;

	

	// ============================================================================================================================
	// c'tor

	public PersonDriverAgentImpl(final Plan plan, final Netsim simulation) {
		super(plan, simulation.getScenario(), simulation.getEventsManager(), simulation.getSimTimer()) ;
		// deliberately does NOT keep a back pointer to the whole Netsim; this should also be removed in the constructor call.
		// yy should we keep the back pointer to the simTimer?  Without it, we need to pass more simulation times around but it might be nice to do it
		// in that way. kai, nov'14

		// I am moving the VehicleUsingAgent functionality into the basicAgentDelegate since a separate VehicleUsingAgentImpl to only
		// pass into DriverAgent and PassengerAgent seems overkill.  kai, nov'14

		List<? extends PlanElement> planElements = this.getCurrentPlan().getPlanElements();
		if (planElements.size() > 0) {
			Activity firstAct = (Activity) planElements.get(0);				
			this.setCurrentLinkId( firstAct.getLinkId() ) ;
			this.setState(MobsimAgent.State.ACTIVITY) ;
			calculateAndSetDepartureTime(firstAct);
		}
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		if (expectedLinkWarnCount < 10 && !newLinkId.equals(this.cachedNextLinkId)) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ... This warning is suppressed after the first 10 warnings.") ;
			expectedLinkWarnCount++;
		}
		this.setCurrentLinkId( newLinkId ) ;
//		this.setCurrentLinkIdIndex(this.getCurrentLinkIdIndex() + 1);
		this.currentLinkIdIndex++ ;
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

		if (this.cachedNextLinkId != null && !this.cachedNextLinkId.equals(this.getCurrentLinkId()) ) {
			// cachedNextLinkId used to be set to null when a leg started.  Now the BasicPlanAgentImpl does not longer have access to cached
			// value.  kai, nov'14

			return this.cachedNextLinkId;
		}

		if ( ! ( this.getCurrentLeg().getRoute() instanceof NetworkRoute ) ) {
			return null ;
		}

		List<Id<Link>> routeLinkIds = ((NetworkRoute) this.getCurrentLeg().getRoute()).getLinkIds();

		if (this.getCurrentLinkIdIndex() >= routeLinkIds.size() ) {
			// we have no more information from the routeLinkIds

			Link currentLink = this.getScenario().getNetwork().getLinks().get(this.getCurrentLinkId());
			Link destinationLink = this.getScenario().getNetwork().getLinks().get(this.getDestinationLinkId());

			// special case:
			if (currentLink == destinationLink && this.getCurrentLinkIdIndex() > routeLinkIds.size()) {
				// this can happen if the last link in a route is a loop link. Don't ask, it can happen in special transit simulation cases... mrieser/jan2014

				// the condition for arrival currently is "route has run dry AND destination link not attached to current link".  now with loop links,
				// this condition is never triggered.  So no wonder that for such cases currently a special condition is needed.  kai, nov'14

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


		Id<Link> nextLinkId = routeLinkIds.get(this.getCurrentLinkIdIndex());
		Link currentLink = this.getScenario().getNetwork().getLinks().get(this.getCurrentLinkId());
		Link nextLink = this.getScenario().getNetwork().getLinks().get(nextLinkId);
		if (currentLink.getToNode().equals(nextLink.getFromNode())) {
			this.cachedNextLinkId = nextLinkId; //save time in later calls, if link is congested
			return this.cachedNextLinkId;
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.getCurrentLinkIdIndex() + " ]");
		// yyyyyy personally, I would throw some kind of abort event here.  kai, aug'10
		return null;
	}

	@Override
	public final boolean isArrivingOnCurrentLink( ) {
		if ( ! ( this.getCurrentLeg().getRoute() instanceof NetworkRoute ) ) {
			// non-network links in the past have always returned true (i.e. "null" to the chooseNextLink question). kai, nov'14
			return true ;
		}

		final int routeLinkIdsSize = ((NetworkRoute) this.getCurrentLeg().getRoute()).getLinkIds().size();

		// the standard condition is "route has run dry AND destination link not attached to current link":
		// 2nd condition essentially means "destination link EQUALS current link" but really stupid way of stating this.  Thus
		// changing the second condition for the time being to "being at destination". kai, nov'14
		if ( this.getCurrentLinkIdIndex() >= routeLinkIdsSize && this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {

			this.currentLinkIdIndex = 0 ; 
			// (the above is not so great; should be done at departure; but there is nothing there to notify the DriverAgent at departure ...  kai, nov'14)

			return true ;
		} else {
			return false ;
		}

	}


	// ============================================================================================================================
	// below there only (package-)private methods or setters/getters

	/**
	 * Some data of the currently simulated Leg is cached to speed up
	 * the simulation. If the Leg changes (for example the Route or
	 * the Destination Link), those cached data has to be reseted.
	 *</p>
	 * If the Leg has not changed, calling this method should have no effect
	 * on the Results of the Simulation!
	 */
	/* package */ final void resetCaches() {

		// moving this method not to WithinDay for the time being since it seems to make some sense to keep this where the internals are
		// known best.  kai, oct'10
		// Compromise: package-private here; making it public in the Withinday class.  kai, nov'10

		this.cachedNextLinkId = null;

		/*
		 * The Leg may have been exchanged in the Person's Plan, so
		 * we update the Reference to the currentLeg Object.
		 */
		if (this.getCurrentPlanElement() instanceof Leg) {
			if (getCurrentLeg().getRoute() == null) {
				log.error("The agent " + this.getId() + " has no route in its leg. Setting agent state to abort." );
				this.setState(MobsimAgent.State.ABORT) ;
			}
		} else {			
			this.calculateAndSetDepartureTime((Activity) this.getCurrentPlanElement());
		}
	}

	final int getCurrentLinkIdIndex() {
		return currentLinkIdIndex;
	}

}
