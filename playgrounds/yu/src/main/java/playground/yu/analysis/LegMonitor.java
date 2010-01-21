/* *********************************************************************** *
 * project: org.matsim.*
 * LegMonitor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * monitor distance and time of leg
 * 
 * @author yu
 * 
 */
public class LegMonitor {
	private double depTime, arrTime = Double.NaN, dist = 0.0/* [km] */,
			dur = 0.0/* [h] */;

	private int idx;
	private Plan plan;

	/**
	 * 
	 */
	public LegMonitor(Plan plan, double depTime) {
		idx = 1;
		this.plan = plan;
		this.depTime = depTime;
	}

	public void setDepTime(double depTime) {
		idx += 2;
		this.depTime = depTime;
		this.arrTime = Double.NaN;
	}

	public void setArrTime(double arrTime) {
		this.arrTime = arrTime;
		this.dur += this.calcDuration_h();
		this.dist += this.calcDistance_km();
	}

	public double getTotalDistances_km() {
		return dist;
	}

	public double getTotalDurations_h() {
		return dur;
	}

	/**
	 * @return leg Distance in [m]
	 * @see {@code org.matsim.core.scoring.charyparNagel.LegScoringFunction}
	 *      line 100
	 */
	private double calcDistance_km() {
		return ((Leg) this.plan.getPlanElements().get(this.idx)).getRoute()
				.getDistance() / 1000.0;
	}

	/**
	 * @return duration of this leg in [h]
	 */
	private double calcDuration_h() {
		return (this.arrTime - this.depTime) / 3600.0;
	}

	public void notifyStuck() {
		this.dur = 24.0;
		this.dist = 0.0;
	}

}
