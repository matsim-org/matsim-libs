/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.benjamin.analysis;

import org.matsim.api.core.v01.Id;

/**
 * @author benkick
 */

public class Row {
	private Id id;
	private Double score1;
	private Double score2;
	private double scoreDiff;
	private double personalIncome;
	private double homeX;
	private double homeY;

	private boolean isCarAvail;
	private boolean isSelectedPlanCar;

	//constructor: set all variables to null or 0.0
	public Row() {
		this.setId(null);
		this.setScore1(0.0);
		this.setScore2(0.0);
		this.setPersonalIncome(0.0);
		this.setHomeX(0.0);
		this.setHomeY(0.0);

		// true = yes, false = no //wtf is this null check?!?
		this.setCarAvail(null != null);
		this.setSelectedPlanCar(null != null);
	}

//===

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public void setScore1(Double score1) {
		this.score1 = score1;
	}

	public Double getScore1() {
		return score1;
	}

	public void setScore2(Double score2) {
		this.score2 = score2;
	}

	public Double getScore2() {
		return score2;
	}

	public void setPersonalIncome(double personalIncome) {
		this.personalIncome = personalIncome;
	}

	public double getPersonalIncome() {
		return personalIncome;
	}

	public void setHomeX(double homeX) {
		this.homeX = homeX;
	}

	public double getHomeX() {
		return homeX;
	}

	public void setHomeY(double homeY) {
		this.homeY = homeY;
	}

	public double getHomeY() {
		return homeY;
	}

	public void setCarAvail(boolean isCarAvail) {
		this.isCarAvail = isCarAvail;
	}

	public boolean isCarAvail() {
		return isCarAvail;
	}

	public void setSelectedPlanCar(boolean isSelectedPlanCar) {
		this.isSelectedPlanCar = isSelectedPlanCar;
	}

	public boolean isSelectedPlanCar() {
		return isSelectedPlanCar;
	}


	public double getScoreDiff() {
		this.scoreDiff = this.score2 - this.score1;
		return this.scoreDiff;
	}
}

