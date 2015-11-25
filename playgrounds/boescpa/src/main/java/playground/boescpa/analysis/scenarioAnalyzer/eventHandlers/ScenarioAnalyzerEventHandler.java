/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.analysis.scenarioAnalyzer.eventHandlers;

import org.matsim.core.events.handler.EventHandler;
import playground.boescpa.analysis.scenarioAnalyzer.spatialFilters.SpatialEventCutter;

/**
 * Any new analysis to be done as part of the ScenarioAnalyzer-process has to implement this interface.
 *
 * @author boescpa
 */
public interface ScenarioAnalyzerEventHandler extends EventHandler {
	/**
	 * @param spatialEventCutter
	 * @return Results of the analysis in form of a (multiline) string.
	 */
	public String createResults(SpatialEventCutter spatialEventCutter, int scaleFactor);
}
