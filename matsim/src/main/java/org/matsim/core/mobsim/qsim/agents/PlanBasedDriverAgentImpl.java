/* *********************************************************************** *
 * project: org.matsim.*												   *
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
public final class PlanBasedDriverAgentImpl implements DriverAgent {

	private static final Logger log = Logger.getLogger(PlanBasedDriverAgentImpl.class);

	private BasicPlanAgentImpl basicPlanAgentDelegate;

	public PlanBasedDriverAgentImpl(BasicPlanAgentImpl basicAgent ) {
		this.basicPlanAgentDelegate = basicAgent ;
	}
	private static int expectedLinkWarnCount = 0;

	private Id<Link> cachedNextLinkId = null;

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		if (expectedLinkWarnCount < 10 && !newLinkId.equals(this.cachedNextLinkId)) {
			log.warn("Agent did not end up on expected link. Ok for within-day replanning agent, otherwise not.  Continuing " +
					"anyway ... This warning is suppressed after the first 10 warnings.") ;
			expectedLinkWarnCount++;
		}
		Gbl.assertNonNull(newLinkId);
		this.basicPlanAgentDelegate.setCurrentLinkId( newLinkId ) ;
		this.basicPlanAgentDelegate.incCurrentLinkIndex();
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
		// very extremely messy.  kai, nov'14 (This behavior is no longer there. kai, nov'14)

		// Please, let's try, amidst all checking and caching, to have this method return the same thing
		// if it is called several times in a row. Otherwise, you get Heisenbugs.
		// I just fixed a situation where this method would give a warning about a bad route and return null
		// the first time it is called, and happily return a link id when called the second time.  michaz 2013-08

		// Agreed.  One should also not assume that anything here is the result of one consistent design process.  Rather, many people added
		// and removed material as they needed it for their own studies.  Making the whole code more consistent would be highly
		// desirable.  kai, nov'14

		// (1) if there is a cached link id, use that one: 
		if (this.cachedNextLinkId != null && !(this.cachedNextLinkId.equals(this.getCurrentLinkId())) ) {
			// cachedNextLinkId used to be set to null when a leg started.  Now the BasicPlanAgentImpl does not longer have access to cached
			// value.  kai, nov'14

			return this.cachedNextLinkId;
		}

		// (2) routes that are not network routes cannot be interpreted
		if ( ! ( this.basicPlanAgentDelegate.getCurrentLeg().getRoute() instanceof NetworkRoute ) ) {
			return null ;
		}

		List<Id<Link>> routeLinkIds = ((NetworkRoute) this.basicPlanAgentDelegate.getCurrentLeg().getRoute()).getLinkIds();
		
		// (3) if route has run dry, we return the destination link (except for one special case, which however may not be necessary any more):
		if (this.basicPlanAgentDelegate.getCurrentLinkIndex() >= routeLinkIds.size() ) {

			// special case:
			if (this.getCurrentLinkId().equals( this.getDestinationLinkId() )  && this.basicPlanAgentDelegate.getCurrentLinkIndex() > routeLinkIds.size()) {
				// this can happen if the last link in a route is a loop link. Don't ask, it can happen in special transit simulation cases... mrieser/jan2014

				// the condition for arrival used to be "route has run dry AND destination link not attached to current link".  now with loop links,
				// this condition was not triggered.  So no wonder that for such cases a special condition was needed.  kai, nov'14
				
				// The special condition may not be necessary any more. kai, nov'14

				return null;
			}
			
			this.cachedNextLinkId = this.getDestinationLinkId();
			return this.cachedNextLinkId;

		}

		// (4) otherwise (normal case): return the next link of the plan (after caching it):
		this.cachedNextLinkId = routeLinkIds.get(this.basicPlanAgentDelegate.getCurrentLinkIndex());
		return this.cachedNextLinkId;
		
	}

	@Override
	public final boolean isWantingToArriveOnCurrentLink( ) {
		
		if ( ! ( this.basicPlanAgentDelegate.getCurrentLeg().getRoute() instanceof NetworkRoute ) ) {
			// non-network links in the past have always returned true (i.e. "null" to the chooseNextLink question). kai, nov'14
			return true ;
		}

		final List<Id<Link>> routeLinkIds = ((NetworkRoute) this.basicPlanAgentDelegate.getCurrentLeg().getRoute()).getLinkIds();
		final int routeLinkIdsSize = routeLinkIds.size();
		
		// the standard condition used to be "route has run dry AND destination link not attached to current link":
		// 2nd condition essentially meant "destination link EQUALS current link" but really stupid way of stating this.  Thus
		// changing the second condition to "being at destination".  This breaks old code; I will fix as far as it is covered by tests.  kai, nov'14
		if ( this.basicPlanAgentDelegate.getCurrentLinkIndex() >= routeLinkIdsSize && this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {
			return true ;
		}
		
// the following are possible consistency checks on which previous code may have relied. Relatively expensive because of hash map lookups. kai, nov'14
			
//		Link currentLink = this.getScenario().getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
//		Link destinationLink = this.getScenario().getNetwork().getLinks().get( this.getDestinationLinkId() ) ;
//			
//		if ( this.currentLinkIndex >= routeLinkIdsSize ) { // route has run dry
//			if ( currentLink.getToNode() == destinationLink.getFromNode() ) { // will arrive on next link
//				return false ;
//			} else { // will not arrive on next link
//				log.error("route has run dry, and destination link is not attached to next node.  In consequence, vehicle has no chance to continue "
//						+ "correctly.  Make it arrive here rather than explode it at the intersection.") ;
//				return true ;
//			}
//		}
			
//		Link nextLink = this.getScenario().getNetwork().getLinks().get( routeLinkIds.get(this.getCurrentLinkIndex()) ) ;
//		
//		if ( currentLink.getToNode() != nextLink.getFromNode() ) {
//			log.error("route is inconsistent.  In consequence, vehicle has no chance to continue correctly." +  
//					"Make it arrive here rather than explode it at the intersection." ) ;
//			return true ;
//		}
			
		return false ;
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
		
		if( this.basicPlanAgentDelegate.getCurrentPlanElement()==null ) {
			throw new RuntimeException("encountered unexpected null pointer" ) ;
		}

		if (this.basicPlanAgentDelegate.getCurrentPlanElement() instanceof Leg) {
			if (basicPlanAgentDelegate.getCurrentLeg().getRoute() == null) {
				log.error("The agent " + this.getId() + " has no route in its leg. Setting agent state to abort." );
				this.basicPlanAgentDelegate.setState(MobsimAgent.State.ABORT) ;
			}
		} 
//		else {			
//			this.basicPlanAgentDelegate.calculateAndSetDepartureTime((Activity) this.basicPlanAgentDelegate.getCurrentPlanElement());
//		}
		this.basicPlanAgentDelegate.resetCaches(); 
	}

	@Override
	public Id<Person> getId() {
		return this.basicPlanAgentDelegate.getId();
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.basicPlanAgentDelegate.getCurrentLinkId() ;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.basicPlanAgentDelegate.getDestinationLinkId() ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.basicPlanAgentDelegate.setVehicle( veh );
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.basicPlanAgentDelegate.getVehicle() ;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return this.basicPlanAgentDelegate.getPlannedVehicleId() ;
	}

	@Override
	public String getMode() {
		return this.basicPlanAgentDelegate.getMode() ;
	}

}
