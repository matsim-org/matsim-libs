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

import org.matsim.contrib.dvrp.tracker.TaskTracker;

/**
 * Root class of Task hierarchy. <br/>
 * <br/>
 * Design comments:
 * <ul>
 * <li>This interface makes fairly strong assumptions on its implementation. I am wondering a bit if the design purpose
 * might not be better expressed by removing the interfaces and simply have an implementation class hierarchy. A bit
 * like the MATSim events, which also originally were interfaces with implementations behind them, and at some point we
 * united this again. kai, feb'17
 * </ul>
 * 
 * @author michalm
 * @author (of documentation) nagel
 */
public interface Task {
	public enum TaskStatus {
		PLANNED, STARTED, PERFORMED;
	}

	TaskStatus getStatus();

	/**
	 * Returns the begin time of the task (inclusive). Should be equal to the end time of the previous task.
	 */
	double getBeginTime();

	/**
	 * Returns the end time of the task (exclusive). Should be equal to the begin time of the next task.<br/>
	 * <br/>
	 * This is a scheduled (planned) end time. To get a more precise estimate on the expected end time of the current
	 * task, use {@link TaskTracker#predictEndTime()}. The actual end time is a result of simulation.<br/>
	 * <br/>
	 * When the task ends, its end time (and the begin/end times of the following tasks) should be updated accordingly
	 * (see {@link Task#setBeginTime(double)} and {@link Task#setEndTime(double)}).
	 */
	double getEndTime();

	/**
	 * Index of the task in the schedule. Managed by ScheduleImpl through a package-protected variable.
	 */
	int getTaskIdx();

	void setBeginTime(double beginTime);

	void setEndTime(double endTime);

	/**
	 * A TaskTracker predicts the task end time. The prediction may be different than the planned and actual end times.
	 */
	TaskTracker getTaskTracker();

	/**
	 * adds the TaskTracker to the ongoing Task (preferably at the beginning of its execution). Only ongoing tasks
	 * (TaskStatus == STARTED) can be tracked.
	 */
	void initTaskTracker(TaskTracker taskTracker);
}
