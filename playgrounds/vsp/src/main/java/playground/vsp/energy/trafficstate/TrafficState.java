/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficState
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
package playground.vsp.energy.trafficstate;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class TrafficState {

	private Map<Id, EdgeInfo> edgeInfoMap = new HashMap<Id, EdgeInfo>();
	
	public Map<Id, EdgeInfo> getEdgeInfoMap(){
		return this.edgeInfoMap;
	}
	
	public EdgeInfo addEdgeInfo(EdgeInfo info){
		return this.edgeInfoMap.put(info.getId(), info);
	}
	
	/*
	 * <edgeInfo>
    <edgeId>15</edgeId>
    <timeBin>
        <startTime>2012-04-08T02:00:00+02:00</startTime>
        <endTime>2012-04-08T03:00:00+02:00</endTime>
        <averageSpeed>42</averageSpeed>
    </timeBin>
</edgeInfo>

	 */
	
}
