/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsTurnInfoBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.builder;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;

import java.util.*;


/**
 * Creates TurnInfo objects from traffic signal data.
 * 
 * @author dgrether
 *
 */
class SignalsTurnInfoBuilder {
	
	private static final Logger log = Logger.getLogger(SignalsTurnInfoBuilder.class);

	private static int warnCount = 0;

	
	public static Map<Id<Link>, List<TurnInfo>> createSignalsTurnInfos(SignalSystemsData ssd) {
		Map<Id<Link>, List<TurnInfo>> inLinkIdTurnInfoMap = new HashMap<Id<Link>, List<TurnInfo>>();
		for (SignalSystemData signalSystem : ssd.getSignalSystemData().values()){
			for (SignalData signal : signalSystem.getSignalData().values()){
				if (signal.getTurningMoveRestrictions() != null && ! signal.getTurningMoveRestrictions().isEmpty()){
					if (warnCount < 1){
						log.warn("Turning move restrictions for signals are implemented for TransportMode.car only, yet!");
						warnCount++;
					}
					if (!inLinkIdTurnInfoMap.containsKey(signal.getLinkId())){
						inLinkIdTurnInfoMap.put(signal.getLinkId(), new ArrayList<TurnInfo>());
					}
					Set<String> modeCar = new HashSet<String>();
					modeCar.add(TransportMode.car);
					for (Id<Link> toLinkId : signal.getTurningMoveRestrictions()){
						TurnInfo ti = new TurnInfo(signal.getLinkId(), toLinkId, modeCar);
						inLinkIdTurnInfoMap.get(signal.getLinkId()).add(ti);
					}
				}
			}
		}
		return inLinkIdTurnInfoMap;
	}
}
