/* *********************************************************************** *
 * project: org.matsim.*
 * TollTools.java
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
package playground.yu.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author yu
 * 
 */
public class TollTools {

	/**
	 * @param loc
	 * @param toll
	 * @return a boolean value, whether a <code>Link</code> belongs to toll
	 *         area.
	 */
	public static boolean isInRange(Id linkId, RoadPricingScheme toll) {
		return toll.getTolledLinkIds().contains(linkId);
	}

}
