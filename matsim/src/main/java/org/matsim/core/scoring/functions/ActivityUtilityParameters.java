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
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.utils.misc.OptionalTime;

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
public final class ActivityUtilityParameters implements MatsimParameters {

	public interface ZeroUtilityComputation {
		double computeZeroUtilityDuration_s( final double priority, final double typicalDuration_s ) ;
	}
	public static final class SameAbsoluteScore implements ZeroUtilityComputation {
		@Override
		public double computeZeroUtilityDuration_s(double priority, double typicalDuration_s) {
			final double priority1 = priority;
			final double typicalDuration_s1 = typicalDuration_s;
			final double zeroUtilityDuration = typicalDuration_s1 * Math.exp( -10.0 / (typicalDuration_s1 / 3600.0) / priority1 );
			// ( the 3600s are in there because the original formulation was in "hours".  So the values in seconds are first
			// translated into hours.  kai, sep'12 )

			return zeroUtilityDuration;
		}
	}
	public static final class SameRelativeScore implements ZeroUtilityComputation {
		@Override
		public double computeZeroUtilityDuration_s(double priority, double typicalDuration_s) {
			final double priority1 = priority;
			final double typicalDuration_s1 = typicalDuration_s;
			final double zeroUtilityDuration = typicalDuration_s1 * Math.exp( -1.0 / priority1 );

			return zeroUtilityDuration;
		}
	}


	/**
	 * This is now deliberately an unmodifiable object which can only instantiated by a builder.  If you want/need to modify
	 * this design, please talk to kai nagel or michael zilske or marcel rieser.  kai, nov'12
	 *
	 * @author nagel
	 */
	public final static class Builder {
		private String type;
		private double priority = 1. ;
		private OptionalTime typicalDuration_s = OptionalTime.undefined();
		private OptionalTime closingTime = OptionalTime.undefined();
		private OptionalTime earliestEndTime = OptionalTime.undefined();
		private OptionalTime latestStartTime = OptionalTime.undefined();
		private OptionalTime minimalDuration = OptionalTime.undefined();
		private OptionalTime openingTime = OptionalTime.undefined();
		private boolean scoreAtAll = true;
		private ZeroUtilityComputation zeroUtilityComputation = new SameRelativeScore();

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
			this.typicalDuration_s = ppp.getTypicalDuration();
			this.closingTime = ppp.getClosingTime();
			this.earliestEndTime = ppp.getEarliestEndTime();
			this.latestStartTime = ppp.getLatestStartTime();
			this.minimalDuration = ppp.getMinimalDuration();
			this.openingTime = ppp.getOpeningTime();
			this.scoreAtAll = ppp.isScoringThisActivityAtAll() ;
			switch( ppp.getTypicalDurationScoreComputation() ) {
				case relative:
					this.zeroUtilityComputation = new SameRelativeScore() ;
					break;
				case uniform:
					this.zeroUtilityComputation = new SameAbsoluteScore() ;
					break;
				default:
					throw new RuntimeException("not defined");
			}
			// seems to be somewhat overkill to set a computation method that is only used in the builder ... but the builder has a method to
			// (re)set the
		}


		public Builder setType(String type) {
			this.type = type;
			return this;
		}

		public Builder setPriority(double priority) {
			this.priority = priority;
			return this;
		}

		public Builder setTypicalDuration_s(double typicalDurationS) {
			typicalDuration_s = OptionalTime.defined(typicalDurationS);
			return this;
		}

		public Builder setClosingTime(double closingTime) {
			this.closingTime = OptionalTime.defined(closingTime);
			return this;
		}

		public Builder setEarliestEndTime(double earliestEndTime) {
			this.earliestEndTime = OptionalTime.defined(earliestEndTime);
			return this;
		}

		public Builder setLatestStartTime(double latestStartTime) {
			this.latestStartTime = OptionalTime.defined(latestStartTime);
			return this;
		}

		public Builder setMinimalDuration(double minimalDuration) {
			this.minimalDuration = OptionalTime.defined(minimalDuration);
			return this;
		}

		public Builder setOpeningTime(double openingTime) {
			this.openingTime = OptionalTime.defined(openingTime);
			return this;
		}

		public Builder setScoreAtAll(boolean scoreAtAll) {
			this.scoreAtAll = scoreAtAll;
			return this;
		}

		public ActivityUtilityParameters build() {
			ActivityUtilityParameters params = new ActivityUtilityParameters(this.type) ;
			params.scoreAtAll = this.scoreAtAll;
			this.typicalDuration_s.ifDefined( duration -> params.typicalDuration_s = duration ) ;
			this.typicalDuration_s.ifDefined( duration -> params.zeroUtilityDuration_h = this.zeroUtilityComputation.computeZeroUtilityDuration_s(priority, duration ) / 3600. );
			// (I think that the only way in which the typical duration can be undefined is if the activity is not scored at all.  Maybe change the condition. kai, may'22)
			params.closingTime = this.closingTime;
			params.earliestEndTime = this.earliestEndTime;
			params.latestStartTime = this.latestStartTime;
			params.minimalDuration = this.minimalDuration;
			params.openingTime = this.openingTime;
			params.checkConsistency();
			return params ;
		}

		public Builder setZeroUtilityComputation( ZeroUtilityComputation zeroUtilityComputation ) {
			this.zeroUtilityComputation = zeroUtilityComputation;
			return this;
		}
	}

	private final String type;
	private double typicalDuration_s = 0;

	/**
	 * 	"duration at which the [performance] utility starts to be positive"
	 * (from Dave's paper, ga-acts-iatbr03.tex, though he called it t_0)
	 * (In decimal number of hours.)
	 */
	private double zeroUtilityDuration_h = 0; // in hours!
	private OptionalTime minimalDuration = OptionalTime.undefined();
	private OptionalTime openingTime = OptionalTime.undefined();
	private OptionalTime closingTime = OptionalTime.undefined();
	private OptionalTime latestStartTime = OptionalTime.undefined();
	private OptionalTime earliestEndTime = OptionalTime.undefined();
	private boolean scoreAtAll=true;

	private ActivityUtilityParameters(final String type) {
		this.type = type;
	}

	private void checkConsistency() {
		//if typical duration is <=48 seconds (and priority=1) then zeroUtilityDuration becomes 0.0 because of the double precision. This means it is not possible
		// to have activities with a typical duration <=48 seconds (GL/June2011)
		if (this.scoreAtAll && this.zeroUtilityDuration_h == 0.0) {
			throw new RuntimeException("zeroUtilityDuration of type " + type + " must be greater than 0.0. Did you forget to specify the typicalDuration?");
		}
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

	public final OptionalTime getMinimalDuration() {
		return this.minimalDuration;
	}

	public final OptionalTime getOpeningTime() {
		return this.openingTime;
	}

	public final OptionalTime getClosingTime() {
		return this.closingTime;
	}

	public final OptionalTime getLatestStartTime() {
		return this.latestStartTime;
	}

	public final OptionalTime getEarliestEndTime() {
		return this.earliestEndTime;
	}

	public final boolean isScoreAtAll() {
		return scoreAtAll;
	}

}
