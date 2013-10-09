/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.tsp;

import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


public class TSPActionCreator
    implements VrpAgentLogic.ActionCreator
{
    @Override
    public DynAction createAction(Task task, double now)
    {
        return new VrpDynLeg((DriveTask)task);
    }
}
