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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.matsim.utils.geometry.shared.Coord;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.DegreeDistributions;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

//	static public double meanDegree(Graph g) {
//		return Descriptive.mean(DegreeDistributions.getDegreeValues(g.getVertices()));
//	}
	
//	static public double meanDegreeSampled(Graph g) {
//		Set<Vertex> vertices = new HashSet<Vertex>();
//		for(Object v : g.getVertices()) {
//			Boolean bool = (Boolean)((Vertex)v).getUserDatum(UserDataKeys.PARTICIPATE_KEY);
//			if(bool != null && bool == true) {
//				vertices.add((Vertex) v);
//			}
//		}
//		return Descriptive.mean(DegreeDistributions.getDegreeValues(vertices));
//	}
	
	static public Histogram1D createDegreeHistogram(Graph g, int min, int max, int ignoreWave) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Object o : g.getVertices()) {
			IntArrayList waves = (IntArrayList) ((Vertex)o).getUserDatum(UserDataKeys.WAVE_KEY);
			if(waves != null) {
				if(waves.get(0) != ignoreWave)
					vertices.add((Vertex) o);
			} else
				vertices.add((Vertex) o);
		}
		DoubleArrayList values = DegreeDistributions.getDegreeValues(vertices);
		if(min < 0 && max < 0) {
			values.sort();
			min = (int)values.get(0);
			max = (int)values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "Degree distribution");
	}
	
	static public Histogram1D createClusteringCoefficientsHistogram(Graph g, double min, double max, int ignoreWave) {
		Map<Vertex, Double> coeffs = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		DoubleArrayList values = new DoubleArrayList(coeffs.size());
		for(Vertex v : coeffs.keySet()) {
			if(((Vertex)v).degree() == 1)
				coeffs.put(v, 0.0);
			
			IntArrayList waves = (IntArrayList) (v).getUserDatum(UserDataKeys.WAVE_KEY);
			if(waves != null) {
				if(waves.get(0) != ignoreWave)
					values.add(coeffs.get(v));
			} else
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
			values.add(r.rankScore);
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "betweeness centrality distribution");
	}
	
	public static Histogram1D createAPLHistogram(Graph g, double min, double max) {
		Map<Vertex, Double> distances = edu.uci.ics.jung.statistics.GraphStatistics.averageDistances(g);
		DoubleArrayList values = new DoubleArrayList(distances.size());
		for(Double d : distances.values())
			values.add(d);
		
		if(min < 0 && max < 0) {
			values.sort();
			min = values.get(0);
			max = values.get(values.size()-1);
		}
		return createHistogram(values, min, max, "average path length distribution");
	}
	
	private static Histogram1D createHistogram(DoubleArrayList values, double min, double max, String title) {
		Histogram1D histogram = new Histogram1D(title, 100, min, max);
		
		int cnt = values.size();
		for(int i = 0; i < cnt; i++) {
			histogram.fill(values.get(i));
		}
		return histogram;
	}
	
//	static public void saveHistogram(Histogram1D histogram, String filename) {
//		try {
//			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
//			
//			for(int i = 0; i < 100; i++) {
//				writer.write(String.valueOf(i));
//				writer.write("\t");
//				writer.write(String.valueOf(histogram.binHeight(i)));
//				writer.newLine();
//			}
//			writer.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	static public int countIsolates(Graph g) {
		int sum = 0;
		for(Object v : g.getVertices()) {
			if(((Vertex)v).degree() == 0)
					sum++;
		}
		return sum;
	}
	
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
		
		for(Object e : g.getEdges()) {
			Pair p = ((Edge)e).getEndpoints();
			int d_v1 = ((Vertex)p.getFirst()).degree();
			int d_v2 = ((Vertex)p.getSecond()).degree();
			
			sum += (d_v1 + d_v2);
			squareSum += (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
			product += (d_v1 * d_v2);
		}
		
		double M_minus1 = 1/(double)g.numEdges();
		double normSumSquare = Math.pow((M_minus1 * 0.5 * sum), 2);
		double numerator = (M_minus1 * product) - normSumSquare;
		double denumerator = (M_minus1 * 0.5 * squareSum) - normSumSquare;
		
		return numerator/denumerator;
	}
	
//	public static void dumpHistograms(Graph g, String prefix, int igonreWave) {
//		/*
//		 * Degree histogram
//		 */
//		Histogram1D degreeHist = createDegreeHistogram(g, igonreWave);
//		try {
//			ChartUtilities.saveChartAsPNG(new File(prefix + "degree.png"), makeChart(degreeHist, "Degree distribution"), 1024, 768);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		/*
//		 * Clustering coefficient distribution
//		 */
//		Histogram1D ccHist = createClusteringCoefficientsHistogram(g, igonreWave);
//		try {
//			ChartUtilities.saveChartAsPNG(new File(prefix + "clustering.png"), makeChart(ccHist, "Clustering coefficient distribution"), 1024, 768);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
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
	
	public static Histogram1D getDistanceHistogram(Graph g) {
		DoubleArrayList values = new DoubleArrayList(g.numEdges());
		for(Object e : g.getEdges()) {
			Pair endPoints = ((Edge)e).getEndpoints(); 
			Vertex v1 = (Vertex) endPoints.getFirst();
			Vertex v2 = (Vertex) endPoints.getSecond();
			double x1 = (Double)v1.getUserDatum(UserDataKeys.X_COORD);
			double y1 = (Double)v1.getUserDatum(UserDataKeys.Y_COORD);
			double x2 = (Double)v2.getUserDatum(UserDataKeys.X_COORD);
			double y2 = (Double)v2.getUserDatum(UserDataKeys.Y_COORD);
			
			Coord c1 = new Coord(x1, y1);
			Coord c2 = new Coord(x2, y2);
			values.add(c1.calcDistance(c2));
		}
		
		return createHistogram(values, 0, 50, "Distance distribution");
	}
	
	public static void writeHistogram(Histogram1D hist, String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), makeChart(hist, hist.title()), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
