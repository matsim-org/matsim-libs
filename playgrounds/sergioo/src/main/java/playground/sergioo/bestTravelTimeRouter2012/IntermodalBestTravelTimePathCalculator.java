/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.sergioo.bestTravelTimeRouter2012;
//package org.matsim.core.router;

import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
//import org.matsim.core.router.util.BestTravelTimePathCalculator;

public interface IntermodalBestTravelTimePathCalculator extends BestTravelTimePathCalculator {

	/**
	 * Restricts the router to only use links that have at least on of the given modes set as allowed.
	 * Set to <code>null</code> to disable any restrictions, i.e. to use all available modes.
	 *
	 * @param modeRestriction {@link TransportMode}s that can be used to find a route
	 *
	 * @see Link#setAllowedModes(Set)
	 */
	public void setModeRestriction(final Set<String> modeRestriction);

}