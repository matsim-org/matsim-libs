/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmDistance.java
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
package playground.johannes.socialnet.mcmc;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.mcmc.AdjacencyMatrix;
import playground.johannes.graph.mcmc.Ergm;
import playground.johannes.graph.mcmc.ErgmDensity;
import playground.johannes.graph.mcmc.ErgmTerm;
import playground.johannes.graph.mcmc.ErgmTriangles;
import playground.johannes.graph.mcmc.GibbsSampler;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialNetworkStatistics;
import playground.johannes.socialnet.io.SNGraphMLWriter;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class ErgmDistance extends ErgmTerm {

	private double beta = 0.001;
	
	private double normBinSize = 1000;
	
	private TDoubleDoubleHashMap normConstants;
	
	public ErgmDistance(SNAdjacencyMatrix<?> m) {
		int n = m.getVertexCount();
		normConstants = new TDoubleDoubleHashMap();
		
		for(int i = 0; i < n; i++) {
			Coord c_i = m.getEgo(i).getCoord();
			
			for(int j = i+1; j < n; j++) {
//				if(i != j) {
					Coord c_j = m.getEgo(j).getCoord();
					double d = CoordUtils.calcDistance(c_i, c_j);
					double bin = Math.ceil(d / normBinSize);
					double count = normConstants.get(bin);
					count++;
					normConstants.put(bin, count);
//				}
			}
			
			
		}
		
		try {
			WeightedStatistics.writeHistogram(normConstants, "/Users/fearonni/vsp-work/socialnets/data-analysis/normconstants.hist.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public double evaluate(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		Coord c_i = ((SNAdjacencyMatrix<?>)m).getEgo(i).getCoord();
		Coord c_j = ((SNAdjacencyMatrix<?>)m).getEgo(j).getCoord();
		
		double d = CoordUtils.calcDistance(c_i, c_j);
		double norm = normConstants.get(Math.ceil(d/normBinSize));
		norm = Math.max(norm, 1.0);
		return -Math.log(1 / (beta * d * norm));
	}

	public static void main(String args[]) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioImpl data = new ScenarioImpl(config);
		Population population = data.getPopulation();
		
		SocialNetwork<Person> socialnet = new SocialNetwork<Person>(population);
		SNAdjacencyMatrix<Person> matrix = new SNAdjacencyMatrix<Person>(socialnet);
		
		double theta1 = 8;
		double theta2 = 1;
		double theta3 = 0;
		double theta4 = 0;
		
		Ergm ergm = new Ergm();
		ErgmTerm[] terms = new ErgmTerm[4];
		terms[0] = new ErgmDensity();
		terms[0].setTheta(theta1);
		terms[1] = new ErgmDistance(matrix);
		terms[1].setTheta(theta2);
		terms[2] = new ErgmAge();
		terms[2].setTheta(theta3);
		terms[3] = new ErgmTriangles();
		terms[3].setTheta(theta4);
		ergm.setErgmTerms(terms);
		
		System.out.println("Starting sampling 1...");
		
		GibbsSampler sampler = new GibbsSampler();
		sampler.sample(matrix, ergm, (int)1E8);
		
		System.out.println("Done.");
		
//		System.out.println("Starting sampling 2...");
//		terms[3].setTheta(1);
//		GibbsEdgeSwitcher switcher = new GibbsEdgeSwitcher();
//		switcher.sample(matrix, ergm, (int)1E6);
//		System.out.println("Done.");
		
		socialnet = matrix.getGraph();
		
		System.out.println("Mean degree is " + GraphStatistics.getDegreeStatistics(socialnet).getMean());
		System.out.println("Mean clustering is " + GraphStatistics.getClusteringStatistics(socialnet).getMean());
		System.out.println("Age correlation is " + SocialNetworkStatistics.getAgeCorrelation(socialnet));
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(socialnet).absoluteDistribution(1000), "/Users/fearonni/vsp-work/socialnets/data-analysis/edgelength.hist.txt");
		WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(socialnet).absoluteDistribution(), "/Users/fearonni/vsp-work/socialnets/data-analysis/degree.hist.txt");
		
		SNGraphMLWriter writer = new SNGraphMLWriter();
		writer.write(socialnet, "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnet.graphml");
		
//		PajekColorizererson> colorizer = new SNPajekDegreeColorizer<Person>(socialnet);
//		SNPajekWriter<Person> pwriter = new SNPajekWriter<Person>();
//		pwriter.write(socialnet, colorizer, "/Users/fearonni/vsp-work/socialnets/data-analysis/socialnet.net");
	}
}
