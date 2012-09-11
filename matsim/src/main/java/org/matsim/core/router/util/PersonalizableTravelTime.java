/* *********************************************************************** *
 * project: org.matsim.*
 * PersonalizableTravelTime.java
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

package org.matsim.core.router.util;


@Deprecated
/**
 * This is a very bad interface, because it led to the following process:
 * 
 * 1.) If a module knows about a Person and deals with TravelTime, it will require the TravelTime to be Personalizable,
 * (because casting is considered bad), so it can tell the TravelTime about the Person. This means that suddenly all 
 * TravelTimes have to be Personalizable, even though they may not take Persons into account. So the Person
 * is not really optional, but pseudo-optional. Then one may ask why the Person is not in the TravelTime query in the first place.
 * (Where it probably should be).
 * 
 * 2.) It effectively forces all implementations to be stateful and inherently non-thread-safe.
 * 
 * 3.) As soon as something like this is in place, it will _instantly_ (has already happened) be imitated for other,
 * similar things and the horror proliferates.
 * 
 * I take full responsibility for this. ;-) Please do not use this interface for any new code.
 * 
 * @author michaz
 *
 */
public interface PersonalizableTravelTime extends TravelTime {
	

}
