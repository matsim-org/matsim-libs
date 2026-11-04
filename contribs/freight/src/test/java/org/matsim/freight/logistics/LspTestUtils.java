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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class LspTestUtils {
	/**
	 * @return {@code true} if both endpoints of {@code link} lie within the axis-aligned box spanned by {@code minCoord}/{@code maxCoord}
	 */
	public static boolean isWithinBound(Link pendingFromLink, Coord minCoord, Coord maxCoord) {
		Coord fromNodeCoord = pendingFromLink.getFromNode().getCoord();
		Coord toNodeCoord = pendingFromLink.getToNode().getCoord();
		return fromNodeCoord.getX() >= minCoord.getX() &&
			fromNodeCoord.getX() <= maxCoord.getX() &&
			fromNodeCoord.getY() >= minCoord.getY() &&
			fromNodeCoord.getY() <= maxCoord.getY() &&
			toNodeCoord.getX() >= minCoord.getX() &&
			toNodeCoord.getX() <= maxCoord.getX() &&
			toNodeCoord.getY() >= minCoord.getY() &&
			toNodeCoord.getY() <= maxCoord.getY();
	}
}
