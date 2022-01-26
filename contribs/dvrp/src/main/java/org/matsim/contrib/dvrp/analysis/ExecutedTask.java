/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Tasks;

import com.google.common.base.Preconditions;

/**
 * Immutable
 *
 * @author Michal Maciejewski (michalm)
 */
public class ExecutedTask {
	/**
	 * ExecutedTasks should be created from events rather than Tasks
	 */
	public static ExecutedTask createFromTask(Task task) {
		// just some sanity check. However, even already PERFORMED tasks can be still modified (the class has setters),
		// so no guarantee if the created ExecutedTask really represents its actual execution.
		Preconditions.checkArgument(task.getStatus() == Task.TaskStatus.PERFORMED);
		return new ExecutedTask(task.getTaskType(), task.getBeginTime(), task.getEndTime(),
				Tasks.getBeginLink(task).getId(), Tasks.getEndLink(task).getId());
	}

	public final Task.TaskType taskType;
	public final double beginTime;
	public final double endTime;
	public final Id<Link> startLinkId;
	public final Id<Link> endLinkId;

	public ExecutedTask(Task.TaskType taskType, double beginTime, double endTime, Id<Link> startLinkId,
			Id<Link> endLinkId) {
		Preconditions.checkArgument(beginTime <= endTime);
		this.taskType = Preconditions.checkNotNull(taskType);
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.startLinkId = Preconditions.checkNotNull(startLinkId);
		this.endLinkId = Preconditions.checkNotNull(endLinkId);
	}
}
