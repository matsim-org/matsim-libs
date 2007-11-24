/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPlanStats.java
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

package org.matsim.stats.algorithms;


/**
 * @author laemmel
 *
 */
public class BasicPlanStats extends AbstractPlanStatsAlgorithm  {
	
	private double xSum;
	private double xxSum;
	private int calls;
	
	public BasicPlanStats(){
		init();
	}
	
	public BasicPlanStats(PlanStatsI nextAlgo){
		this.nextAlgorithm = nextAlgo;
		init();
	}
	

	private double getAvg(){
		return xSum /  calls;
	}
	private double getVar(){
		return xxSum / calls - (getAvg() * getAvg());
	}
	
	@Override
	public void printStats(){
		System.out.print( " " + calls + " " + getAvg() + " " + getVar());
	}

	public void init(){
		xSum = 0;
		xxSum = 0;
		calls = 0;
	}

	@Override
	public void update(double score, int iteration) {
		xSum += score;
		xxSum += score * score;
		calls++;
	}

	@Override
	public String printStrStats() {
		return " " + calls + " " + getAvg() + " " + getVar();
	}
}
