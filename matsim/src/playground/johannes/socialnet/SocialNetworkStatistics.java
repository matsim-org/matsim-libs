/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnet;

import gnu.trove.TDoubleDoubleHashMap;

import org.matsim.interfaces.basic.v01.Coord;

import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SocialNetworkStatistics {

	public static WeightedStatistics getEdgeLengthDistribution(SocialNetwork<?> network) {
		return getEdgeLengthDistribution(network, false, 0);
	}
	
	public static WeightedStatistics getEdgeLengthDistribution(SocialNetwork<?> network, boolean normalize, double normBinSize) {
		WeightedStatistics stats = new WeightedStatistics();
		for(Ego<?> e : network.getVertices()) {
			Coord c1 = e.getCoord();
			TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
			
			if(normalize) {
				for(Ego<?> e2 : network.getVertices()) {
					Coord c2 = e2.getCoord();
					double d = c1.calcDistance(c2);
					double bin = Math.floor(d / normBinSize);
					double count = hist.get(bin);
					count++;
					hist.put(bin, count);
				}
			}
			
			for(Ego<?> e2 : e.getNeighbours()) {
				double d = e.getCoord().calcDistance(e2.getCoord());
				double w = 1;
				if(normalize)
					w = 1 / hist.get(Math.floor(d / normBinSize));
				stats.add(d, w);
			}
		}
		
		return stats;
	}
}
