/* *********************************************************************** *
 * project: org.matsim.*
 * LegRouter.java
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

package playground.johannes.coopsim.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

/**
 * @deprecated this is part of the old (mono-leg trip) approach.
 * Use the RoutingModule interface instead.
 */
@Deprecated // use RoutingModule (with TripRouter) instead. kai, mar'15
public interface LegRouter {

	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime);

}
