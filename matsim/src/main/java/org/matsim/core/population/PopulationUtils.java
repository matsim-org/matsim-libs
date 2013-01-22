/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.utils.misc.Time;

/**
 * @author nagel
 *
 */
public class PopulationUtils {
	private PopulationUtils() {} // container for static methods, not meant to be instantiated.
	
	/**
	 * Convenience method to compute (expected or planned) activity end time, depending on the different time interpretations.
	 * <p/>
	 * Design comments:<ul>
	 * <li> The whole Config is part of the argument, since it may eventually make sense to move the config parameter from VspExperimental to
	 * some more regular place.  kai, jan'13
	 * </ul>
	 */
	public static double getActivityEndTime( Activity act, double now, Config config ) {
		switch ( config.vspExperimental().getActivityDurationInterpretation() ) {
		case endTimeOnly:
			return act.getEndTime() ;
		case tryEndTimeThenDuration:
			if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
				return act.getEndTime() ;
			} else if ( act.getMaximumDuration() != Time.UNDEFINED_TIME ) {
				return now + act.getMaximumDuration() ;
			} else {
				return Time.UNDEFINED_TIME ;
			}
		case minOfDurationAndEndTime:
			return Math.min( now + act.getMaximumDuration() , act.getEndTime() ) ;
		}
		return Time.UNDEFINED_TIME ;
	}

}
