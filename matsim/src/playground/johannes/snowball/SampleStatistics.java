/* *********************************************************************** *
 * project: org.matsim.*
 * SampleStatistics.java
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

/**
 * 
 */
package playground.johannes.snowball;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class SampleStatistics {
	
	static public Map<String, Integer> countSampledVertices(Graph g) {
		return countSampledElements(g.getVertices());
	}
	
	static public Map<String, Integer> countSampledEdges(Graph g) {
		return countSampledElements(g.getEdges());
	}
	
	static private Map<String, Integer> countSampledElements(Collection<UserDataContainer> elements) {
		Map<String, Integer> table = new HashMap<String, Integer>();

		int totalSampled = 0;
		int totalMultiSampled = 0;
		for(UserDataContainer e : elements) {
			IntArrayList waves = (IntArrayList) e.getUserDatum(Sampler.WAVE_KEY);
			if(waves != null) {
				String key = "wave" + waves.get(0);
				Integer waveCount = table.get(key);
				if(waveCount == null)
					waveCount = 0;
				waveCount++;
				totalSampled++;
				table.put(key, waveCount);
				
				if(waves.size() > 1) {
					key = waves.size() + "counts";
					Integer nCounts = table.get(key);
					if(nCounts == null)
						nCounts = 0;
					nCounts++;
					totalMultiSampled++;
					table.put(key, nCounts);
				}
			}
		}
		
		table.put("totalSampled", totalSampled);
		table.put("totalMultipleSampled", totalMultiSampled);
		return table;

	}
}
