/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface WakeupGenerator {
	List<Pair<Double, ActivityEngineWithWakeup.AgentWakeup>> generateWakeups(MobsimAgent agent);
}
