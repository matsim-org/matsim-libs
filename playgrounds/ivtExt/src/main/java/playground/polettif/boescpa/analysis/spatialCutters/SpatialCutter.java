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

package playground.polettif.boescpa.analysis.spatialCutters;

import org.matsim.api.core.v01.network.Link;

/**
 * @author boescpa
 */
public interface SpatialCutter {

	/**
	 * Checks if a link is in a given area.
	 *
	 * @param link
	 * @return true if the link is in the area, false if not.
	 */
	public boolean spatiallyConsideringLink(Link link);

	/**
	 * @return A description of the considered area.
	 */
	public String toString();
}
