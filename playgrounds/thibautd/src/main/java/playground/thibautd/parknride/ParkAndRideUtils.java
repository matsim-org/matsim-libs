/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.utils.RoutingUtils;

import java.util.Iterator;
import java.util.List;


/**
 * @author thibautd
 */
public class ParkAndRideUtils {
	public final static String PNR_TRIP_FLAG = "pnr";
	private ParkAndRideUtils() {}

	/**
	 * Loads a scenario defined by a config file, including park and ride facilities
	 * @param config the config
	 * @return the loaded scenario
	 */
	public static Scenario loadScenario(final Config config) {
		Scenario scen = ScenarioUtils.loadScenario( config );

		ParkAndRideConfigGroup configGroup = getConfigGroup( config );

		ParkAndRideFacilitiesXmlReader reader = new ParkAndRideFacilitiesXmlReader();
		reader.parse( configGroup.getFacilities() );

		scen.addScenarioElement( ParkAndRideFacilities.ELEMENT_NAME , reader.getFacilities() );

		return scen;
	}

	/**
	 * convenience method which adds pnr related config group(s)
	 */
	public static void setConfigGroup(final Config config) {
		config.addModule( new ParkAndRideConfigGroup() );
	}

	/**
	 * Convnience method to get the config group
	 * @param config
	 * @return
	 */
	public static ParkAndRideConfigGroup getConfigGroup(final Config config) {
		return (ParkAndRideConfigGroup) config.getModule( ParkAndRideConfigGroup.GROUP_NAME );
	}

	/**
	 * Convenience method to get the facilities.
	 * @param scenario
	 * @return
	 */
	public static ParkAndRideFacilities getParkAndRideFacilities(final Scenario scenario) {
		return (ParkAndRideFacilities) scenario.getScenarioElement( ParkAndRideFacilities.ELEMENT_NAME );
	}

	/**
	 * extracts the plan structure considering pnr trips as individual
	 * trips with mode {@link TransportMode#pt}.
	 */
	public static List<PlanElement> extractPlanStructure(
			final TripRouter tripRouter,
			final StageActivityTypes pnrTypes,
			final Plan plan) {
		List<PlanElement> planStructure =
				RoutingUtils.tripsToLegs(
						plan,
						tripRouter.getStageActivityTypes(),
						new MainModeIdentifierImpl());

		// then, remove park and ride trips, and mark them as pt
		Iterator<PlanElement> iter = planStructure.iterator();
		Leg lastLeg = null;
		for (PlanElement pe = iter.next(); iter.hasNext(); pe = iter.next()) {
			if (pe instanceof Activity  &&
					pnrTypes.isStageActivity(
						((Activity) pe).getType() )) {
				// remove the activity
				iter.remove();
				// remove the following leg
				iter.next();
				iter.remove();
				// set the trip to pnr
				lastLeg.setMode( PNR_TRIP_FLAG );
			}
			if (pe instanceof Leg) {
				lastLeg = (Leg) pe;
			}
		}

		return planStructure;
	}


}

