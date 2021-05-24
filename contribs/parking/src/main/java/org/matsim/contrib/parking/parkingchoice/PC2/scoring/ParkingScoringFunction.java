/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.PC2.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

public class ParkingScoringFunction implements BasicScoring{
	double score=0;
	private Plan plan;
	private ParkingScore parkingScoreManager;

	public ParkingScoringFunction(Plan plan, ParkingScore parkingScoreManager) {
		super();
		this.plan = plan;
		this.parkingScoreManager = parkingScoreManager;
	}
	
	@Override
	public void finish() {
		score = parkingScoreManager.getScore(plan.getPerson().getId());
	}

	@Override
	public double getScore() {
		return score;
	}

}
