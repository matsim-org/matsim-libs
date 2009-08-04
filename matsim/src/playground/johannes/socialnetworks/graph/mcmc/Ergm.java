/* *********************************************************************** *
 * project: org.matsim.*
 * Ergm.java
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
package playground.johannes.socialnetworks.graph.mcmc;



/**
 * @author illenberger
 *
 */
public class Ergm implements ConditionalDistribution {

	private ErgmTerm[] ergmTerms;
	
//	private TIntDoubleHashMap norm_i = new TIntDoubleHashMap();
	
	public void setErgmTerms(ErgmTerm[] terms) {
		ergmTerms = terms;
	}
	
	public ErgmTerm[] getErgmTerms() {
		return ergmTerms;
	}
	

	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		double h_y = evaluateExpHamiltonian(y, i, j, y_ij);
		if(Double.isInfinite(h_y))
			throw new IllegalArgumentException("H(y) must not be infinity!");
		else if(Double.isNaN(h_y))
			throw new IllegalArgumentException("H(y) must not be NaN!");
		
		
		return h_y;
	}

	public double evaluateExpHamiltonian(AdjacencyMatrix y, int i, int j, boolean y_ij) {
		double sum = 0;
		for(ErgmTerm term : ergmTerms) {
			double r =term.changeStatistic(y, i, j, y_ij); 
			sum += r; 
		}
//		System.out.println(sum);
		return Math.exp(sum);
	}
	
	public double getNormConstant(int i) {
//		return norm_i.get(i);
		return 1.0;
	}
	
	public void init(AdjacencyMatrix y) {
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/fearonni/Desktop/normvals.txt"));
//		
//		int n = y.getVertexCount();
//		
//		for(int i = 0; i < n; i++) {
//			double sum = 0;
//			for(int j = 0; j < n; j++) {
////				if(i != j) {
//					sum += 1/(1+changeStatistic(y, i, j, false));
////				}
//			}
//			
//			norm_i.put(i, 5/sum);
//			double dx = 50 - (i - Math.floor(i/100)*100);
//			double dy = 50 - Math.floor(i/100);
//			
//			double d = Math.sqrt(dx*dx + dy*dy);
//			writer.write(String.valueOf(d));
//			writer.write("\t");
//			writer.write(String.valueOf(sum));
//			writer.newLine();
//			
//		}
//		writer.close();
//		} catch (Exception e) {
//			
//		}
	}
}
