/* *********************************************************************** *
 * project: org.matsim.*
 * EstimDiffContainer.java
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
package playground.johannes.socialnetworks.snowball2.sim.postprocess;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

/**
 * @author illenberger
 *
 */
public class EstimDiffContainer {

	protected TIntIntHashMap kSum = new TIntIntHashMap();
	
	protected TIntDoubleHashMap pSum = new TIntDoubleHashMap();
	
	protected TIntIntHashMap kCount = new TIntIntHashMap();
}
