/* *********************************************************************** *
 * project: org.matsim.*
 * DgNetwork
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
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgKSNetwork {

	private static final Logger log = Logger.getLogger(DgKSNetwork.class);
	
	private Map<Id<DgCrossing>, DgCrossing> crossings = new HashMap<>();
	private Map<Id<DgStreet>, DgStreet> streets = new HashMap<>();

	public void addCrossing(DgCrossing crossing) {
		if (this.crossings.containsKey(crossing.getId())) {
			log.warn("Crossing Id " + crossing.getId() + " already exists and will be overwritten!");
		}
		this.crossings.put(crossing.getId(), crossing);
	}
	
	public Map<Id<DgCrossing>, DgCrossing> getCrossings(){
		return this.crossings;
	}

	public void addStreet(DgStreet street) {
		if (this.streets.containsKey(street.getId())) {
			log.warn("Street Id " + street.getId() + " already exists and will be overwritten!");
		}
		this.warnIfEdgeExists(street);
		this.streets.put(street.getId(), street);
	}
	
	public Map<Id<DgStreet>, DgStreet> getStreets(){
		return this.streets;
	}
	
	
	private void warnIfEdgeExists(DgStreet street){
		Id<DgCrossingNode> fromNodeId = street.getFromNode().getId();
		Id<DgCrossingNode> toNodeId = street.getToNode().getId();
		for (DgStreet s : this.streets.values()){
			if (s.getFromNode().getId().equals(fromNodeId) && s.getToNode().getId().equals(toNodeId)) {
				log.warn("duplicated street " + street.getId() + " from node " + fromNodeId + " to node " + toNodeId);
			}
		}
	}

}
