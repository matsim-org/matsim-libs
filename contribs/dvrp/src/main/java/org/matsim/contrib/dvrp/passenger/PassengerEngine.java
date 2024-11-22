/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package org.matsim.contrib.dvrp.passenger;

import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;

public interface PassengerEngine extends DistributedMobsimEngine, DistributedDepartureHandler, PassengerHandler, MobsimScopeEventHandler {
}
