/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsEdgeSwitcher.java
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

/**
 * 
 */
package playground.johannes.graph.mcmc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.io.PajekClusteringColorizer;
import playground.johannes.graph.io.PajekColorizer;
import playground.johannes.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.socialnet.io.SNGraphMLWriter;
import playground.johannes.socialnet.io.SNPajekWriter;
import playground.johannes.socialnet.mcmc.ErgmDistance;
import playground.johannes.socialnet.mcmc.SNAdjacencyMatrix;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GibbsEdgeSwitcher {

	private Random random = new Random();
	
	public void sample(AdjacencyMatrix m, ConditionalDistribution d, int burninTime) {
		int N = m.getVertexCount();
		int M = m.getEdgeCount();
		int[][] edges = new int[M][2];
		int idx_ij = 0;
		for(int i = 0; i < N; i++) {
			for(int j = i+1; j < N; j++) {
				if(m.getEdge(i, j)) {
					edges[idx_ij][0] = i;
					edges[idx_ij][1] = j;
					idx_ij++;
				}
			}
		}
		int accept = 0;
		for(int it = 0; it < burninTime; it++) {
			idx_ij = random.nextInt(M);
			int i = edges[idx_ij][0];
			int j = edges[idx_ij][1];
			
			int idx_uv = random.nextInt(M);
			while(idx_uv == idx_ij) {
				idx_uv = random.nextInt(M);
			}
			
			int u = edges[idx_uv][0];
			int v = edges[idx_uv][1];
			
			if(i != u && j != v && j != u && i != v && !m.getEdge(i, u) && !m.getEdge(j, v)) {
				
				double p_change = d.changeStatistic(m, i, u, false)
						* d.changeStatistic(m, j, v, false)
						* 1/d.changeStatistic(m, i, j, true)
						* 1/d.changeStatistic(m, u, v, true); 
				double p = 1 / (1 + p_change);
				if(random.nextDouble() <= p) {
					m.removeEdge(i, j);
					m.removeEdge(u, v);
					m.addEdge(i, u);
					m.addEdge(j, v);
					
					edges[idx_ij][0] = i;
					edges[idx_ij][1] = u;
					edges[idx_uv][0] = j;
					edges[idx_uv][1] = v;
					
					accept++;
				}
			}
			if(it%100000==0) {
				System.out.println(it + " steps simulated. Accepted " + accept + " steps.");
				accept = 0;
				int sum = 0;
				for(int k_3 = 0; k_3 < m.getVertexCount(); k_3++) {
					sum += m.getNeighbours(k_3).size();
				}
				System.out.println("Mean degree is " + (sum/(float)m.getVertexCount()));
			}
			if(it%1000000==0) {
				Graph g = ((SNAdjacencyMatrix)m).getGraph();
				double c = GraphStatistics.getClusteringStatistics(g).getMean();
				System.out.println("*** Mean clustering is " + c + " ***");
			}
		}
	}

//	public void sample2(AdjacencyMatrix m, ConditionalDistribution d, int burninTime) {
//		int N = m.getVertexCount();
//		int M = m.getEdgeCount();
//		int[][] edges = new int[M][2];
//		int idx_ij = 0;
//		for(int i = 0; i < N; i++) {
//			for(int j = i+1; j < N; j++) {
//				if(m.getEdge(i, j)) {
//					edges[idx_ij][0] = i;
//					edges[idx_ij][1] = j;
//					idx_ij++;
//				}
//			}
//		}
//		int accept = 0;
//		for(int it = 0; it < burninTime; it++) {
//			idx_ij = random.nextInt(M);
//			int i = edges[idx_ij][0];
//			int j = edges[idx_ij][1];
//			
//			int idx_uv = random.nextInt(M);
//			while(idx_uv == idx_ij) {
//				idx_uv = random.nextInt(M);
//			}
//			
//			int u = random.nextInt(N);
//			int v = random.nextInt(N);
//			while(m.getEdge(u, v)) {
//				u = random.nextInt(N);
//				v = random.nextInt(N);
//			}
//			
//			if(i != u && j != v && j != u && i != v) {
//				
//				double p_change = d.evaluateChange_1(m, u, v, false)
//						* d.evaluateChange_0(m, i, j, true);
//				double p = 1 / (1 + p_change);
//				if(random.nextDouble() <= p) {
//					m.removeEdge(i, j);
//					m.addEdge(u, v);
//					
//					edges[idx_ij][0] = u;
//					edges[idx_ij][1] = v;
//					
//					accept++;
//				}
//			}
//			if(it%100000==0) {
//				System.out.println(it + " steps simulated. Accepted " + accept + " steps.");
//				accept = 0;
//				int sum = 0;
//				for(int k_3 = 0; k_3 < m.getVertexCount(); k_3++) {
//					sum += m.getNeighbours(k_3).size();
//				}
//				System.out.println("Mean degree is " + (sum/(float)m.getVertexCount()));
//			}
//			if(it%1000000==0) {
//				long time = System.currentTimeMillis();
//				Graph g = ((SNAdjacencyMatrix)m).getGraph();
//				
//				double c = GraphStatistics.getClusteringStatistics(g).getMean();
//				System.err.println(System.currentTimeMillis() - time);
//				double sum = 0;
//				time = System.currentTimeMillis();
//				for(int count = 0; count < m.getVertexCount(); count++) {
//					int k = m.getNeighbours(count).size();
//					if(k > 1)
//						sum += 2 * m.countTriangles(count) / (double)(k * (k-1));
//				}
//				double c2 = sum / (double)m.getVertexCount();
//				System.err.println(System.currentTimeMillis() - time);
//				System.out.println("*** Mean clustering is " + c + " / " + c2 + " ***");
//			}
//		}
//	}
	

	
	public static void main(String args[]) throws FileNotFoundException, RuntimeException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioImpl data = new ScenarioImpl(config);
		Population population = data.getPopulation();
		String outputDir = args[2];
		int samplesize = Integer.parseInt(args[3]);
		double theta4 = Double.parseDouble(args[4]);
		SNGraphMLReader<Person> reader = new SNGraphMLReader<Person>(population);
		SocialNetwork<Person> socialnet = reader.readGraph(args[1]);
		
		SNAdjacencyMatrix<Person> matrix = new SNAdjacencyMatrix<Person>(socialnet);
		
		
//		double theta1 = 8;
		double theta2 = 0;
//		double theta3 = 0;
		theta4 = 10000;
		ErgmTerm[] terms;
		Ergm ergm = new Ergm();
		terms = new ErgmTerm[2];
//		terms[0] = new ErgmDensity();
//		terms[0].setTheta(theta1);
		terms[0] = new ErgmDistance(matrix);
		terms[0].setTheta(theta2);
//		terms[2] = new ErgmAge();
//		terms[2].setTheta(theta3);
		terms[1] = new ErgmTriangles();
		terms[1].setTheta(theta4);
		ergm.setErgmTerms(terms);
		
		System.out.println("Starting sampling 2...");
//		terms[3].setTheta(1);
		GibbsEdgeSwitcher switcher = new GibbsEdgeSwitcher();
		switcher.sample(matrix, ergm, samplesize);
		System.out.println("Done.");
		
		socialnet = matrix.getGraph();
		
		System.out.println("Mean degree is " + GraphStatistics.getDegreeStatistics(socialnet).getMean());
		System.out.println("Mean clustering is " + GraphStatistics.getClusteringStatistics(socialnet).getMean());
		System.out.println("Age correlation is " + SocialNetworkStatistics.getAgeCorrelation(socialnet));
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(socialnet).absoluteDistribution(1000), outputDir + "edgelength.hist.txt");
		WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution(), outputDir + "degree.hist.txt");
		
		SNGraphMLWriter writer = new SNGraphMLWriter();
		writer.write(socialnet, outputDir + "socialnet2.graphml");
		
//		PajekDegreeColorizer<Ego<Person>, SocialTie> colorizer = new PajekDegreeColorizer<Ego<Person>, SocialTie>(socialnet, false);
		PajekClusteringColorizer<Ego<Person>, SocialTie> colorizer = new PajekClusteringColorizer<Ego<Person>, SocialTie>(socialnet);
		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
		pwriter.write(socialnet, colorizer, outputDir + "socialnet2.net");
	}
}
