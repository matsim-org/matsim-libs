/* *********************************************************************** *
 * project: org.matsim.*
 * SignalData
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
package org.matsim.contrib.signals.data;

import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.ambertimes.v10.IntergreenTimesData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;


/**
 * @author dgrether
 *
 */
public interface SignalsData {
	/**
	 * Name under which signals data are added as scenario elements
	 */
	public static final String ELEMENT_NAME = "signalsData";
	
	public SignalSystemsData getSignalSystemsData();
	
	public SignalControlData getSignalControlData();

	public SignalGroupsData getSignalGroupsData();
	/**
	 * @return null if feature is not enabled
	 */
	public AmberTimesData getAmberTimesData();
	/**
	 * @return null if feature is not enabled
	 */
	public IntergreenTimesData getIntergreenTimesData();
	
}
