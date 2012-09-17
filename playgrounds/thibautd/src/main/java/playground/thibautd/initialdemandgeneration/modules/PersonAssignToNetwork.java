/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.thibautd.initialdemandgeneration.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Sets activity links to the facility link, and than, if it is asked, routes the plans.
 *
 * @author thibautd
 */
public class PersonAssignToNetwork extends AbstractPersonAlgorithm implements PlanAlgorithm {

/////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignToNetwork.class);

	private final PlansCalcRoute router;
	private final ActivityFacilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * initialises the module without routing capability
	 */
	public PersonAssignToNetwork(
			final Network network,
			final ActivityFacilities facilities) {
		this( network , facilities , null , null );
	}

	/**
	 * initialises the module with routing capability
	 */
	public PersonAssignToNetwork(
			final Network network,
			final ActivityFacilities facilities,
			final Config config,
			final ModeRouteFactory routeFactory) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		if (routeFactory != null) {
			log.info( "    plans will be routed" );
			FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
			this.router = new PlansCalcRoute(
					config.plansCalcRoute(),
					network,
					timeCostCalc,
					timeCostCalc,
					new AStarLandmarksFactory(network, timeCostCalc),
					routeFactory);
		}
		else {
			log.info( "    plans will NOT be routed" );
			this.router = null;
		}
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	private static final void removeRoutes(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				((Leg) pe).setRoute( null );
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				act.setLinkId(((ActivityFacilityImpl) this.facilities.getFacilities().get(act.getFacilityId())).getLinkId());
			}
		}
		if ( router != null ) {
			router.run(plan);
		}
		else {
			removeRoutes( plan );
		}
	}
}
