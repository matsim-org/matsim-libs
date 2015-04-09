/* *********************************************************************** *
 * project: org.matsim.*
 * ActUtilityParameters.java
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

package org.matsim.core.scoring.functions;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

/**
 * Class that converts the config parameters into parameters that are used by the scoring.
 * </p>
 * Design thoughts:<ul>
 * <li> Class name feels a bit like a misnomer to me since the class seems to make sense only for the
 * so-called Charypar-Nagel scoring, especially with the zeroUtilityDuration in the ctor.
 *   Yet maybe one might be able to rework the approach so that the most
 * important params (such as typical duration, slope at typical duration, ...) become universal. kai, nov'12
 * </ul>
 * 
 * @author nagel
 *
 */
public class ActivityUtilityParameters implements MatsimParameters {

	/**
	 * This is now deliberately an unmodifiable object which can only instantiated by a builder.  If you want/need to modify
	 * this design, please talk to kai nagel or michael zilske or marcel rieser.  kai, nov'12
	 * 
	 * @author nagel
	 */
	public static class Builder {
		private String type;
		private double priority = 1. ;
		private double typicalDuration_s;
		private double closingTime;
		private double earliestEndTime;
		private double latestStartTime;
		private double minimalDuration;
		private double openingTime;
		private boolean scoreAtAll;

		/**
		 * empty constructor; deliberately permitted
		 */
		public Builder() {
		}

		/**
		 * Convenience constructor for main use case
		 */
		public Builder(ActivityParams ppp ) {
			this.type = ppp.getActivityType() ;
			this.priority = ppp.getPriority() ;
			this.typicalDuration_s = ppp.getTypicalDuration() ;
			this.closingTime = ppp.getClosingTime() ;
			this.earliestEndTime = ppp.getEarliestEndTime() ;
			this.latestStartTime = ppp.getLatestStartTime() ;
			this.minimalDuration = ppp.getMinimalDuration() ;
			this.openingTime = ppp.getOpeningTime() ;
			this.scoreAtAll = ppp.isScoringThisActivityAtAll() ;
		}

		public void setType(String type) {
			this.type = type;
		}

		public void setPriority(double priority) {
			this.priority = priority;
		}

		public void setTypicalDuration_s(double typicalDurationS) {
			typicalDuration_s = typicalDurationS;
		}

		public void setClosingTime(double closingTime) {
			this.closingTime = closingTime;
		}

		public void setEarliestEndTime(double earliestEndTime) {
			this.earliestEndTime = earliestEndTime;
		}

		public void setLatestStartTime(double latestStartTime) {
			this.latestStartTime = latestStartTime;
		}

		public void setMinimalDuration(double minimalDuration) {
			this.minimalDuration = minimalDuration;
		}

		public void setOpeningTime(double openingTime) {
			this.openingTime = openingTime;
		}

		public void setScoreAtAll(boolean scoreAtAll) {
			this.scoreAtAll = scoreAtAll;
		}

		public ActivityUtilityParameters create() {
			ActivityUtilityParameters params = new ActivityUtilityParameters(this.type) ;
			params.setScoreAtAll(this.scoreAtAll) ;
			params.setPriorityAndTypicalDuration(this.priority, this.typicalDuration_s) ;
			params.setClosingTime(this.closingTime) ;
			params.setEarliestEndTime(this.earliestEndTime) ;
			params.setLatestStartTime(this.latestStartTime) ;
			params.setMinimalDuration(this.minimalDuration) ;
			params.setOpeningTime(this.openingTime) ;
			return params ;
		}
	}

	private final String type;
	private double typicalDuration_s;

	/**
	 * 	"duration at which the [performance] utility starts to be positive"
	 * (from Dave's paper, ga-acts-iatbr03.tex, though he called it t_0)
	 * (In decimal number of hours.)
	 */
	private double zeroUtilityDuration_h; // in hours!
	private double minimalDuration = -1;
	private double openingTime = -1;
	private double closingTime = -1;
	private double latestStartTime = -1;
	private double earliestEndTime = -1;
	private boolean scoreAtAll=true;

	// use factory.  nov'12
	/*package!*/ ActivityUtilityParameters(final String type) {
		this.type = type;	
	}

	/*package!*/ final void setScoreAtAll(boolean scoreAtAll) {
		this.scoreAtAll = scoreAtAll;
	}

	/*package!*/ final void setPriorityAndTypicalDuration(final double priority, final double typicalDuration_s) {
		//if typical duration is <=48 seconds (and priority=1) then zeroUtilityDuration becomes 0.0 because of the double precision. This means it is not possible
		// to have activities with a typical duration <=48 seconds (GL/June2011)

		this.typicalDuration_s = typicalDuration_s;

		//		this.zeroUtilityDuration_h = (typicalDuration_s / 3600.0)
		//		* Math.exp( -10.0 / (typicalDuration_s / 3600.0) / priority );
		// replacing the above two lines with the two lines below causes the test failure of ReRoutingTest.  kai, nov'12

		this.zeroUtilityDuration_h = CharyparNagelScoringUtils.computeZeroUtilityDuration(priority,
				typicalDuration_s) / 3600. ;

		// example: pt interaction activity with typical duration = 120sec.
		// 120/3600 * exp( -10 / (120 / 3600) ) =  1.7 x 10^(-132)  (!!!!!!!!!!)
		// In consequence, even a pt interaction of one seconds causes a fairly large utility.

		if (this.scoreAtAll && this.zeroUtilityDuration_h <= 0.0) {
			throw new RuntimeException("zeroUtilityDuration of type " + type + " must be greater than 0.0. Did you forget to specify the typicalDuration?");
		}
	}
	
	/*package!*/ final void setMinimalDuration(final double dur) {
		this.minimalDuration = dur;
	}

	/*package!*/ final void setOpeningTime(final double time) {
		this.openingTime = time;
	}

	/*package!*/ final void setClosingTime(final double time) {
		this.closingTime = time;
	}

	/*package!*/ final void setLatestStartTime(final double time) {
		this.latestStartTime = time;
	}

	/*package!*/ final void setEarliestEndTime(final double time) {
		this.earliestEndTime = time;
	}

	public final String getType() {
		return this.type;
	}

	public final double getTypicalDuration() {
		return this.typicalDuration_s;
	}

	public final double getZeroUtilityDuration_h() {
		return this.zeroUtilityDuration_h;
	}

	public final double getMinimalDuration() {
		return this.minimalDuration;
	}

	public final double getOpeningTime() {
		return this.openingTime;
	}

	public final double getClosingTime() {
		return this.closingTime;
	}

	public final double getLatestStartTime() {
		return this.latestStartTime;
	}

	public final double getEarliestEndTime() {
		return this.earliestEndTime;
	}

	public final boolean isScoreAtAll() {
		return scoreAtAll;
	}

}
