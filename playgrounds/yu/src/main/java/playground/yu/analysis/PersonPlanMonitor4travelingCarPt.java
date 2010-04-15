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

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.utils.misc.Time;

import playground.yu.utils.DebugTools;

/**
 * monitor distance and time of leg
 * 
 * @author yu
 * 
 */
public class PersonPlanMonitor4travelingCarPt {
	private double legDepTime, legArrTime = Double.NaN,
			legDurCar = 0.0/* [h] */, legDurPt = 0.0/* [h] */,
			actStartTime = Double.NaN, actEndTime = Double.NaN,
			actDur = 0.0/* [h] */, firstActEndTime;

	private int idx;
	private Plan plan;
	private boolean stuck = false;

	/**
	 * 
	 */
	public PersonPlanMonitor4travelingCarPt(Plan plan) {
		idx = -1;
		this.plan = plan;
	}

	public void setLegDepTime(double depTime) {
		idx += 1;// PlanElement index grows
		this.legDepTime = depTime;
		this.legArrTime = Double.NaN;
	}

	public void setLegArrTime(double arrTime, Network network) {
		this.legArrTime = arrTime;
		if (this.idx % 2 == 0)
			throw new RuntimeException("idx should not be an even number");
		TransportMode mode = ((Leg) this.plan.getPlanElements().get(idx))
				.getMode();

		if (mode.equals(TransportMode.car))
			this.legDurCar += this.calcLegTravelTime_h();
		else if (mode.equals(TransportMode.pt))
			this.legDurPt += this.calcLegTravelTime_h();

		this.actStartTime = Double.NaN;
	}

	public void setActStartTime(double startTime) {
		this.idx += 1;// PlanElement index grows
		this.actStartTime = startTime;
		this.actEndTime = Double.NaN;
	}

	public void setActEndTime(double endTime, ActivityParams actParams) {
		if (this.idx == -1)// if first act without startTime
			this.idx += 1;
		this.actEndTime = endTime;
		this.actDur += this.calcActDuration_h(actParams);
		this.legDepTime = Double.NaN;
	}

	public double getTotalTravelTimesCar_h() {
		if (this.stuck)
			return 24.0 + legDurCar;
		return legDurCar;
	}

	public double getTotalTravelTimesPt_h() {
		if (this.stuck)
			return 24.0 + legDurPt;
		return legDurPt;
	}

	public double getTotalPerformTime_h(CharyparNagelScoringConfigGroup scoring) {
		// if (this.stuck) return 0.0;
		if (this.idx % 2 == 1)
			throw new RuntimeException(PersonPlanMonitor4travelingCarPt.class
					.getName()
					+ "\tline:\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\tthis.idx%2=1, it's impossible!!!\tfrom person\t"
					+ this.plan.getPerson());

		List<PlanElement> pes = this.plan.getPlanElements();

		String actType = ((Activity) pes.get(this.idx)).getType();
		if (Double.isNaN(this.actEndTime)
				&& (actType.startsWith("h") || actType.equals("tta"))
				&& this.idx == pes.size() - 1) {
			// last act (home or tta)
			// this.actEndTime = 24.0 * 3600.0 - 1.0;
			this.actDur += this.calcActDuration_h(scoring
					.getActivityParams(actType));
		}
		if (Double.isNaN(actDur))
			throw new RuntimeException(PersonPlanMonitor4travelingCarPt.class
					.getName()
					+ "\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\tactDur isNaN");
		return this.actDur;
	}

	public boolean isStuck() {
		return stuck;
	}

	/**
	 * @return duration of this leg in [h]
	 */
	private double calcLegTravelTime_h() {
		return (this.legArrTime - this.legDepTime) / 3600.0;
	}

	private double calcActDuration_h(ActivityParams actParams) {
		String actType = actParams.getType();
		if (this.idx == 0/* fist act */&& actType.startsWith("h")) {
			this.firstActEndTime = this.actEndTime + 3600.0 * 24.0;
			return 0.0;
		}

		double typicalDuration_h = actParams.getTypicalDuration()/* [s] *// 3600.0, //
		zeroUtilityDuration_h = typicalDuration_h
				* Math.exp(-10.0 / typicalDuration_h / actParams.getPriority());

		double actStart = actStartTime, actEnd;

		if (!actType.startsWith("h")) {
			double openTime = actParams.getOpeningTime(), closeTime = actParams
					.getClosingTime();
			if (Double.isNaN(this.actStartTime))
				this.actStartTime = openTime;

			actStart = this.actStartTime;

			if (Double.isNaN(this.actEndTime))
				this.actEndTime = closeTime;

			actEnd = this.actEndTime;

			if (openTime >= 0 && this.actStartTime < openTime)
				actStart = openTime;
			if (closeTime >= 0 && closeTime < this.actEndTime)
				actEnd = closeTime;
			if (openTime >= 0
					&& closeTime >= 0
					&& (openTime > this.actEndTime || closeTime < this.actStartTime)) {
				// agent could not perform action
				actStart = this.actEndTime;
				actEnd = this.actEndTime;
			}
		} else/* act==home */{
			actEnd = this.firstActEndTime;
		}

		double performingTime_h = (actEnd - actStart) / 3600.0;
		performingTime_h = Math.max(performingTime_h, 0.0);
		double durAttr = typicalDuration_h
				* Math.log(performingTime_h / zeroUtilityDuration_h);
		if (Double.isNaN(durAttr))
			throw new RuntimeException(PersonPlanMonitor4travelingCarPt.class
					.getName()
					+ "\t"
					+ DebugTools.getLineNumber(new Exception())
					+ "\tdurAttr isNaN");
		// System.out.println("PlanMonitor:\tperson:\t"
		// + plan.getPerson().getId().toString() + "\tidx=\t" + idx
		// + "\tactType\t" + actType + "\tdurAttr\t" + durAttr
		// + "\twith performingTime\t" + performingTime_h
		// + " [h]\tactEnd\t" + Time.writeTime(actEnd) + "\tactStart\t"
		// + Time.writeTime(actStart));
		return Math.max(durAttr, 0);
	}

	public void notifyStuck() {
		this.stuck = true;
	}

}
