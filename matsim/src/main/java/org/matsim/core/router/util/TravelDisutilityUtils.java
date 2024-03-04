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
package org.matsim.core.router.util;

import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;

/**
 * @author nagel
 *
 */
public final class TravelDisutilityUtils {
	private TravelDisutilityUtils(){} // do not instantiate

	public static TravelDisutility createFreespeedTravelTimeAndDisutility(ScoringConfigGroup cnScoringGroup) {
		return new FreespeedTravelTimeAndDisutility(cnScoringGroup) ;
	}

}
