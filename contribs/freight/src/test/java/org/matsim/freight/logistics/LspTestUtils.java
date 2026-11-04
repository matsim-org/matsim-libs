/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                          *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 *
 *                                                                           *
 *   *********************************************************************** *
 */

package org.matsim.freight.logistics;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.Coord;

public class LspTestUtils {

	/**
	 * @return {@code true} if both endpoints of {@code link} lie within the axis-aligned box spanned by {@code minCoord}/{@code maxCoord}
	 */
	public static boolean isWithinBound(Link link, Coord minCoord, Coord maxCoord) {
		Coord fromNodeCoord = link.getFromNode().getCoord();
		Coord toNodeCoord = link.getToNode().getCoord();
		return isWithinBound(fromNodeCoord, minCoord, maxCoord) && isWithinBound(toNodeCoord, minCoord, maxCoord);
	}

	private static boolean isWithinBound(Coord coord, Coord minCoord, Coord maxCoord) {
		return coord.getX() >= minCoord.getX() && coord.getX() <= maxCoord.getX()
				&& coord.getY() >= minCoord.getY() && coord.getY() <= maxCoord.getY();
	}

	/**
	 * Loads the LSPs from the configuration-file setup used throughout the logistics consistency tests.
	 *
	 * @param inputDirectory directory containing the carriers, vehicle-types and LSP input files
	 * @param lspsFileName   the LSP file to load (e.g. {@code "lsps.xml"} or {@code "lsps_fail.xml"})
	 * @return the configured {@link Scenario} with its LSPs loaded
	 */
	public static Scenario loadScenario(String inputDirectory, String lspsFileName) {
		Config config = ConfigUtils.createConfig();

		FreightLogisticsConfigGroup logisticsConfigGroup = ConfigUtils.addOrGetModule(config, FreightLogisticsConfigGroup.class);
		logisticsConfigGroup.setLspsFile(inputDirectory + lspsFileName);

		FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(inputDirectory + "carriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(inputDirectory + "vehicles.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);
		LSPUtils.loadLspsAccordingToConfig(scenario);
		return scenario;
	}
}
