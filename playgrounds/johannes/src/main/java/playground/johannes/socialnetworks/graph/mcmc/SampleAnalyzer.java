/* *********************************************************************** *
 * project: org.matsim.*
 * SampleAnalyzer.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class SampleAnalyzer<G extends Graph, E extends Edge, V extends Vertex> implements SamplerListener<V> {

	private static final Logger logger = Logger.getLogger(SampleAnalyzer.class);
	
	private final G graph;
	
	private final GraphBuilder<G, V, E> builder;
	
	private GraphMLWriter writer;
	
	private AnalyzerTask analyzerTask;
	
	private final String rootDirectory;
	
	private long infoInteraval;
	
	private long analysisInterval;
	
	private long maxIteration;
	
	public SampleAnalyzer(G graph, GraphBuilder<G, V, E> builder, String rootDirectory) {
		this.graph = graph;
		this.builder = builder;
		this.rootDirectory = rootDirectory;
	}
	
	public void setWriter(GraphMLWriter writer) {
		this.writer = writer;
	}

	public void setAnalyzerTask(AnalyzerTask analyzerTask) {
		this.analyzerTask = analyzerTask;
	}

	public void setInfoInteraval(long infoInteraval) {
		this.infoInteraval = infoInteraval;
	}

	public void setAnalysisInterval(long analysisInterval) {
		this.analysisInterval = analysisInterval;
	}

	public void setMaxIteration(long maxIteration) {
		this.maxIteration = maxIteration;
	}

	@Override
	public boolean beforeSampling(AdjacencyMatrix<V> y, long iteration) {
		if(iteration % analysisInterval == 0) {
			analyze(y, iteration);
		}
		
		if(iteration % infoInteraval == 0) {
			double m = y.countEdges();
			double k_mean = AdjacencyMatrixStatistics.getMeanDegree(y);
			double c_local = AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y);
			double c_global = AdjacencyMatrixStatistics.getGlobalClusteringCoefficient(y);
		
			logger.info(String.format(Locale.US, "m=%1$s, <k>=%2$.4f, <c_local>=%3$.4f, <c_global>=%4$.4f.", m, k_mean, c_local, c_global));
		}
		
		if(iteration > maxIteration)
			return false;
		else
			return true;
	}

	private void analyze(AdjacencyMatrix<V> y, long iteration) {
		y.synchronizeEdges(graph, builder);
	
		File file = new File(String.format("%1$s/%2$d/", rootDirectory, iteration));
		file.mkdirs();
		
		try {
			if(writer != null)
				writer.write(graph, String.format("%1$s/graph.graphml", file.getAbsolutePath()));
			
			if(analyzerTask != null)
				GraphAnalyzer.analyze(graph, analyzerTask, file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
