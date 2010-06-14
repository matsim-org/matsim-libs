/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsNew.data.v20;

import java.util.SortedMap;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public interface SignalSystemData {
	
	public Id getId();
	/**
	 * @return the default cycle in seconds
	 */
	public Double getDefaultCycleTime();

	public void setDefaultCycleTime(Double defaultCycleTimeSeconds);
	
	public SortedMap<Id, SignalGroupData> getSignalGroupData();
	
	public void addSignalGroupData(SignalGroupData signal);
}
