/* *********************************************************************** *
 * project: org.matsim.*
 * PlanScoreTrajectory.java
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

package playground.gregor.stats.algorithms;


/**
 * @author laemmel
 *
 */
public class PlanScoreTrajectory extends AbstractPlanStatsAlgorithm {

	private double[] TRAJECTORY;
	private int minIteration;
	private int iterations;
	
	public PlanScoreTrajectory(int iters, int minIter){
		minIteration = minIter;
		iterations = iters;
		init();
	}
	
	public PlanScoreTrajectory(PlanStats nextAlgo,int iters, int minIter){
		minIteration = minIter;
		iterations = iters;		
		this.nextAlgorithm = nextAlgo;
		init();
	}

	private void init(){
		TRAJECTORY = new double[iterations];
		for (int i = 0 ; i < iterations; i++) TRAJECTORY[i] = 0;
		
	}


	@Override
	public void printStats() {
		for (int i = 0; i < iterations;  i++)
			System.out.print(" " + TRAJECTORY[i] );

		
	}

	@Override
	public void update(double score, int iteration) {
		TRAJECTORY[iteration - minIteration] = score;
	}

	@Override
	public String printStrStats() {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < iterations; i++) {
			str.append(' ');
			str.append(TRAJECTORY[i]);
		}
		return str.toString();
	}

}
