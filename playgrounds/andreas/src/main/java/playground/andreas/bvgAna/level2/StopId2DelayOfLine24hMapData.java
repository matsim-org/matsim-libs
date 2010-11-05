/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bvgAna.level2;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

/**
 * Simple data container to sum up the delay of each line
 * 
 * @author aneumann
 *
 */
public class StopId2DelayOfLine24hMapData {
	
	private final Id stopId;
	
	private TreeMap<Id, Tuple<Integer, Double>> lineId2Delay24h = new TreeMap<Id, Tuple<Integer, Double>>();
//	private TreeMap<Id, LinkedList<Tuple<Double, Double>>> routeId2TimeDelayMap = new TreeMap<Id, LinkedList<Tuple<Double,Double>>>();
//	private TreeMap<Id, LinkedList<Tuple<Double, Double>>> vehId2TimeDelayMap = new TreeMap<Id, LinkedList<Tuple<Double,Double>>>();
	
	public StopId2DelayOfLine24hMapData(Id stopId){
		this.stopId = stopId;
	}

	public void addDelayForLine(Id lineId, double delay) {
		if(this.lineId2Delay24h.get(lineId) == null){
			this.lineId2Delay24h.put(lineId, new Tuple<Integer, Double>(new Integer(0), new Double(0.0)));
		}
		Tuple<Integer, Double> oldTuple = this.lineId2Delay24h.get(lineId);
		Tuple<Integer, Double> newTuple = new Tuple<Integer, Double>(new Integer(oldTuple.getFirst().intValue() + 1), new Double(oldTuple.getSecond().doubleValue() + delay));
		this.lineId2Delay24h.put(lineId, newTuple);
	}
	
	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Average delay per line for stop " + this.stopId + ": ");
		for (Entry<Id, Tuple<Integer, Double>> entry : this.lineId2Delay24h.entrySet()) {
			strB.append("Line " + entry.getKey() + " with " + entry.getValue().getFirst().intValue() + " departures has an average delay of " + entry.getValue().getSecond().doubleValue() / entry.getValue().getFirst().doubleValue() + "s");
		}
		return strB.toString();
	}

}
