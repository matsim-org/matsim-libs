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
package org.matsim.contrib.parking.PC2.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.controler.Controler;

public class ParkingScoreManager {

	private ParkingBetas parkingBetas;
	private double parkingScoreScalingFactor;
	private double randomErrorTermScalingFactor;
	DoubleValueHashMap<Id> scores;
	
	public double getScore(Id id) {
		return scores.get(id);
	}
	
	public void addScore(Id id, double incValue) {
		scores.incrementBy(id, incValue);
	}
	
	
	public void prepareForNewIteration(){
		scores=new DoubleValueHashMap<Id>();
	}

	public void init(Controler controler) {
		this.parkingBetas=new ParkingBetas();
		this.parkingBetas.setParkingWalkBeta(controler.getConfig().getParam("parkingChoice", "parkingWalkBeta"));
		this.parkingBetas.setParkingCostBeta(controler.getConfig().getParam("parkingChoice", "parkingCostBeta"));

		this.parkingScoreScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice", "parkingScoreScalingFactor"));
		this.randomErrorTermScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice", "randomErrorTermScalingFactor"));
	}

}
