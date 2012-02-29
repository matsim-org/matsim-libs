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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class ParkAndRideUtils {
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

		scen.addScenarioElement( reader.getFacilities() );

		return scen;
	}

	/**
	 * convenience method which adds pnr related config group(s)
	 */
	public static void setConfigGroup(final Config config) {
		config.addModule( ParkAndRideConfigGroup.GROUP_NAME , new ParkAndRideConfigGroup() );
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
		return scenario.getScenarioElement( ParkAndRideFacilities.class );
	}
}

