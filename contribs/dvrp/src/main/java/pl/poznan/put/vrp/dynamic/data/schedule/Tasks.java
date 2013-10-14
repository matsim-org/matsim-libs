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

package pl.poznan.put.vrp.dynamic.data.schedule;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class Tasks
{
    public static Vertex getBeginVertex(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getArc().getFromVertex();
            case STAY:
                return ((StayTask)task).getVertex();
            default:
                throw new IllegalStateException("Only: DRIVE or STAY");
        }
    }


    public static Vertex getEndVertex(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getArc().getToVertex();
            case STAY:
                return ((StayTask)task).getVertex();
            default:
                throw new IllegalStateException("Only: DRIVE or STAY");
        }
    }
}
