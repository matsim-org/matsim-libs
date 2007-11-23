/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPlanStatsAlgorithm.java
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
public abstract class AbstractPlanStatsAlgorithm implements PlanStatsI {

	protected PlanStatsI nextAlgorithm = null;
	
	abstract public void update(double score, int iteration);
	
	abstract public void printStats();
	
	abstract public String printStrStats();
	
	public void run(double score, int iteration) {
		
		update(score, iteration);
		if (this.nextAlgorithm != null)
			this.nextAlgorithm.run(score, iteration);
	}
	
	public String printStr(){
		String tmp = printStrStats();
		if (this.nextAlgorithm != null){
			return tmp + this.nextAlgorithm.printStr();
		}
		return tmp;
	}
	
	
	public void print(){
		printStats();
		if (this.nextAlgorithm != null)
			this.nextAlgorithm.print();
	}
	
	public void setAlgorithm(PlanStatsI nextAlgo){
		nextAlgorithm = nextAlgo;
	}

}
