/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsUtils
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
package org.matsim.signalsystems;

import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;


/**
 * @author dgrether
 *
 */
public class SignalUtils {

	
	private SignalUtils(){}

	/**
	 * Creates a SignalGroupData instance for each SignalData instance of the
	 * SignalSystemData instance given as parameter and adds it to the SignalGroupsData
	 * container given as parameter. The SignalGroupData instance
	 * has the same Id as the SignalData instance.
	 * @param groups The container to that the SignalGroupData instances are added.
	 * @param system The SignalSystemData instance  whose SignalData Ids serve
	 * as template for the SignalGroupData
	 */
	public static void createAndAddSignalGroups4Signals(SignalGroupsData groups, SignalSystemData system) {
		for (SignalData signal : system.getSignalData().values()){
			SignalGroupData group4signal = groups.getFactory().createSignalGroupData(system.getId(), signal.getId());
			group4signal.addSignalId(signal.getId());
			groups.addSignalGroupData(group4signal);
		}
	}
	
	
	
}
