/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsNetworkGenerator.java
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
package playground.johannes.mcmc;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.BasicPerson;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.BasicPopulationImpl;
import org.matsim.basic.v01.BasicPopulationReaderV5;
import org.matsim.config.Config;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.utils.geometry.Coord;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.Vertex;
import playground.johannes.graph.generators.ErdosRenyiGenerator;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkFactory;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.SocialTie;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class GibbsNetworkGenerator {

	private static double STEPS = 100000000;//Double.MAX_VALUE;
	
	private static double DUMP_INTERVAL = 1000;
	
	private static double LAMBDA = 2;
	
	private static double THETA;
	
	private static int N = 5000;
	
	private static SocialNetwork<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> g; 
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
//		STEPS = Integer.parseInt(args[0]);
//		N = Integer.parseInt(args[1]);
		LAMBDA = Double.parseDouble(args[2]);
		DUMP_INTERVAL = Integer.parseInt(args[3]);
		String outputStats = args[4];
		String outputHist = args[5];
	
		
		
		BufferedWriter meanDegreeWriter = new BufferedWriter(new FileWriter(outputStats));
		meanDegreeWriter.write("it\tz\tP_Y\tn_p_1\tn_p_0\tn_edges");
		meanDegreeWriter.newLine();
		/*
		 * Initialize with a random graph.
		 */
		BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> pop = new BasicPopulationImpl<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>();
		PopulationReader reader = new BasicPopulationReaderV5(pop, null);
		reader.readFile("/Users/fearonni/vsp-work/socialnets/devel/MCMC/plans.100km.1.xml");
		ErdosRenyiGenerator<SocialNetwork<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, Ego<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, SocialTie> generator = 
			new ErdosRenyiGenerator<SocialNetwork<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, Ego<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>, SocialTie>(
					new SocialNetworkFactory<BasicPerson<BasicPlan,BasicKnowledge<BasicActivity>>>(pop));
		g = generator.generate(N, 0.001, 0);
//		System.out.println("Initial mean degree is " + GraphStatistics.getDegreeStatistics(g).getMean());
		
		
		
		 
//		Population pop = new Population(Population.NO_STREAMING);
//		MatsimPopulationReader reader = new MatsimPopulationReader(pop);
//		reader.readFile("/Users/fearonni/vsp-work/socialnets/devel/MCMC/plans.100km.1.xml");
//		g = new SocialNetwork<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>>(pop);		
//		double alpha = 1000 / (double)(N*(N-1)) * 2;
//		THETA = Math.log(alpha / (1 - alpha));
		/*
		 * Create a list of vertices.
		 */
		Random random = new Random(2);
		ArrayList<Ego<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>>> vertices = new ArrayList<Ego<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>>>(g.getVertices());
		int size = vertices.size();

		int sum_p1 = 0;
		int sum_p0 = 0;
		double sum_P_Y_0 = 0;
		double sum_P_Y_1 = 0;
		double sum_P_Y = 0;
		
		for(int i = 0; i < STEPS; i++) {
			/*
			 * Draw two random vertices.
			 */
			Ego v1 = vertices.get((int)Math.round(random.nextDouble() * (size-1)));
			random.nextDouble();
			Ego v2 = vertices.get((int)Math.round(random.nextDouble() * (size-1)));
			
			if(v1 != v2) {
				
				double P_Y_0;
				double P_Y_1;
				double P_Y;
				/*
				 * If v1 and v2 are connected, remove the edge.
				 */
				SparseEdge e = g.getEdge(v1, v2);
				if(e != null) {
					if(g.removeEdge(e) == false)
						throw new RuntimeException();
				}
				/*
				 * Edge is switched off.
				 */
				P_Y_0 = calcP_Y(v1.getEdges().size(), v2.getEdges().size()); 
				/*
				 * Insert an edge between v1 and v2.
				 */
				e = g.addEdge(v1, v2);
				if(e == null)
					throw new RuntimeException();
				/*
				 * Edge is switched on.
				 */
				P_Y_1 = calcP_Y(v1.getEdges().size(), v2.getEdges().size());
				/*
				 * Calc P_Y
				 */
//				P_Y = P_Y_1 / (P_Y_0 + P_Y_1);
				Coord c1 = ((BasicPlan) v1.getPerson().getPlans().get(0)).getIteratorAct().next().getCoord();
				Coord c2 = ((BasicPlan) v2.getPerson().getPlans().get(0)).getIteratorAct().next().getCoord();
				
					
//				double dist = c1.calcDistance(c2)/1000.0;
//				double alpha = 0.1* 1/(1+dist);
				double k1 = Math.max(1, v1.getEdges().size());
				double k2 = Math.max(1, v2.getEdges().size());
				
				double alpha = 0.5 * Math.pow(k1, - LAMBDA) * Math.pow(k2, - LAMBDA);
				THETA = Math.log(alpha / (1 - alpha));
				P_Y = Math.exp(THETA) / (1 + Math.exp(THETA));
				if(Double.isNaN(P_Y))
					throw new IllegalArgumentException();
//				double logit0 = Math.log(P_Y_0/(1-P_Y_0));
//				double logit1 = Math.log(P_Y_1/(1-P_Y_1));
//				P_Y = logit0/(logit0 + logit1);
				
				if(random.nextDouble() <= P_Y) {
					/*
					 * Leave the edge switched on.
					 */
					sum_p1++;
				} else {
					/*
					 * Remove the edge.
					 */
					if(g.removeEdge(e) == false)
						throw new RuntimeException();
					
					sum_p0++;
					
				}

				sum_P_Y_0 += P_Y_0;
				sum_P_Y_1 += P_Y_1;
				sum_P_Y += P_Y;
				
			
				
				if(i % DUMP_INTERVAL == 0) {
					double z = GraphStatistics.getDegreeStatistics(g).getMean();
					
					meanDegreeWriter.write(String.format("%1$s\t%2$s\t%3$s\t%4$s\t%5$s\t%6$s", i, z, sum_P_Y / (double)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					meanDegreeWriter.newLine();
					
					System.out.println(String.format("[%1$s] z = %2$s, P_Y_0 = %3$s, P_Y_1 = %4$s, P_Y = %5$s, p_1 = %6$s, p_0 = %7$s, edges = %8$s",
							i, z, sum_P_Y_0 / (float)DUMP_INTERVAL, sum_P_Y_1 / (float)DUMP_INTERVAL,
							sum_P_Y / (float)DUMP_INTERVAL, sum_p1, sum_p0, g.getEdges().size()));
					
					sum_P_Y_0 = 0;
					sum_P_Y_1 = 0;
					sum_P_Y = 0;
					sum_p1 = 0;
					sum_p0 = 0;
				}
			
			} else {
//				System.err.println("Selected same vertices...");
			}
		}
		meanDegreeWriter.close();
		
		System.out.println("Mean degree is " + GraphStatistics.getDegreeStatistics(g).getMean());
		System.out.println("Clustering is " + GraphStatistics.getClusteringStatistics(g).getMean());
		System.out.println("Degree correlation is " + GraphStatistics.getDegreeCorrelation(g));
		WeightedStatistics stats = GraphStatistics.getDegreeDistribution(g);
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), outputHist);	
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(g, true, 1000).absoluteDistribution(1000), "/Users/fearonni/vsp-work/socialnets/devel/MCMC/edgelength.txt");
	}
	
	private static double calcP_Y(int k1, int k2) {
//		return Math.exp(-LAMBDA * (k1 + k2) / N);
//		return Math.pow(k1, - LAMBDA) * Math.pow(k2, - LAMBDA); 
//		return Math.exp(1 * (- Math.abs(4000 - g.getEdges().size())));
//		return 1/(double)(1+Math.abs(3500 - g.getEdges().size()));
		return Math.exp(THETA*g.getEdges().size());
	}
}
