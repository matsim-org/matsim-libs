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

import java.util.List;
import java.util.stream.Stream;

/**
 * A Schedule contains Tasks. <br/>
 * <br/>
 * Design comments:
 * <ul>
 * <li>This interface makes fairly strong assumptions on its implementation. I am wondering a bit if the design purpose
 * might not be better expressed by removing the interfaces and simply have the implementation. kai, feb'17
 * <li>I am happy with Task as an interface. Otherwise casts like (List<DrtTask>)getTasks() would not work (directly).
 * michalm, aug'17
 * </ul>
 * 
 * @author michalm
 * @author (of documentation) nagel
 *
 */
public interface Schedule {
	enum ScheduleStatus {
		UNPLANNED, PLANNED, STARTED, COMPLETED;
	};

	/**
	 * Tasks in the schedule.
	 */
	List<? extends Task> getTasks();// unmodifiableList

	/**
	 * Stream of tasks in the schedule.
	 */
	Stream<? extends Task> tasks();

	/**
	 * Shortcut to getTasks().size()
	 */
	int getTaskCount();

	/**
	 * Pointer to current task.
	 */
	Task getCurrentTask();

	/**
	 * A Task can be planned, started, and done. A schedule can in addition be unplanned. And the naming is a bit
	 * different.
	 */
	ScheduleStatus getStatus();

	/**
	 * Returns the begin time of the initial task, or fails if the Schedule is unplanned.
	 */
	double getBeginTime();

	/**
	 * Returns the end time of the final task, or fails if the Schedule is unplanned.
	 */
	double getEndTime();

	// schedule modification functionality:

	/**
	 * Add a Task to the Schedule.
	 */
	void addTask(Task task);

	/**
	 * Insert a Task into the Schedule at the specified position. The method should re-set all task indices of those
	 * tasks that are moved.
	 */
	void addTask(int taskIdx, Task task);

	/**
	 * Does what it says.
	 */
	void removeLastTask();

	/**
	 * Remove a Task from the Schedule at the specified position. The method should re-set all task indices of those
	 * tasks that are moved.
	 */
	void removeTask(Task task);

	/**
	 * This behaves a bit like it.next() in collections: It moves to the next task, makes it the current one, and
	 * returns it. If no task is left, it sets the Schedule to completed and returns null.
	 */
	Task nextTask();// this one seems synchronous (will be executed when switching between DynActions)
}
