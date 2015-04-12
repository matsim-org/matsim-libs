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
package org.matsim.contrib.signals;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signals.data.signalsystems.v20.SignalData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalGroup;


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
			SignalGroupData group4signal = groups.getFactory().createSignalGroupData(system.getId(), Id.create(signal.getId(), SignalGroup.class));
			group4signal.addSignalId(signal.getId());
			groups.addSignalGroupData(group4signal);
		}
	}
	
	/**
	 * Convenience method to create a signal with the given Id on the link with the given Id on the lanes
	 * with the given Ids. The signal is added to the SignalSystemData. 
	 */
	public static void createAndAddSignal(SignalSystemData sys, SignalSystemsDataFactory factory, 
			Id<Signal> signalId, Id<Link> linkId, Id<Lane>... laneIds){
		SignalData signal = factory.createSignalData(signalId);
		sys.addSignalData(signal);
		signal.setLinkId(linkId);
		if (laneIds != null){
			for (Id<Lane> laneId : laneIds){
				signal.addLaneId(laneId);
			}
		}
	}
	
	
}
