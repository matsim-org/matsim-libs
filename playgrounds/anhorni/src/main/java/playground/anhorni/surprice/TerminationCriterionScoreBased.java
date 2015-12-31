/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.TerminationCriterion;

public class TerminationCriterionScoreBased implements TerminationCriterion {
	
	private double stoppingCriterionVal = 0.1;
	final private static int INDEX_BEST = 1;
	private MatsimServices controler;
	private final static Logger log = Logger.getLogger(TerminationCriterionScoreBased.class);
	private int finalIteration = 0;
	
	public TerminationCriterionScoreBased(double stoppingCriterionVal, MatsimServices controler) {
		this.stoppingCriterionVal = stoppingCriterionVal;
		this.controler = controler;
		this.finalIteration = this.controler.getConfig().controler().getLastIteration();
	}

	@Override
	public boolean continueIterations(int iteration) {
		
		if (iteration > this.controler.getConfig().controler().getLastIteration()) {
			this.finalIteration = iteration - 1;
			return false;
		}
		
		double prevBestScore = -999.0;
		double bestScore = 999.0;
		// let us do at least 10 iterations
		if (iteration > 10) {
			prevBestScore = this.controler.getScoreStats().getHistory()[INDEX_BEST][iteration - 2];
			bestScore = this.controler.getScoreStats().getHistory()[INDEX_BEST][iteration - 1];
		}		
		if (Math.abs((bestScore - prevBestScore) / prevBestScore) < this.stoppingCriterionVal) {
			log.info("Run terminated at iteration " + (iteration - 1) + ". Relative score diff: " + Math.abs((bestScore - prevBestScore) / prevBestScore));
			this.finalIteration = iteration - 1;
			return false;
		}
		else {			
			return true;
		}
	}

	public int getFinalIteration() {
		return finalIteration;
	}
}
