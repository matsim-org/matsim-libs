/* *********************************************************************** *
 * project: org.matsim.*
 * EstimRelation.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;

/**
 * @author illenberger
 *
 */
public class EstimRelation extends RunLocator<Double> {

	private static final Logger logger = Logger.getLogger(EstimRelation.class);
	
	private TDoubleDoubleHashMap targetDistr;
	
	public static void main(String[] args) throws IOException {
		File rootDir = new File(args[0]);
		final String dumpPattern = args[1];
		String analyzerKey = args[2];
		String outputTable = args[3];
		String graphFile = args[4];
		
		logger.info(String.format("Root dir = %1$s", rootDir));
		
		EstimRelation estimRelation = new EstimRelation();
		/*
		 * Load target degree distribution.
		 */
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(graphFile);
		
		estimRelation.targetDistr = Histogram.createHistogram(Degree.getInstance().statistics(graph.getVertices()), new LinearDiscretizer(1.0), false);
		/*
		 * Locate and load run dirs.
		 */
		Map<String, Double[]> dataTable = estimRelation.locate(rootDir, dumpPattern, analyzerKey);
		Map<String, Double> avrTable = estimRelation.averageValues(dataTable);
		/*
		 * Write.
		 */
		logger.info("Writing table...");
		List<String> dumpKeys = estimRelation.sortDumpKeys(dataTable);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputTable));
		writer.write("dump\tdiff");
		
		for(String dumpKey : dumpKeys) {
			writer.write(dumpKey);
			writer.write("\t");
			writer.write(avrTable.get(dumpKey).toString());
			writer.newLine();
		}
		writer.close();
	}
	
	@Override
	protected Double[] createObjectArray(int nRuns) {
		return new Double[nRuns];
	}

	@Override
	protected void inDirectory(String path, String dumpKey, int runIdx, Double[] objectArray) {
		File file = new File(path);
		TDoubleDoubleHashMap kDistr = loadDistribution(String.format("%1$s/obs/k.txt", file.getParent()));
		TDoubleDoubleHashMap pDistr = loadDistribution(String.format("%1$s/propa.txt", path));
		
		if(kDistr == null || pDistr == null)
			return;
		
		double kProd = 1;
		double pProd = 1;
		TDoubleDoubleIterator it = kDistr.iterator();
		for(int i = 0; i < kDistr.size(); i++) {
			it.advance();
			
			kProd *= targetDistr.get(it.key())/it.value();
			pProd *= 1/pDistr.get(it.key());
		}
		if(kProd == 0)
			System.err.println();
		objectArray[runIdx] = kProd - pProd;
	}


}
