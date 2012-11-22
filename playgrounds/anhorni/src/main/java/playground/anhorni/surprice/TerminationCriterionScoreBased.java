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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controler.TerminationCriterion;

public class TerminationCriterionScoreBased implements TerminationCriterion {
	
	private double stoppingCriterionVal = 0.1;
	final private static int INDEX_BEST = 1;
	private Controler controler;
	private final static Logger log = Logger.getLogger(TerminationCriterionScoreBased.class);
	
	public TerminationCriterionScoreBased(double stoppingCriterionVal, Controler controler) {
		this.stoppingCriterionVal = stoppingCriterionVal;
		this.controler = controler;
	}

	@Override
	public boolean continueIterations(int iteration) {
		double prevBestScore = 0.0;
		if (iteration >= 1) {
			prevBestScore = this.controler.getScoreStats().getHistory()[INDEX_BEST][iteration - 1];
		}
		double bestScore = this.controler.getScoreStats().getHistory()[INDEX_BEST][iteration];
		
		if (Math.abs((bestScore - prevBestScore) / prevBestScore) < this.stoppingCriterionVal) {
			return false;
		}
		else {
			log.info("Run terminated at iteration " + iteration + ". Relative score diff: " + Math.abs((bestScore - prevBestScore) / prevBestScore));
			return true;
		}
	}
}
