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

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;

import com.google.common.base.*;
import com.google.common.collect.Iterators;


public class Schedules
{
    public static final Predicate<Task> SERVE_TASK_PREDICATE = new TaskTypePredicate(TaskType.SERVE);

    public static final Predicate<Task> DRIVE_TASK_PREDICATE = new TaskTypePredicate(TaskType.DRIVE);

    public static final Predicate<Task> WAIT_TASK_PREDICATE = new TaskTypePredicate(TaskType.WAIT);

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

    public static final Function<ServeTask, Request> SERVE_TASK_TO_REQUEST = new Function<ServeTask, Request>() {
        public Request apply(ServeTask input)
        {
            return input.getRequest();
        }
    };


    public static int getActualT1(Schedule schedule)
    {
        Vehicle veh = schedule.getVehicle();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return veh.getT1();
        }

        return Math.min(veh.getT1(), schedule.getBeginTime() + veh.getTimeLimit());
    }


    public static Task getFirstTask(Schedule schedule)
    {
        return schedule.getTasks().get(0);
    }


    public static Task getLastTask(Schedule schedule)
    {
        List<Task> tasks = schedule.getTasks();
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
        return task.getSchedule().getTasks().get(task.getTaskIdx() + 1);
    }


    public static Task getPreviousTask(Task task)
    {
        return task.getSchedule().getTasks().get(task.getTaskIdx() - 1);
    }


    public static Vertex getLastVertexInSchedule(Schedule schedule)
    {
        List<Task> tasks = schedule.getTasks();

        if (tasks.size() == 0) {
            return schedule.getVehicle().getDepot().getVertex();
        }

        Task task = tasks.get(tasks.size() - 1);

        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getArc().getToVertex();

            case SERVE:
            case WAIT:
                return ((StayTask)task).getAtVertex();

            default:
                throw new IllegalStateException();
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Iterator<Request> createRequestIter(Schedule schedule)
    {
        return (Iterator)createTaskFilterIter(schedule, SERVE_TASK_PREDICATE);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Iterator<ServeTask> createServeTaskIter(Schedule schedule)
    {
        return (Iterator)createTaskFilterIter(schedule, SERVE_TASK_PREDICATE);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Iterator<DriveTask> createDriveTaskIter(Schedule schedule)
    {
        return (Iterator)createTaskFilterIter(schedule, DRIVE_TASK_PREDICATE);
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Iterator<DriveTask> createWaitTaskIter(Schedule schedule)
    {
        return (Iterator)createTaskFilterIter(schedule, WAIT_TASK_PREDICATE);
    }


    public static Iterator<Task> createTaskFilterIter(Schedule schedule,
            Predicate<Task> taskPredicate)
    {
        return Iterators.filter(schedule.getTasks().iterator(), taskPredicate);
    }


    private static class TaskTypePredicate
        implements Predicate<Task>
    {
        private TaskType taskType;


        public TaskTypePredicate(TaskType taskType)
        {
            this.taskType = taskType;
        }


        @Override
        public boolean apply(Task input)
        {
            return input.getType() == taskType;
        }
    }
}
