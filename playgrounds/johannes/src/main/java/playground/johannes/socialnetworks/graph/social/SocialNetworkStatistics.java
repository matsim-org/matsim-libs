/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkStatistics.java
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
package playground.johannes.socialnetworks.graph.social;





/**
 * @author illenberger
 *
 */
public class SocialNetworkStatistics {
	
	public static double ageCorrelation(SocialNetwork g) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;

		for (SocialTie e : g.getEdges()) {
			Ego v1 = (Ego) e.getVertices().getFirst();
			Ego v2 = (Ego) e.getVertices().getSecond();
			int age1 = v1.getPerson().getAge();
			int age2 = v2.getPerson().getAge();

			sum += 0.5 * (age1 + age2);
			squareSum += 0.5 * (Math.pow(age1, 2) + Math.pow(age2, 2));
			product += age1 * age2;			
		}
		
		double norm = 1 / (double)g.getEdges().size();
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}
}
