/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalsTurnInfoBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.router.util.NetworkTurnInfoBuilder;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;

/**
 * @author nagel
 *
 */
class Utils {
	private Utils(){} // do not instantiate

	static Map<Id<Link>, List<TurnInfo>> createTurnInfos(Lanes laneDefs) {
		Map<Id<Link>, List<TurnInfo>> inLinkIdTurnInfoMap = new HashMap<>();
		Set<Id<Link>> toLinkIds = new HashSet<>();
		for (LanesToLinkAssignment l2l : laneDefs.getLanesToLinkAssignments().values()) {
			toLinkIds.clear();
			for (Lane lane : l2l.getLanes().values()) {
				if (lane.getToLinkIds() != null
						&& (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty())) { // make sure that it is a lane at the end of a link
					toLinkIds.addAll(lane.getToLinkIds());
				}
			}
			if (!toLinkIds.isEmpty()) {
				List<TurnInfo> turnInfoList = new ArrayList<TurnInfo>();
				for (Id<Link> toLinkId : toLinkIds) {
					turnInfoList.add(new TurnInfo(l2l.getLinkId(), toLinkId));
				}
				inLinkIdTurnInfoMap.put(l2l.getLinkId(), turnInfoList);
			}
		}
	
		return inLinkIdTurnInfoMap;
	}

	
	static Map<Id<Link>, List<TurnInfo>> createAllowedTurnInfos(Scenario sc){
		Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = new HashMap<>();
	
		NetworkTurnInfoBuilder netTurnInfoBuilder = new NetworkTurnInfoBuilder();
		netTurnInfoBuilder.createAndAddTurnInfo(TransportMode.car, allowedInLinkTurnInfoMap, sc.getNetwork() );
	
		if ( sc.getConfig().network().getLaneDefinitionsFile()!=null || sc.getConfig().qsim().isUseLanes()) {
			Lanes ld = sc.getLanes();
			Map<Id<Link>, List<TurnInfo>> lanesTurnInfoMap = createTurnInfos(ld);
			netTurnInfoBuilder.mergeTurnInfoMaps(allowedInLinkTurnInfoMap, lanesTurnInfoMap);
		}
		final SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(sc.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfig.isUseSignalSystems()) {
			SignalSystemsData ssd = ((SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData();
			Map<Id<Link>, List<TurnInfo>> signalsTurnInfoMap = new SignalsTurnInfoBuilder().createSignalsTurnInfos(ssd);
			netTurnInfoBuilder.mergeTurnInfoMaps(allowedInLinkTurnInfoMap, signalsTurnInfoMap);
		}
		return allowedInLinkTurnInfoMap;
	}

}
