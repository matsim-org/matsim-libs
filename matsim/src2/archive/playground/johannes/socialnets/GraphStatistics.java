/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistics.java
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
package playground.johannes.socialnets;

import hep.aida.ref.Histogram1D;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

	public static double fracVertices = 1;
	
	public static String outputDir;
	
	static public Histogram createDegreeHistogram(Graph g, int min, int max, int ignoreWave) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Object o : g.getVertices()) {
			if(((Vertex)o).getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null)
				vertices.add((Vertex) o);
		}

		DoubleArrayList values = new DoubleArrayList(vertices.size());
		DoubleArrayList weights = new DoubleArrayList(vertices.size());

		try {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + ignoreWave + ".weights.txt");
		
		for(Vertex v : vertices) {
			int k = v.degree();
			values.add(k);
			writer.write(String.valueOf(k));
			writer.write("\t");
			Integer wave = (Integer) v.getUserDatum(UserDataKeys.SAMPLED_KEY);
			double w = 1;
			if(wave == null || k == 0)
				weights.add(1);
			else {
//				w = 1 / (1 - Math.pow((1 - fracVertices),k));
				w = 1;// / (Double)v.getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
				weights.add(w);
			}
			writer.write(String.valueOf(w));
			writer.newLine();
			writer.flush();
		}
		writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(min < 0 && max < 0) {
			DoubleArrayList copy = values.copy();
			copy.sort();
			min = (int)copy.get(0);
			max = (int)copy.get(values.size()-1);
		}

		Histogram hist = new Histogram(1.0, 0, max);
		for(int i = 0; i < values.size(); i++) {
			hist.add(values.get(i), weights.get(i));
		}
		System.out.println("Gamma exponent is estimated to " + estimatePowerLawExponent(values));
		
		return hist;
	}
	
	static public Histogram1D createClusteringCoefficientsHistogram(Graph g, double min, double max, int ignoreWave) {
		Map<Vertex, Double> coeffs = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		DoubleArrayList values = new DoubleArrayList(coeffs.size());
		for(Vertex v : coeffs.keySet()) {
			if(((Vertex)v).degree() == 1)
				coeffs.put(v, 0.0);
			
			if(((Vertex)v).getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null)
				values.add(coeffs.get(v));
		}
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "Clutering coefficients");
	}
	
	public static Histogram1D createBetweenessHistogram(Graph g, double min, double max) {
		BetweennessCentrality bc = new BetweennessCentrality(g, true, false);
		bc.evaluate();
		List<NodeRanking> rankings = bc.getRankings();
		DoubleArrayList values = new DoubleArrayList(rankings.size());
		for(NodeRanking r : rankings)
			if(r.vertex.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null)
				values.add(r.rankScore);
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "betweeness centrality distribution");
	}
	
	public static Histogram1D createAPLHistogram(Graph g, double min, double max) {
		ClusterSet clusters = new WeakComponentClusterer().extract(g);
		Map<Vertex, Double> distances = new HashMap<Vertex, Double>();
		for(int i = 0; i < clusters.size(); i++) {
			Graph subGraph = clusters.getClusterAsNewSubGraph(i);
			if(subGraph.numVertices() > 1)
				distances.putAll(edu.uci.ics.jung.statistics.GraphStatistics.averageDistances(subGraph));
		}
		DoubleArrayList values = new DoubleArrayList(distances.size());
		for(Vertex v : distances.keySet()) {
			if(v.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null)
				values.add(distances.get(v));
		}
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "average path length distribution");
	}
	
	private static Histogram1D createHistogram(DoubleArrayList values, double min, double max, String title) {
		if(max <= min) {
			/*
			 * Should never occur, but to avoid an exception...
			 */
			min = 0;
			max = 1;
		}
		Histogram1D histogram = new Histogram1D(title, 100, min, max);
		
		int cnt = values.size();
		for(int i = 0; i < cnt; i++) {
			histogram.fill(values.get(i));
		}
		return histogram;
	}
	
	private static Histogram1D createWeightedHistogram(DoubleArrayList values, DoubleArrayList weights, double min, double max, String title) {
		if(max <= min) {
			/*
			 * Should never occur, but to avoid an exception...
			 */
			min = 0;
			max = 1;
		}
		Histogram1D histogram = new Histogram1D(title, 100, min, max);
		
		int cnt = values.size();
		for(int i = 0; i < cnt; i++) {
			histogram.fill(values.get(i), weights.get(i));
		}
		return histogram;
	}
	
	static public int countIsolates(Graph g) {
		int sum = 0;
		for(Object v : g.getVertices()) {
			if(((Vertex)v).degree() == 0)
					sum++;
		}
		return sum;
	}
	
	/**
	 * @deprecated
	 */
	public static IntArrayList countEdgewiseSharedPartners(Graph g) {
		IntArrayList espCounts = new IntArrayList();
		
		for(Object o : g.getVertices()) {
			int esp = 0;
			List<Vertex> trippleEndPoints = new ArrayList<Vertex>();
			Vertex v = (Vertex)o;
			for(Object neighbor1 : v.getNeighbors()) {
				for(Object neighbor2 : ((Vertex)neighbor1).getNeighbors()) {
					if(neighbor2 != o && ((Vertex)neighbor2).findEdge(v) == null) {
						trippleEndPoints.add((Vertex) neighbor2);
					}
				}
			}
			
			int size = trippleEndPoints.size();
			for(int i = 0; i < size; i++) {
				Vertex v1 = trippleEndPoints.get(i);
				for(int k = i+1; k < size; k++) {
					Vertex v2 = trippleEndPoints.get(k);
					if(v1 == v2) {
						esp++;
					}
				}
			}
			if(esp > 0) {
				if(espCounts.size() > esp) {
					int val = espCounts.get(esp);
					espCounts.set(esp, val+1);
				} else {
					espCounts.setSize(esp+1);
					espCounts.set(esp, 1);
				}
			}
		}
		return espCounts;
	}
	
	/**
	 * @deprecated
	 */
	public static double averagePathLength(Graph g) {
		Map values = edu.uci.ics.jung.statistics.GraphStatistics.averageDistances(g);
		double sum = 0;
		for(Object d : values.values()) {
			sum += (Double)d;
		}
		
		return sum/(double)values.size();
	}
	
	public static double pearsonCorrelationCoefficient(Graph g) {
		int product = 0;
		int sum = 0;
		int squareSum = 0;
		double edges = 0;
		for (Object e : g.getEdges()) {
			Pair p = ((Edge) e).getEndpoints();
			Vertex v1 = (Vertex) p.getFirst();
			Vertex v2 = (Vertex) p.getSecond();
			
			if (v1.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null
					&& v2.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null) {
				int d_v1 = v1.degree();
				int d_v2 = v2.degree();

				double w = 1;//1 - Math.pow(1 - fracVertices, d_v1 + d_v2);
				
				sum += (d_v1 + d_v2) / w;
				squareSum += (Math.pow(d_v1, 2) + Math.pow(d_v2, 2)) / w;
				product += (d_v1 * d_v2) / w;
				
				edges += 1;
			}
		}
//		double M_minus1 = 1 / (double) g.numEdges();
		double M_minus1 = 1 / (double) edges;
		double normSumSquare = Math.pow((M_minus1 * 0.5 * sum), 2);
		double numerator = (M_minus1 * product) - normSumSquare;
		double denumerator = (M_minus1 * 0.5 * squareSum) - normSumSquare;

		return numerator / denumerator;
	}
	
	public static JFreeChart makeChart(Histogram1D hist, String title) {
		final XYSeriesCollection data = new XYSeriesCollection();
		final XYSeries wave = new XYSeries(title, false, true);
		
		for (int i = 0; i < 100; i++) {
			wave.add(hist.xAxis().binCentre(i), hist.binHeight(i));
		}

		data.addSeries(wave);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "title", "x", "y", data,
        PlotOrientation.VERTICAL,
        true,   // legend
        false,   // tooltips
        false   // urls
    );

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("x");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("y"));
		return chart;
	}
	
	public static Histogram1D getDistanceHistogram(Graph g, double min, double max) {
		DoubleArrayList values = new DoubleArrayList(g.numEdges());
		for(Object e : g.getEdges()) {
			Pair endPoints = ((Edge)e).getEndpoints(); 
			Vertex v1 = (Vertex) endPoints.getFirst();
			Vertex v2 = (Vertex) endPoints.getSecond();
			double x1 = (Double)v1.getUserDatum(UserDataKeys.X_COORD);
			double y1 = (Double)v1.getUserDatum(UserDataKeys.Y_COORD);
			double x2 = (Double)v2.getUserDatum(UserDataKeys.X_COORD);
			double y2 = (Double)v2.getUserDatum(UserDataKeys.Y_COORD);
			
			CoordImpl c1 = new CoordImpl(x1, y1);
			CoordImpl c2 = new CoordImpl(x2, y2);
			values.add(c1.calcDistance(c2));
		}
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		
		return createHistogram(values, min, max, "distance distribution");
	}
	
	public static void plotHistogram(Histogram1D hist, String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), makeChart(hist, hist.title()), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeHistogramNormalized(Histogram1D hist, String filename) {
		try {
			int maxBin = hist.minMaxBins()[1];
			double maxY = hist.binHeight(maxBin);
			
			
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write("x\ty");
			writer.newLine();
			for (int i = 0; i < 100; i++) {
				writer.write(String.valueOf(hist.xAxis().binCentre(i)));
				writer.write("\t");
				writer.write(String.valueOf(hist.binHeight(i)/maxY));
				writer.newLine();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void writeHistogram(Histogram1D hist, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.write("x\ty");
			writer.newLine();
			for (int i = 0; i < 100; i++) {
				writer.write(String.valueOf(hist.xAxis().binCentre(i)));
				writer.write("\t");
				writer.write(String.valueOf(hist.binHeight(i)));
				writer.newLine();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static double estimatePowerLawExponent(DoubleArrayList values) {
		values.sort();
		double min = values.get(0);
		double max = values.get(values.size()-1);
		Histogram1D hist = createHistogram(values, min, max, "");
		int maxBin = hist.minMaxBins()[1];
		double minVal = hist.xAxis().binCentre(maxBin);
		
		int count = 0;
		double logsum = 0;
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i) >= minVal) {
				logsum += Math.log(values.get(i)/minVal);
				count++;
			}
		}
		
		double gamma = 1 + (count/logsum);
		double maxY = hist.binHeight(maxBin);
		double scalesum = 0;
		for (int i = maxBin; i < 100; i++) {
			scalesum += (hist.binHeight(i)/maxY)/(Math.pow(hist.xAxis().binCentre(i),-gamma));
		}
		double scale = scalesum/100.0;
		System.out.println("Coefficient A is " + scale);
		return gamma;
	}
}
