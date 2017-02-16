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
 * Root class of Task hierarchy.
 * <br/><br/>
 * Design comments:<ul>
 * <li> This interface makes fairly strong assumptions on its implementation.  I am wondering a bit if the design
 * purpose might not be better expressed by removing the interfaces and simply have an implementation class hierarchy.  A bit 
 * like the MATSim events, which also originally were interfaces with implementations behind them, and at some point
 * we united this again.  kai, feb'17
 * </ul>
 * 
 * @author (of documentation) nagel
 */
public interface Task
{
	public enum TaskStatus
	{
		PLANNED, STARTED, PERFORMED;
	}


	TaskStatus getStatus();


	// inclusive
	/**
	 * Returns the begin time of the task.  Design comments:<ul>
	 * <li> I don't know in which cases this is not the end time of the previous task.  kai, feb'17
	 * </ul>
	 */
	double getBeginTime();


	// exclusive
	/**
	 * Returns the end time of the task.  
	 * <br/><br/>
	 * Design comment(s):<ul>
	 * <li> I cannot say what the difference to getTaskTracker().predictEndTime() is.  Possibly, 
	 * getEndTime() is the time when the task actually ends, either because it is a stay task, or because the end time is
	 * only set when it ends.   kai, feb'17
	 * </ul>
	 */
	double getEndTime();


	/**
	 * Back pointer the schedule that contains the task.  Set by ScheduleImpl
	 * through a package-protected variable.  
	 */
	Schedule getSchedule();


	/**
	 * Index of the task in the schedule.  Managed by ScheduleImpl
	 * through a package-protected variable.
	 * <br/><br/>
	 * Design comment(s):<ul>
	 * <li> Should we really have this in the interface?  {@link java.util.List} has an <code>indexOf(...)</code> method that is able to pull out indices (given that the same task object is never entered twice).  kai, feb'17
	 * </ul>
	 */
	int getTaskIdx();


	void setBeginTime(double beginTime);


	void setEndTime(double endTime);


	/**
	 * A TaskTracker predicts the task end time.  
	 * <br/><br/>
	 * Design notes:<ul>
	 * <li> I cannot say what the difference to getEndTime is.  kai, feb'17
	 * </ul>
	 */
	TaskTracker getTaskTracker();


	/**
	 * adds the TaskTracker to the Task.  The existing implementation accepts this only when the Task is already started.
	 */
	void initTaskTracker(TaskTracker taskTracker);
}
