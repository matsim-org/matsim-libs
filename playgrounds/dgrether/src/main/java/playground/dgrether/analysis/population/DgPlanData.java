/* *********************************************************************** *
 * project: org.matsim.*
 * DgPlanData
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
package playground.dgrether.analysis.population;

import org.matsim.api.core.v01.population.Plan;


public class DgPlanData {

	private Double score = null;

	private Plan plan;
	
	public DgPlanData() {
	}

	
	public Double getScore() {
		return score;
	}

	
	public void setScore(Double score) {
		this.score = score;
	}


	
	public Plan getPlan() {
		return plan;
	}


	
	public void setPlan(Plan plan) {
		this.plan = plan;
	}
	
	

}
