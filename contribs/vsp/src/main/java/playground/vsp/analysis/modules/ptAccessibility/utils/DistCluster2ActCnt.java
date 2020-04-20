/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.vsp.analysis.modules.ptAccessibility.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * @author droeder
 *
 */
public class DistCluster2ActCnt {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(DistCluster2ActCnt.class);

	private Map<String, SortedMap<String, Double>> dist2act2cnt;
	
	public DistCluster2ActCnt(Collection<String> distCluster, Collection<String> actCluster) {
		this.dist2act2cnt = new HashMap<String, SortedMap<String,Double>>();
		
		// initialize
		SortedMap<String, Double> temp;
		for(String d: distCluster){
			temp = new TreeMap<String, Double>();
			for(String a: actCluster){
				temp.put(a, 0.);
			}
			temp.put("unknown", 0.);
			this.dist2act2cnt.put(d, temp);
		}
	}
	
	public void increase(String distanceCluster, String activityCluster){
		Double temp = this.dist2act2cnt.get(distanceCluster).get(activityCluster) + 1;
		this.dist2act2cnt.get(distanceCluster).put(activityCluster, temp);
	}
	
	public Map<String, SortedMap<String, Double>> getResults(){
		return this.dist2act2cnt;
	}
}

