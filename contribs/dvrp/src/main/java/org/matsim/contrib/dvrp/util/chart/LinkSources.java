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

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.schedule.*;

import com.google.common.collect.Lists;


public class LinkSources
{
    public static abstract class AbstractCoordSource<T>
        implements CoordSource
    {
        private List<T> list;


        public AbstractCoordSource(List<T> list)
        {
            this.list = list;
        }


        @Override
        public int getCount()
        {
            return list.size();
        }


        @Override
        public Coord getCoord(int index)
        {
            return getCoord(list.get(index));
        }


        protected abstract Coord getCoord(T item);
    }


    public static <T extends BasicLocation<T>> CoordSource createFromBasicLocations(
            final List<T> basicLocations)
    {
        return new AbstractCoordSource<T>(basicLocations) {
            protected Coord getCoord(T item)
            {
                return item.getCoord();
            };
        };
    }


    // n DriveTasks -> n+1 Links
    public static CoordSource createFromDriveTasks(final List<DriveTask> tasks)
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
    public static CoordSource createLinkSource(Schedule<?> schedule)
    {
        List<DriveTask> tasks = Lists.newArrayList(Schedules.createDriveTaskIter(schedule));
        return LinkSources.createFromDriveTasks(tasks);
    }
}
