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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.api.core.v01.network.Link;


public class Tasks
{
    public static Link getBeginLink(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getPath().getFromLink();

            case STAY:
                return ((StayTask)task).getLink();

            default:
                throw new IllegalStateException("Only: DRIVE or STAY");
        }
    }


    public static Link getEndLink(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getPath().getToLink();

            case STAY:
                return ((StayTask)task).getLink();

            default:
                throw new IllegalStateException("Only: DRIVE or STAY");
        }
    }
}
