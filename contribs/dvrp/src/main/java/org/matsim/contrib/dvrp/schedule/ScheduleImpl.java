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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;

/**
 * @author michalm
 */
public class ScheduleImpl implements Schedule {
	private final Vehicle vehicle;

	private final List<AbstractTask> tasks = new ArrayList<>();
	private final List<? extends Task> unmodifiableTasks = Collections.unmodifiableList(tasks);

	private ScheduleStatus status = ScheduleStatus.UNPLANNED;
	private AbstractTask currentTask = null;

	public ScheduleImpl(Vehicle vehicle) {
		this.vehicle = vehicle;
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

	public void addTask(Task task) {
		addTask(tasks.size(), task);
	}

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
		if (status == ScheduleStatus.STARTED && taskIdx <= currentTask.getTaskIdx()) {
			throw new IllegalStateException();
		}

		double beginTime = task.getBeginTime();
		double endTime = task.getEndTime();
		Link beginLink = Tasks.getBeginLink(task);
		// Link endLink = Tasks.getEndLink(task);
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

			if (Tasks.getEndLink(previousTask) != beginLink) {
				Logger.getLogger(getClass()).error("Last task End link: " + Tasks.getEndLink(previousTask).getId()
						+ " ; next Task start link: " + beginLink.getId());
				throw new IllegalArgumentException();
			}
		} else { // taskIdx == 0
			if (vehicle.getStartLink() != beginLink) {
				throw new IllegalArgumentException();
			}
		}

		// if (taskIdx < taskCount) {
		// Task nextTask = tasks.get(taskIdx);// currently at taskIdx, but soon at taskIdx+1
		//
		// if (nextTask.getBeginTime() != endTime) {
		// throw new IllegalArgumentException();
		// }
		//
		// if (Tasks.getBeginLink(nextTask) != endLink) {
		// throw new IllegalArgumentException();
		// }
		// }
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

		AbstractTask task = tasks.get(taskIdx);

		if (task.getStatus() != TaskStatus.PLANNED) {
			throw new IllegalStateException();
		}

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
	public String toString() {
		return "Schedule_" + vehicle.getId();
	}

	private void failIfUnplanned() {
		if (status == ScheduleStatus.UNPLANNED) {
			throw new IllegalStateException();
		}
	}

	private void failIfCompleted() {
		if (status == ScheduleStatus.COMPLETED) {
			throw new IllegalStateException();
		}
	}

	private void failIfNotStarted() {
		if (status != ScheduleStatus.STARTED) {
			throw new IllegalStateException();
		}
	}
}
