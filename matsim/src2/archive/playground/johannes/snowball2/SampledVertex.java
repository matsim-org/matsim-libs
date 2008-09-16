/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertex.java
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
package playground.johannes.snowball2;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class SampledVertex extends UndirectedSparseVertex {

	private static final Logger logger = Logger.getLogger(SampledVertex.class);
	
	private static final UserDataContainer.CopyAction.Shared COPY_ACT = new UserDataContainer.CopyAction.Shared();
	
	private static final String WAVE_SAMPLED_KEY = "wavesampled";
	
	private static final String WAVE_DETECTED_KEY = "wavedetected";
	
	private static final String SAMPLE_PROBA_KEY = "sampleproba";
	
	private static final String VISITED_KEY = "visited";
	
	private static final String IS_NON_RESPONDING_KEY = "isnonresponding";
	
	public SampledVertex(int waveDeteceted) {
		super();
		addUserDatum(WAVE_DETECTED_KEY, waveDeteceted, COPY_ACT);
		addUserDatum(VISITED_KEY, 1, COPY_ACT);
	}
	
	public void setSampled(int wave) {
		if(getWaveSampled() < 0)
			addUserDatum(WAVE_SAMPLED_KEY, wave, COPY_ACT);
		else
			logger.warn("Vertex has already been sampled!");
	}
	
	public int getWaveDetected() {
		return (Integer)getUserDatum(WAVE_DETECTED_KEY);
	}
	
	public int getWaveSampled() {
		Integer i = (Integer)getUserDatum(WAVE_SAMPLED_KEY);
		if(i == null)
			return -1;
		else
			return i;
	}

	public boolean isAnonymous() {
		if(getWaveSampled() < 0)
			return true;
		else
			return false;
	}
	
	public void setSampleProbability(double p) {
		setUserDatum(SAMPLE_PROBA_KEY, p, COPY_ACT);
	}
	
	public double getSampleProbability() {
		return (Double)getUserDatum(SAMPLE_PROBA_KEY);
	}
	
	public int getVisited() {
		return (Integer)getUserDatum(VISITED_KEY);
	}
	
	public void increaseVisited() {
		setUserDatum(VISITED_KEY, getVisited() + 1, COPY_ACT);
	}
	
	public boolean isNonResponding() {
		return (Boolean)getUserDatum(IS_NON_RESPONDING_KEY);
	}
	
	public void setIsNonResponding(boolean flag) {
		setUserDatum(IS_NON_RESPONDING_KEY, flag, COPY_ACT);
	}
}
