/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.gridlock.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.collections.Tuple;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * @author tthunig
 *
 */
public class TtAnalyzeGridlockInflowOutflow implements LinkEnterEventHandler, LinkLeaveEventHandler{
	
	private Map<Double, Integer> outflow = new TreeMap<>();
	private Map<Double, Integer> inflow = new TreeMap<>();
	private boolean mapsCompleted = false;
	
	@Override
	public void reset(int iteration) {
		outflow.clear();
		inflow.clear();
		mapsCompleted = false;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		switch (event.getLinkId().toString()){
		case "1_2":
		case "4_3":
			if (!inflow.containsKey(event.getTime())){
				inflow.put(event.getTime(), 0);
			}
			inflow.put(event.getTime(), inflow.get(event.getTime()) + 1);
			break;
		default:
			break;	
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		switch (event.getLinkId().toString()){
		case "2_1":
		case "3_4":
			if (!outflow.containsKey(event.getTime())){
				outflow.put(event.getTime(), 0);
			}
			outflow.put(event.getTime(), outflow.get(event.getTime()) + 1);
			break;
		default:
			break;	
		}
	}
	
	public Map<Double, Integer> getInflowPerSec() {
		if (!mapsCompleted) completeMaps();
		return inflow;
	}
	
	public Map<Double, Integer> getOutflowPerSec() {
		if (!mapsCompleted) completeMaps();
		return outflow;
	}
	
	public Map<Double, Integer> determineCumulativeInflowPerSec(){
		if (!mapsCompleted) completeMaps();
		return determineCumulativeCounts(inflow);
	}
	
	public Map<Double, Integer> determineCumulativeOutflowPerSec(){
		if (!mapsCompleted) completeMaps();
		return determineCumulativeCounts(outflow);
	}
	
	private void completeMaps(){
		Tuple<Double, Double> firstLastCountInflow = TtAbstractAnalysisTool.determineMinMaxDoubleInSet(inflow.keySet());
		Tuple<Double, Double> firstLastCountOutflow = TtAbstractAnalysisTool.determineMinMaxDoubleInSet(outflow.keySet());
		long minMin = firstLastCountInflow.getFirst().longValue(); // is allways the minimum
		long maxMax = (long) Math.max(firstLastCountInflow.getSecond(), firstLastCountOutflow.getSecond());
		completeMap(inflow, minMin, maxMax);
		completeMap(outflow, minMin, maxMax);
		mapsCompleted = true;
	}
	
	/**
	 * fill missing time steps between first and last with zeros
	 */
	private static void completeMap(Map<Double, Integer> countsMap, long first, long last) {
		for (long sec = first; sec <= last; sec++) {
			if (!countsMap.containsKey((double) sec)) {
				countsMap.put((double) sec, 0);
			}
		}
	}
	
	private Map<Double, Integer> determineCumulativeCounts(Map<Double, Integer> countsMap) {
		Map<Double, Integer> comCounts = new TreeMap<>();
		int currentCumCounts = 0;
		for (Entry<Double, Integer> entry : countsMap.entrySet()){
			currentCumCounts += entry.getValue();
			comCounts.put(entry.getKey(), currentCumCounts);
		}
		return comCounts;
	}

}
