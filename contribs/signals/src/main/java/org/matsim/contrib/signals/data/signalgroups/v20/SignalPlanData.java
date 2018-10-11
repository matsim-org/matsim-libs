/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.data.signalgroups.v20;

import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
/**
 * 
 * @author dgrether
 *
 */
public interface SignalPlanData {

	public Id<SignalPlan> getId();

	public void addSignalGroupSettings(SignalGroupSettingsData signalGroupSettings);

	public SortedMap<Id<SignalGroup>, SignalGroupSettingsData> getSignalGroupSettingsDataByGroupId();

	/**
	 * @return 0.0 if not set
	 */
	public double getStartTime();
	/**
	 * Set the time of day the  plan should be activated. Set start and end to 0.0 if the plan should be active all day.
	 */
	public void setStartTime(Double seconds);
	/**
	 * @return 0.0 if not set
	 */
	public double getEndTime();
	/**
	 * Set the time of day the  plan should be activated. Set start and end to 0.0 if the plan should be active all day.
	 */
	public void setEndTime(Double seconds);
	/**
	 * @return null if not set
	 */
	public Integer getCycleTime();

	public void setCycleTime(Integer cycleTime);
	/**
	 * @return 0 if not set
	 */
	public int getOffset();

	public void setOffset(int seconds);
	
}