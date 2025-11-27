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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;

import com.google.common.base.Preconditions;

/**
 * @author michalm
 */
final class ScheduleImpl implements Schedule {
	private final DvrpVehicleSpecification vehicleSpecification;

	private final List<AbstractTask> tasks = new ArrayList<>();
	private final List<? extends Task> unmodifiableTasks = Collections.unmodifiableList(tasks);

	private ScheduleStatus status = ScheduleStatus.UNPLANNED;
	private AbstractTask currentTask = null;

	ScheduleImpl(DvrpVehicleSpecification vehicleSpecification) {
		this.vehicleSpecification = vehicleSpecification;
	}

	@Override
	public List<? extends Task> getTasks() {
		return unmodifiableTasks;
	}

	@Override
	public Stream<? extends Task> tasks() {
		return tasks.stream();
	}

	@Override
	public int getTaskCount() {
		return tasks.size();
	}

	@Override
	public void addTask(Task task) {
		addTask(tasks.size(), task);
	}

	@Override
	public void addTask(int taskIdx, Task task) {
		validateArgsBeforeAddingTask(taskIdx, task);

		if (status == ScheduleStatus.UNPLANNED) {
			status = ScheduleStatus.PLANNED;
		}

		AbstractTask t = (AbstractTask)task;
		tasks.add(taskIdx, t);
		t.taskIdx = taskIdx;
		t.status = TaskStatus.PLANNED;

		// update idx of the existing tasks
		for (int i = taskIdx + 1; i < tasks.size(); i++) {
			tasks.get(i).taskIdx = i;
		}
	}

	private void validateArgsBeforeAddingTask(int taskIdx, Task task) {
		failIfCompleted();
		Preconditions.checkState(status != ScheduleStatus.STARTED || taskIdx > currentTask.getTaskIdx(),
				"Schedule hasn't started; vehicle %s", vehicleSpecification.getId());

		double beginTime = task.getBeginTime();
		double endTime = task.getEndTime();
		Link beginLink = Tasks.getBeginLink(task);
		int taskCount = tasks.size();

		Preconditions.checkArgument(taskIdx >= 0 && taskIdx <= taskCount,
				"Task index %s out of bounds [0,%s]; vehicle %s", taskIdx, taskCount, vehicleSpecification.getId());
		Preconditions.checkArgument(beginTime <= endTime,
				"Begin time %s > end time %s; vehicle %s", beginTime, endTime, vehicleSpecification.getId());

		if (taskIdx > 0) {
			Task previousTask = tasks.get(taskIdx - 1);
			Preconditions.checkArgument(previousTask.getEndTime() == beginTime,
					"Previous task end time %s != next task begin time %s; Vehicle %s",
					previousTask.getEndTime(), beginTime, vehicleSpecification.getId());
			Preconditions.checkArgument(Tasks.getEndLink(previousTask) == beginLink,
					"Last task end link: %s; Next task start link: %s; vehicle %s", Tasks.getEndLink(previousTask).getId(),
					beginLink.getId(), vehicleSpecification.getId());
		} else { // taskIdx == 0
			Preconditions.checkArgument(vehicleSpecification.getStartLinkId().equals(beginLink.getId()),
					"First task link %s != vehicle start link %s; vehicle %s",
					beginLink.getId(), vehicleSpecification.getStartLinkId(), vehicleSpecification.getId());
		}
	}

	@Override
	public void removeLastTask() {
		removeTaskImpl(tasks.size() - 1);
	}

	@Override
	public void removeTask(Task task) {
		removeTaskImpl(task.getTaskIdx());
	}

	private void removeTaskImpl(int taskIdx) {
		failIfUnplanned();
		failIfCompleted();

		Preconditions.checkState(tasks.get(taskIdx).getStatus() == TaskStatus.PLANNED,
				"Task %s is not in planned state but in %s; vehicle %s", taskIdx, tasks.get(taskIdx).getStatus(), vehicleSpecification.getId());
		tasks.remove(taskIdx);

		for (int i = taskIdx; i < tasks.size(); i++) {
			tasks.get(i).taskIdx = i;
		}

		if (tasks.size() == 0) {
			status = ScheduleStatus.UNPLANNED;
		}
	}

	@Override
	public Task getCurrentTask() {
		failIfNotStarted();// status != ScheduleStatus.STARTED
		return currentTask;
	}

	@Override
	public Task nextTask() {
		failIfUnplanned();
		failIfCompleted();

		nextTaskImpl();

		return currentTask;
	}

	private void nextTaskImpl() {
		int nextIdx;

		if (status == ScheduleStatus.PLANNED) {
			status = ScheduleStatus.STARTED;
			nextIdx = 0;
		} else { // STARTED
			currentTask.status = TaskStatus.PERFORMED;
			// TODO ?? currentTask.setTaskTracker(null);
			nextIdx = currentTask.taskIdx + 1;
		}

		if (nextIdx == tasks.size()) {
			currentTask = null;
			status = ScheduleStatus.COMPLETED;
		} else {
			currentTask = tasks.get(nextIdx);
			currentTask.status = TaskStatus.STARTED;
		}
	}

	@Override
	public ScheduleStatus getStatus() {
		return status;
	}

	@Override
	public double getBeginTime() {
		failIfUnplanned();
		return tasks.get(0).getBeginTime();
	}

	@Override
	public double getEndTime() {
		failIfUnplanned();
		return tasks.get(tasks.size() - 1).getEndTime();
	}

	@Override
	public void update(Schedule schedule) {

		if (!(schedule instanceof ScheduleImpl s)) {
			throw new IllegalArgumentException("Schedule must be of type ScheduleImpl");
		}

		tasks.clear();
		tasks.addAll(s.tasks);

		status = s.status;
		currentTask = s.currentTask;
	}

	@Override
	public String toString() {
		return "Schedule_" + vehicleSpecification.getId();
	}

	private void failIfUnplanned() {
		Preconditions.checkState(status != ScheduleStatus.UNPLANNED,
				"Schedule hasn't been planned yet; vehicle %s", vehicleSpecification.getId());
	}

	private void failIfCompleted() {
		Preconditions.checkState(status != ScheduleStatus.COMPLETED,
				"Schedule has been completed; " +
				"vehicle %s", vehicleSpecification.getId());
	}

	private void failIfNotStarted() {
		Preconditions.checkState(status == ScheduleStatus.STARTED,
				"Schedule hasn't been started yet; vehicle %s", vehicleSpecification.getId());
	}
}
