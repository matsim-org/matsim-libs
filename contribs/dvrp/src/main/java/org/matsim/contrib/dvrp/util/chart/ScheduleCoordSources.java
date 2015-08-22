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

package org.matsim.contrib.dvrp.util.chart;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.util.chart.CoordDataset.CoordSource;

import com.google.common.collect.Lists;


public class ScheduleCoordSources
{
    // n DriveTasks -> n+1 Links
    public static CoordSource createCoordSource(final List<DriveTask> tasks)
    {
        return new CoordSource() {

            @Override
            public Coord getCoord(int item)
            {
                if (item == 0) {
                    return tasks.get(0).getPath().getFromLink().getCoord();
                }

                return tasks.get(item - 1).getPath().getToLink().getCoord();
            }


            @Override
            public int getCount()
            {
                int size = tasks.size();
                return size == 0 ? 0 : size + 1;
            }
        };
    }


    // Schedule -> n DriveTasks -> n+1 Links
    public static CoordSource createCoordSource(Schedule<?> schedule)
    {
        List<DriveTask> driveTasks = Lists.newArrayList(Schedules.createDriveTaskIter(schedule));
        return ScheduleCoordSources.createCoordSource(driveTasks);
    }
}
