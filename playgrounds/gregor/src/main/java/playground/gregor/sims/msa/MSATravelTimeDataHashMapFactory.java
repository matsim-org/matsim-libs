/* *********************************************************************** *
 * project: org.matsim.*
 * MSATravelTimeDataHashMapFactory.java
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
package playground.gregor.sims.msa;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.trafficmonitoring.TravelTimeData;
import org.matsim.core.trafficmonitoring.TravelTimeDataFactory;

public class MSATravelTimeDataHashMapFactory implements TravelTimeDataFactory {

	
	private final Network network;
	private final int binSize;
	
	private Map<Id, HashMap<Integer,Double>> msaTT = new HashMap<Id, HashMap<Integer,Double>>();

	public MSATravelTimeDataHashMapFactory(Network network, int binSize) {
		this.network = network;
		this.binSize = binSize;
	}

	@Override
	public TravelTimeData createTravelTimeData(Id linkId) {
		HashMap<Integer, Double> lmsa = this.msaTT.get(linkId);
		if (lmsa == null) {
			lmsa = new HashMap<Integer, Double>(7200/this.binSize);
			this.msaTT.put(linkId, lmsa);
		}
		return new MSATravelTimeDataHashMap(this.network.getLinks().get(linkId),this.binSize, lmsa);
	}

}
