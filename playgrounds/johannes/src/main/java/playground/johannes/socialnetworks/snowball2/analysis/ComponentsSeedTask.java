/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsSize.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.util.TXTWriter;

/**
 * @author illenberger
 *
 */
public class ComponentsSeedTask extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if (outputDirectoryNotNull()) {
			List<Set<SampledVertex>> comps = new Components().components(graph);
			TDoubleDoubleHashMap map = new TDoubleDoubleHashMap();
			
			for (Set<SampledVertex> component : comps) {
				int seeds = 0;
				for(SampledVertex v : component) {
					if(v.isSampled() && v.getIterationSampled() == 0) {
						seeds++;
					}
				}
				map.put(component.size(), seeds);
			}

			try {
				TXTWriter.writeMap(map, "size", "seeds", String.format("%1$s/componentSeeds.txt", getOutputDirectory()), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
