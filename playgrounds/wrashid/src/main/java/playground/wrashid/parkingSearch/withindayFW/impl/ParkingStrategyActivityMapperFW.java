/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,     *
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

package playground.wrashid.parkingSearch.withindayFW.impl;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingStrategyActivityMapper;

public class ParkingStrategyActivityMapperFW implements ParkingStrategyActivityMapper {

	// TODO: in this mapping, for all people the same strategy is applied for the same kind
	// of activity. We need to make a scond class also, which makes individual mappings
	
	//HashMap<Id, LinkedListValueHashMap<String, ParkingStrategy>> mapping;
	LinkedListValueHashMap<String, ParkingStrategy> mapping;

	public ParkingStrategyActivityMapperFW() {
		mapping = new LinkedListValueHashMap<String, ParkingStrategy>();
	}

	@Override
	public Collection<ParkingStrategy> getParkingStrategies(Id agentId, String activityType) {
		return mapping.get(activityType);
	}

	@Override
	public void addSearchStrategy(Id agentId, String activityType, ParkingStrategy parkingStrategy) {
//		if (!mapping.containsKey(agentId)) {
//			mapping.put(agentId, new LinkedListValueHashMap<String, ParkingStrategy>());
//		}

		//LinkedListValueHashMap<String, ParkingStrategy> linkedListValueHashMap = mapping.get(activityType);

		mapping.put(activityType, parkingStrategy);
	}

}
