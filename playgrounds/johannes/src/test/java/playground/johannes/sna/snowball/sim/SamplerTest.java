/* *********************************************************************** *
 * project: org.matsim.*
 * SamplerTest.java
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
package playground.johannes.sna.snowball.sim;

import junit.framework.TestCase;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.socnetgen.sna.snowball.sim.SnowballSampler;
import org.matsim.core.utils.misc.CRCChecksum;
import playground.johannes.sna.TestCaseUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

/**
 * @author jillenberger
 * 
 */
public class SamplerTest extends TestCase {

	/**
	 * Runs a snowball simulation and compares the degree distributions for each
	 * iteration.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void test() throws FileNotFoundException, IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(TestCaseUtils.getPackageInputDirecoty(getClass()) + "test.graphml.gz");

		SnowballSampler<Graph, Vertex, Edge> sampler = new SnowballSampler<Graph, Vertex, Edge>();
		sampler.setSeedGenerator(new FixedSizeRandomPartition<Vertex>(1, 1));
		sampler.run(graph);

		Degree degree = Degree.getInstance();
		for (int it = 0; it <= sampler.getIteration(); it++) {
			Set<? extends SampledVertex> vertices = SnowballPartitions.createSampledPartition(sampler.getSampledGraph()
					.getVertices(), it);
			DescriptiveStatistics distr = degree.statistics(vertices);

			String reference = String.format("%1$s/k.%2$s.txt", TestCaseUtils.getPackageInputDirecoty(getClass()), it);
			String tmp = String.format("%1$s/k.%2$s.txt", TestCaseUtils.getOutputDirectory(), it);

			StatsWriter.writeHistogram(Histogram.createHistogram(distr, new LinearDiscretizer(1.0), false), "bin", "count", tmp);

			assertEquals(CRCChecksum.getCRCFromFile(reference), CRCChecksum.getCRCFromFile(tmp));
		}
	}
}
