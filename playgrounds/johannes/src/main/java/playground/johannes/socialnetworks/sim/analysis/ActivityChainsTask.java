/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChainsTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.util.TXTWriter;

/**
 * @author illenberger
 *
 */
public class ActivityChainsTask extends PlansAnalyzerTask {

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.sim.analysis.PlansAnalyzerTask#analyze(java.util.Set, java.util.Map)
	 */
	@Override
	public void analyze(Set<Plan> plans, Map<String, DescriptiveStatistics> results) {
		ActivityChains module = new ActivityChains();
		
		TObjectDoubleHashMap<String> chains = module.chains(plans);
		
		if(outputDirectoryNotNull()) {
			try {
				TXTWriter.writeMap(chains, "chain", "n", getOutputDirectory() + "/actchains.txt", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
