/* *********************************************************************** *
 * project: org.matsim.*
 * SampleStats.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import gnu.trove.TIntIntHashMap;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SampleStats {

	private final TIntIntHashMap sampled;
	
	private final TIntIntHashMap sampledAccumulated;
	
	private final TIntIntHashMap detected;
	
	private final TIntIntHashMap detectedAccumulated;
	
	private int maxIteration;
	
	private double responseRate;
	
	public SampleStats(SampledGraph graph) {
		sampled = new TIntIntHashMap();
		detected = new TIntIntHashMap();
		sampledAccumulated = new TIntIntHashMap();
		detectedAccumulated = new TIntIntHashMap();
		
		maxIteration = 0;
		for(Vertex vertex : graph.getVertices()) {
			sampled.adjustOrPutValue(((SampledVertex)vertex).getIterationSampled(), 1, 1);
			detected.adjustOrPutValue(((SampledVertex)vertex).getIterationDetected(), 1, 1);
			maxIteration = Math.max(maxIteration, ((SampledVertex)vertex).getIterationSampled());
		}
		
		detected.adjustValue(0, - sampled.get(0));
		
		for(int i = 0; i <= maxIteration; i++) {
			sampledAccumulated.put(i, sampledAccumulated.get(i - 1) + sampled.get(i));
			detectedAccumulated.put(i, detectedAccumulated.get(i - 1) + detected.get(i));
		}
		
		responseRate = 1;
		if(maxIteration > 0)
			responseRate = sampledAccumulated.get(maxIteration)/(double)(detectedAccumulated.get(maxIteration - 1) + sampled.get(0));
	}
	
	public int getNumSampled(int iteration) {
		return sampled.get(iteration);
	}
	
	public int getAccumulatedNumSampled(int iteration) {
		return sampledAccumulated.get(iteration);
	}
	
	public int getNumDetected(int iteration) {
		return detected.get(iteration);
	}
	
	public int getAccumulatedNumDetected(int iteration) {
		return detectedAccumulated.get(iteration);
	}
	
	public int getMaxIteration() {
		return maxIteration;
	}
	
	public double getResonseRate() {
		return responseRate;
	}
}
