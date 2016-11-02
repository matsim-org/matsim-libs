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
package org.matsim.core.api.internal;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacilityImpl;

/**
 * Marker interface for data class implementations such as {@link Person}, {@link Activity}, or {@link ActivityFacilityImpl}.
 * <p></p>
 * Currently (feb'16) we think that<ul>
 * <li> methods in these implementations should be final
 * <li> the implementation classes themselves need not be final
 * <li> constructors should be protected.
 * </ul>
 * This would allow implementers to extend these classes.
 * 
 * @author nagel
 * 
 * @see MatsimToplevelContainer
 *
 */
public interface MatsimDataClassImplMarkerInterface {

}
