/* *********************************************************************** *
 * project: org.matsim.*
 * BetweennessRScript.java
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

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.GraphWriterTable;
import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class BetweennessRScript extends AbstractRScript implements VertexStatistic {

	private Map<Vertex, Double> values;

	private final String vertexFile;
	
	private final String edgeFile;
	
	private final String valuesFile;
	

	public BetweennessRScript(String exePath, String tmpDir) {
		super(exePath, tmpDir);
		vertexFile = tmpDir + "vertices.txt";
		edgeFile = tmpDir + "edges.txt";
		valuesFile = tmpDir + "values.txt";
	}

	private String createScript() {
		StringBuilder script = new StringBuilder(loadGraphScript(vertexFile, edgeFile));
		script.append("cat(\"Calculating betweenness...\",sep=\"\\n\")");
		script.append(NEW_LINE);
		script.append("scores <- betweenness(net, gmode=\"graph\", cmode=\"undirected\", rescale=FALSE)");
		script.append(NEW_LINE);
		script.append("cat(\"Wrting values...\",sep=\"\\n\")");
		script.append(NEW_LINE);
		script.append("write.table(scores, \"");
		script.append(valuesFile);
		script.append("\")");
		script.append(NEW_LINE);
		
		return script.toString();
	}
	
	public double run(Graph g) {
		try {
			/*
			 * Export graph...
			 */
			GraphWriterTable writer = new GraphWriterTable(g);
			writer.write(vertexFile, edgeFile);
			/*
			 * Run R-Script...
			 */
			values = new HashMap<Vertex, Double>();
			if(execute(createScript()) == 0) {
				/*
				 * Import computed data...
				 */
				BufferedReader reader = IOUtils.getBufferedReader(valuesFile);
				String line = reader.readLine();
				int index = 0;
				while((line = reader.readLine()) != null) {
					double score = Double.parseDouble(line.split(" ")[1]);
					values.put(writer.getVertex(index), score);
					index++;
				}
				
				double sum = 0;
				for(Double d : values.values())
					sum += d;
				return sum/(double)values.size();
			} else {
				return Double.NaN;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Double.NaN;
		}
	}

	public Histogram getHistogram() {
		Histogram hist = new Histogram(100);
		for(Double d : values.values())
			hist.add(d);
		return hist;
	}

	public Histogram getHistogram(double min, double max) {
		Histogram hist = new Histogram(100, min, max);
		for(Double d : values.values())
			hist.add(d);
		return hist;
	}
}
