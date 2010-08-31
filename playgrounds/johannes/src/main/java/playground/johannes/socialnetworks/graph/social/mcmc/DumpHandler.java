/* *********************************************************************** *
 * project: org.matsim.*
 * DumpHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social.mcmc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixStatistics;
import playground.johannes.socialnetworks.graph.mcmc.SampleHandler;
import playground.johannes.socialnetworks.graph.social.analysis.AgeTask;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbabilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.Distance;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class DumpHandler implements SampleHandler<SocialSparseVertex> {

	private static final Logger logger = Logger.getLogger(DumpHandler.class);
	
	private SocialSparseGraph graph;
	
	private SocialSparseGraphBuilder builder;
	
	private long burnin;
	
	private long dumpInterval;
	
	private long logInterval;

	private Distribution edges = new Distribution();
	
	private Distribution degree = new Distribution();
	
	private Distribution clustering = new Distribution();
	
	private Distribution distance = new Distribution();
	
	protected String outputDir;
	
	private BufferedWriter writer;
	
	private AnalyzerTaskComposite analyzerTask;
	
	public DumpHandler(SocialSparseGraph graph, SocialSparseGraphBuilder builder, String outputDir) {
		this.graph = graph;
		this.builder = builder;
		this.outputDir = outputDir;
		try {
			writer = new BufferedWriter(new FileWriter(outputDir + "samplestats.txt"));
			writer.write("it\tm\t<k>\t<c_local>\t<c_global>\t<d>");
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		analyzerTask = initAnalyzerTask();
	}
	
	private AnalyzerTaskComposite initAnalyzerTask() {
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new GraphSizeTask());
		task.addTask(new DegreeTask());
		task.addTask(new TransitivityTask());
		task.addTask(new DistanceTask());
		task.addTask(new AgeTask());
		
//		AcceptanceProbabilityTask ptask = new AcceptanceProbabilityTask();
//		ptask.setDistanceCalculator(new CartesianDistanceCalculator());
//		task.addTask(ptask);
		
		return task;
	}
	
	public AnalyzerTaskComposite getAnalyzerTaks() {
		return analyzerTask; 
	}
	
	public long getBurnin() {
		return burnin;
	}

	public void setBurnin(long burnin) {
		this.burnin = burnin;
	}

	public long getDumpInterval() {
		return dumpInterval;
	}

	public void setDumpInterval(long dumpInterval) {
		this.dumpInterval = dumpInterval;
	}

	public long getLogInterval() {
		return logInterval;
	}

	public void setLogInterval(long logInterval) {
		this.logInterval = logInterval;
	}

	public boolean handle(AdjacencyMatrix<SocialSparseVertex> y, long iteration) {
		if(iteration % logInterval == 0) {
			log(y, iteration);
		}
		
		if(iteration >= burnin) {
			analyze(y, iteration);
			return false;
		} else if (iteration % dumpInterval == 0 && iteration > 0) {
			analyze(y, iteration);
			return true;
		} else {
			return true;
		}
	}

	private void log(AdjacencyMatrix<SocialSparseVertex> y, long iteration) {
		double m = y.countEdges();
		double k_mean = AdjacencyMatrixStatistics.getMeanDegree(y);
		double c_local = AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y);
		double c_global = AdjacencyMatrixStatistics.getGlobalClusteringCoefficient(y);
	
//		y.synchronizeEdges(graph, builder);
//		double d_mean = SpatialGraphStatistics.edgeLengthDistribution(graph).mean();
		double d_mean = Double.NaN;
		logger.info(String.format(Locale.US, "m=%1$s, <k>=%2$.4f, <c_local>=%3$.4f, <c_global>=%4$.4f, <d>=%5$.4f", m, k_mean, c_local, c_global, d_mean));
	
		try {
			writer.write(String.format(Locale.US, "%6$s\t%1$s\t%2$.4f\t%3$.4f\t%4$.4f\t%5$.4f", m, k_mean, c_local, c_global, d_mean, iteration));
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public SpatialGraph analyze(AdjacencyMatrix<SocialSparseVertex> y, long iteration) {
		String currentOutputDir = String.format("%1$s%2$s/", outputDir, iteration);
		File file = new File(currentOutputDir);
		file.mkdirs();
		
		edges.add(y.countEdges());
		degree.add(AdjacencyMatrixStatistics.getMeanDegree(y));
		clustering.add(AdjacencyMatrixStatistics.getLocalClusteringCoefficient(y));
		
		y.synchronizeEdges(graph, builder);
		distance.add(new Distance().distribution(graph.getVertices()).mean());
		
		logger.info(String.format("VarK(m)=%1$.4f, VarK(<k>)=%2$.4f, VarK(<c_local>)=%3$.4f, VarK(<d>)=%4$.4f",
				edges.coefficientOfVariance(),
				degree.coefficientOfVariance(),
				clustering.coefficientOfVariance(),
				distance.coefficientOfVariance()));
		
		analyzerTask.setOutputDirectoy(currentOutputDir);
		Map<String, Double> stats = GraphAnalyzer.analyze(graph, analyzerTask);
		try {
			GraphAnalyzer.writeStats(stats, currentOutputDir + "stats.txt");
			
			dump(graph, currentOutputDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return graph;
	}
	
	protected void dump(SpatialSparseGraph graph, String iterationDir) {
		try {
			/* 
			 * graphML
			 */
			SocialGraphMLWriter writer = new SocialGraphMLWriter();
			writer.write(graph, String.format("%1$sgraph.graphml", iterationDir));
			/*
			 * KML
			 */
			SpatialGraphKMLWriter kmlWriter = new SpatialGraphKMLWriter();
			kmlWriter.setDrawEdges(false);
			kmlWriter.write(graph, String.format("%1$sgraph.k.kml", iterationDir));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
