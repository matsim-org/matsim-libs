/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreEdgeWeightTransformer.java
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

package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.util.Map;

import org.apache.commons.collections15.Transformer;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

import edu.uci.ics.jung.graph.util.Pair;

public class DigicoreEdgeWeightTransformer implements Transformer<Pair<Id<ActivityFacility>>, String> {
	private final Map<Pair<Id<ActivityFacility>>, Integer> map;
	
	public DigicoreEdgeWeightTransformer(Map<Pair<Id<ActivityFacility>>, Integer> map) {
		this.map = map;
	}

	@Override
	public String transform(Pair<Id<ActivityFacility>> pair) {
		return map.get(pair).toString();
	}

}

