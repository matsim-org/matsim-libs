/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.osm;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * @author droeder
 *
 */
public class LineNetworkStore {
	
	private Network net;
	private Map<String, Network> lineId2Network;
	public final String ONE = "oneway";
	public final String BOTH = "both";

	/**
	 * 
	 * @param net the origin Network
	 */
	public LineNetworkStore(Network net){
		this.net = net;
		this.lineId2Network = new HashMap<String, Network>();
	}
	
	public void addWay(Way w, String direction, String line){
		Network net;
		if(lineId2Network.containsKey(line)){
			net = lineId2Network.get(line);
		}else{
			net = NetworkImpl.createNetwork();
			lineId2Network.put(line, net);
		}
		
		if(direction.equals(this.ONE)){
			this.addOneWay(w, net);
		}else if(direction.equals(this.BOTH)){
			this.addBoth(w, net);
		}
	}


	/**
	 * @param w
	 * @param net2
	 */
	private void addBoth(Way w, Network net2) {
//		this.addOneWay(w, net2);
//		this.addOneWayReverse();
	}

	/**
	 * @param w
	 * @param net2
	 */
	private void addOneWay(Way w, Network net2) {
		// TODO Auto-generated method stub
		
	}

}
