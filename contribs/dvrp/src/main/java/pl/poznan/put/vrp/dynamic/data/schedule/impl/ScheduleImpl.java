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

package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskStatus;


public class ScheduleImpl
    implements Schedule
{
    private final Vehicle vehicle;

    private final List<AbstractTask> tasks;
    private final List<Task> unmodifiableTasks;

    private ScheduleStatus status;
    private AbstractTask currentTask;

    private List<ScheduleListener> listeners;


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ScheduleImpl(Vehicle vehicle)
    {
        this.vehicle = vehicle;

        //TODO what about LinkedList??
        tasks = new ArrayList<AbstractTask>();
        unmodifiableTasks = (List)Collections.unmodifiableList(tasks);

        status = ScheduleStatus.UNPLANNED;
        currentTask = null;

        listeners = new ArrayList<ScheduleListener>();
    }


    @Override
    public Vehicle getVehicle()
    {
        return vehicle;
    }


    @Override
    public List<Task> getTasks()
    {
        return unmodifiableTasks;
    }


    @Override
    public int getTaskCount()
    {
        return tasks.size();
    }


    public void addTask(Task task)
    {
        addTask(tasks.size(), task);
    }


    public void addTask(int taskIdx, Task task)
    {
        validateArgsBeforeAddingTask(taskIdx, task);

        if (status == ScheduleStatus.UNPLANNED) {
            status = ScheduleStatus.PLANNED;
        }

        AbstractTask at = (AbstractTask)task;
        tasks.add(taskIdx, at);
        at.schedule = this;
        at.taskIdx = taskIdx;
        at.status = TaskStatus.PLANNED;

        // update idx of the existing tasks
        for (int i = taskIdx + 1; i < tasks.size(); i++) {
            tasks.get(i).taskIdx = i;
        }

        at.notifyAdded();

        for (ScheduleListener l : listeners) {
            l.taskAdded(task);
        }
    }


    private void validateArgsBeforeAddingTask(int taskIdx, Task task)
    {
        int beginTime = task.getBeginTime();
        int endTime = task.getEndTime();
        Vertex beginVertex = Tasks.getBeginVertex(task);
        Vertex endVertex = Tasks.getEndVertex(task);

        failIfCompleted();

        if (status == ScheduleStatus.STARTED && taskIdx <= currentTask.getTaskIdx()) {
            throw new IllegalStateException();
        }

        int taskCount = tasks.size();

        if (taskIdx < 0 || taskIdx > taskCount) {
            throw new IllegalArgumentException();
        }

        if (beginTime > endTime) {
            throw new IllegalArgumentException();
        }

        if (taskIdx > 0) {
            Task previousTask = tasks.get(taskIdx - 1);

            if (previousTask.getEndTime() != beginTime) {
                throw new IllegalArgumentException();
            }

            if (Tasks.getEndVertex(previousTask) != beginVertex) {
                throw new IllegalArgumentException();
            }
        }
        else { // taskIdx == 0
            if (vehicle.getDepot().getVertex() != beginVertex) {
                throw new IllegalArgumentException();
            }
        }

        if (taskIdx < taskCount) {
            Task nextTask = tasks.get(taskIdx);// currently at taskIdx, but soon at taskIdx+1

            if (nextTask.getBeginTime() != endTime) {
                throw new IllegalArgumentException();
            }

            if (Tasks.getBeginVertex(nextTask) != endVertex) {
                throw new IllegalArgumentException();
            }
        }
    }


    @Override
    public void removeLastPlannedTask()
    {
        removePlannedTask(tasks.size() - 1);
    }


    @Override
    public void removePlannedTask(int taskIdx)
    {
        removePlannedTaskImpl(taskIdx);
    }


    private void removePlannedTaskImpl(int taskIdx)
    {
        failIfUnplanned();
        failIfCompleted();

        AbstractTask task = tasks.get(taskIdx);

        if (task.getStatus() != TaskStatus.PLANNED) {
            throw new IllegalStateException();
        }

        removeTaskImpl(taskIdx);

        for (int i = taskIdx; i < tasks.size(); i++) {
            tasks.get(i).taskIdx = i;
        }

        if (tasks.size() == 0) {
            status = ScheduleStatus.UNPLANNED;
        }
    }


    @Override
    public void removeAllPlannedTasks()
    {
        failIfUnplanned();
        failIfCompleted();

        // only PLANNED schedule (currentTask == NULL) or STARTED schedule (currentTask != NULL)

        int currentIdx = (status == ScheduleStatus.PLANNED) ? -1 : currentTask.taskIdx;

        for (int i = tasks.size() - 1; i > currentIdx; i--) {
            removeTaskImpl(i);
        }

        if (currentIdx == -1) {// all tasks removed
            status = ScheduleStatus.UNPLANNED;
        }
    }


    private void removeTaskImpl(int taskIdx)
    {
        AbstractTask task = tasks.remove(taskIdx);
        task.notifyRemoved();
        task.schedule = null;
        task.status = null;
        task.taskIdx = -1;
    }


    @Override
    public Task getCurrentTask()
    {
        failIfNotStarted();
        return currentTask;
    }


    @Override
    public Task nextTask()
    {
        failIfUnplanned();
        failIfCompleted();

        int nextIdx;

        if (status == ScheduleStatus.PLANNED) {
            status = ScheduleStatus.STARTED;
            nextIdx = 0;
        }
        else { // STARTED
            currentTask.status = TaskStatus.PERFORMED;
            nextIdx = currentTask.taskIdx + 1;
        }

        if (nextIdx == tasks.size()) {
            currentTask = null;
            status = ScheduleStatus.COMPLETED;
        }
        else {
            currentTask = tasks.get(nextIdx);
            currentTask.status = TaskStatus.STARTED;
        }

        for (ScheduleListener l : listeners) {
            l.currentTaskChanged(this);
        }

        return currentTask;
    }


    @Override
    public ScheduleStatus getStatus()
    {
        return status;
    }


    @Override
    public int getBeginTime()
    {
        failIfUnplanned();
        return tasks.get(0).beginTime;
    }


    @Override
    public int getEndTime()
    {
        failIfUnplanned();
        return tasks.get(tasks.size() - 1).endTime;
    }


    @Override
    public void addScheduleListener(ScheduleListener listener)
    {
        listeners.add(listener);
    }


    @Override
    public void removeScheduleListener(ScheduleListener listener)
    {
        listeners.remove(listener);
    }


    @Override
    public String toString()
    {
        return "Schedule_" + vehicle.getId();
    }


    private void failIfUnplanned()
    {
        if (status == ScheduleStatus.UNPLANNED) {
            throw new IllegalStateException();
        }
    }


    private void failIfCompleted()
    {
        if (status == ScheduleStatus.COMPLETED) {
            throw new IllegalStateException();
        }
    }


    private void failIfNotStarted()
    {
        if (status != ScheduleStatus.STARTED) {
            throw new IllegalStateException();
        }
    }
}
