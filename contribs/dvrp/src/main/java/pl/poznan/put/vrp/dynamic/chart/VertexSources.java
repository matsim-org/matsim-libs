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

import pl.poznan.put.vrp.dynamic.data.model.Localizable;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;

import com.google.common.collect.Lists;


public class VertexSources
{
    public static abstract class AbstractVertexSource<T>
        implements VertexSource
    {
        private List<T> list;


        public AbstractVertexSource(List<T> list)
        {
            this.list = list;
        }


        @Override
        public int getCount()
        {
            return list.size();
        }


        @Override
        public Vertex getVertex(int index)
        {
            return getVertex(list.get(index));
        }


        protected abstract Vertex getVertex(T item);
    }


    public static VertexSource createFromVertices(final List<Vertex> vertices)
    {
        return new AbstractVertexSource<Vertex>(vertices) {
            protected Vertex getVertex(Vertex item)
            {
                return item;
            };
        };
    }


    public static <T extends Localizable> VertexSource createFromLocalizables(
            final List<T> localizables)
    {
        return new AbstractVertexSource<T>(localizables) {
            protected Vertex getVertex(T item)
            {
                return item.getVertex();
            };
        };
    }


    // n DriveTasks -> n+1 Vertices
    public static VertexSource createFromDriveTasks(final List<DriveTask> tasks)
    {
        return new VertexSource() {

            @Override
            public Vertex getVertex(int item)
            {
                if (item == 0) {
                    return tasks.get(0).getArc().getFromVertex();
                }

                return tasks.get(item - 1).getArc().getToVertex();
            }


            @Override
            public int getCount()
            {
                int size = tasks.size();
                return size == 0 ? 0 : size + 1;
            }
        };
    }


    // Schedule -> n DriveTasks -> n+1 Vertices
    public static VertexSource createVertexSource(Schedule schedule)
    {
        List<DriveTask> tasks = Lists.newArrayList(Schedules.createDriveTaskIter(schedule));
        return VertexSources.createFromDriveTasks(tasks);
    }
}
