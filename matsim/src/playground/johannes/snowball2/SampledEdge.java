/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEdge.java
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

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public class SampledEdge extends UndirectedSparseEdge {

	private static final UserDataContainer.CopyAction.Shared COPY_ACT = new UserDataContainer.CopyAction.Shared();
	
	private static final String WAVE_SAMPLED_KEY = "wavesampled";
	
	public SampledEdge(Vertex from, Vertex to, int waveSampled) {
		super(from, to);
		addUserDatum(WAVE_SAMPLED_KEY, waveSampled, COPY_ACT);
	}
	
	public int getWaveSampled() {
		return (Integer)getUserDatum(WAVE_SAMPLED_KEY);
	}
}
