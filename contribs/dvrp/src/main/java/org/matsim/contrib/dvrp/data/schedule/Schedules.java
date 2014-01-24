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

package org.matsim.contrib.dvrp.data.schedule;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.data.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.data.schedule.Task.TaskType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;


public class Schedules
{
    public static final Predicate<Task> STAY_TASK_PREDICATE = new Predicate<Task>() {
        public boolean apply(Task input)
        {
            return input.getType() == TaskType.STAY;
        };
    };

    public static final Predicate<Task> DRIVE_TASK_PREDICATE = new Predicate<Task>() {
        public boolean apply(Task input)
        {
            return input.getType() == TaskType.DRIVE;
        };
    };

    public static final Comparator<Task> TASK_SCHEDULE_IDX_COMPARATOR = new Comparator<Task>() {
        @Override
        public int compare(Task t1, Task t2)
        {
            if (t1.getSchedule().equals(t2.getSchedule())) {
                throw new IllegalArgumentException("Cannot compare tasks from different schedules");
            }

            return t1.getTaskIdx() - t2.getTaskIdx();
        }
    };


    public static double getActualT1(Schedule<?> schedule)
    {
        Vehicle veh = schedule.getVehicle();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return veh.getT1();
        }

        return Math.min(veh.getT1(), schedule.getBeginTime() + veh.getTimeLimit());
    }


    public static <T extends Task> T getFirstTask(Schedule<T> schedule)
    {
        return schedule.getTasks().get(0);
    }


    public static <T extends Task> T getLastTask(Schedule<T> schedule)
    {
        List<T> tasks = schedule.getTasks();
        return tasks.get(tasks.size() - 1);
    }


    public static boolean isFirstTask(Task task)
    {
        return task.getTaskIdx() == 0;
    }


    public static boolean isLastTask(Task task)
    {
        return task.getTaskIdx() + 1 == task.getSchedule().getTaskCount();
    }


    public static Task getNextTask(Task task)
    {
        return (task.getSchedule()).getTasks().get(task.getTaskIdx() + 1);
    }


    public static Task getPreviousTask(Task task)
    {
        return task.getSchedule().getTasks().get(task.getTaskIdx() - 1);
    }


    public static <T extends Task> Link getLastLinkInSchedule(Schedule<T> schedule)
    {
        List<T> tasks = schedule.getTasks();

        if (tasks.size() == 0) {
            return schedule.getVehicle().getStartLink();
        }

        Task task = tasks.get(tasks.size() - 1);

        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getPath().getToLink();

            case STAY:
                return ((StayTask)task).getLink();

            default:
                throw new IllegalStateException();
        }
    }


    @SuppressWarnings("unchecked")
    public static Iterator<StayTask> createStayTaskIter(Schedule<?> schedule)
    {
        return (Iterator<StayTask>)createTaskFilterIter(schedule, STAY_TASK_PREDICATE);
    }


    @SuppressWarnings("unchecked")
    public static Iterator<DriveTask> createDriveTaskIter(Schedule<?> schedule)
    {
        return (Iterator<DriveTask>)createTaskFilterIter(schedule, DRIVE_TASK_PREDICATE);
    }


    public static Iterator<? extends Task> createTaskFilterIter(Schedule<?> schedule,
            Predicate<Task> taskPredicate)
    {
        return Iterators.filter(schedule.getTasks().iterator(), taskPredicate);
    }
}
