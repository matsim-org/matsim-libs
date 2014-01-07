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

package pl.poznan.put.vrp.dynamic.chart;

import java.util.List;

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.data.model.Localizable;
import pl.poznan.put.vrp.dynamic.data.schedule.*;

import com.google.common.collect.Lists;


public class LinkSources
{
    public static abstract class AbstractLinkSource<T>
        implements LinkSource
    {
        private List<T> list;


        public AbstractLinkSource(List<T> list)
        {
            this.list = list;
        }


        @Override
        public int getCount()
        {
            return list.size();
        }


        @Override
        public Link getLink(int index)
        {
            return getLink(list.get(index));
        }


        protected abstract Link getLink(T item);
    }


    public static LinkSource createFromLinks(final List<Link> links)
    {
        return new AbstractLinkSource<Link>(links) {
            protected Link getLink(Link item)
            {
                return item;
            };
        };
    }


    public static <T extends Localizable> LinkSource createFromLocalizables(
            final List<T> localizables)
    {
        return new AbstractLinkSource<T>(localizables) {
            protected Link getLink(T item)
            {
                return item.getLink();
            };
        };
    }


    // n DriveTasks -> n+1 Links
    public static LinkSource createFromDriveTasks(final List<DriveTask> tasks)
    {
        return new LinkSource() {

            @Override
            public Link getLink(int item)
            {
                if (item == 0) {
                    return tasks.get(0).getShortestPath().getFromLink();
                }

                return tasks.get(item - 1).getShortestPath().getToLink();
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
    public static LinkSource createLinkSource(Schedule<?> schedule)
    {
        List<DriveTask> tasks = Lists.newArrayList(Schedules.createDriveTaskIter(schedule));
        return LinkSources.createFromDriveTasks(tasks);
    }
}
