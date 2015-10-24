/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultEstimator.java
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
package playground.johannes.studies.smallworld;

import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.PiEstimator;

/**
 * @author illenberger
 *
 */
public class DefaultEstimator implements PiEstimator {

	@Override
	public void update(SampledGraph graph) {
	}

	@Override
	public double probability(SampledVertex vertex) {
		return 1;
	}

	@Override
	public double probability(SampledVertex vertex, int iteration) {
		return 1;
	}

}
