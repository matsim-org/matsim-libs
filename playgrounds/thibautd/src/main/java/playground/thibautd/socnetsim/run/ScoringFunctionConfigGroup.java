/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.thibautd.socnetsim.run;

import org.matsim.core.config.experimental.ReflectiveModule;

import playground.thibautd.scoring.BeingTogetherScoring;

class ScoringFunctionConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "scoringFunction";
	private boolean useKtiScoring = false;
	private double marginalUtilityOfBeingTogether_h = 0;
	private double marginalUtilityOfBeingDriver_h = -3;
	private double marginalUtilityOfBeingPassenger_h = -3;

	static enum TogetherScoringForm {
		linear,
		logarithmic;
	}
	private TogetherScoringForm togetherScoringForm = TogetherScoringForm.linear;
	
	static enum TogetherScoringType {
		allModesAndActs,
		leisureOnly;
	}
	private TogetherScoringType togetherScoringType = TogetherScoringType.allModesAndActs;

	public ScoringFunctionConfigGroup() {
		super( GROUP_NAME );
	}

	@StringSetter( "useKtiScoring" )
	public void setUseKtiScoring(final boolean v) {
		this.useKtiScoring = v;
	}

	@StringGetter( "useKtiScoring" )
	public boolean isUseKtiScoring() {
		return useKtiScoring;
	}

	@StringGetter( "marginalUtilityOfBeingTogether_h" )
	public double getMarginalUtilityOfBeingTogether_h() {
		return this.marginalUtilityOfBeingTogether_h;
	}

	public double getMarginalUtilityOfBeingTogether_s() {
		return this.marginalUtilityOfBeingTogether_h / 3600;
	}

	@StringSetter( "marginalUtilityOfBeingTogether_h" )
	public void setMarginalUtilityOfBeingTogether_h(
			double marginalUtilityOfBeingTogether_h) {
		this.marginalUtilityOfBeingTogether_h = marginalUtilityOfBeingTogether_h;
	}

	@StringGetter( "togetherScoringType" )
	public TogetherScoringType getTogetherScoringType() {
		return this.togetherScoringType;
	}

	@StringSetter( "togetherScoringType" )
	public void setTogetherScoringType(final TogetherScoringType togetherScoringType) {
		this.togetherScoringType = togetherScoringType;
	}

	@StringGetter( "marginalUtilityOfBeingDriver_h" )
	public double getMarginalUtilityOfBeingDriver_h() {
		return this.marginalUtilityOfBeingDriver_h;
	}

	public double getMarginalUtilityOfBeingDriver_s() {
		return this.marginalUtilityOfBeingDriver_h / 3600d;
	}

	@StringSetter( "marginalUtilityOfBeingDriver_h" )
	public void setMarginalUtilityOfBeingDriver_h(
			double marginalUtilityOfBeingDriver_h) {
		this.marginalUtilityOfBeingDriver_h = marginalUtilityOfBeingDriver_h;
	}

	@StringGetter( "marginalUtilityOfBeingPassenger_h" )
	public double getMarginalUtilityOfBeingPassenger_h() {
		return this.marginalUtilityOfBeingPassenger_h;
	}

	public double getMarginalUtilityOfBeingPassenger_s() {
		return this.marginalUtilityOfBeingPassenger_h / 3600d;
	}

	@StringSetter( "marginalUtilityOfBeingPassenger_h" )
	public void setMarginalUtilityOfBeingPassenger_h(
			double marginalUtilityOfBeingPassenger_h) {
		this.marginalUtilityOfBeingPassenger_h = marginalUtilityOfBeingPassenger_h;
	}

	@StringGetter( "togetherScoringForm" )
	public TogetherScoringForm getTogetherScoringForm() {
		return this.togetherScoringForm;
	}

	@StringSetter( "togetherScoringForm" )
	public void setTogetherScoringForm(final TogetherScoringForm togetherScoringForm) {
		this.togetherScoringForm = togetherScoringForm;
	}

	// I do not like so much this kind of "intelligent" method in Modules...
	public BeingTogetherScoring.Filter getActTypeFilterForJointScoring() {
		switch ( togetherScoringType ) {
			case allModesAndActs:
				return new BeingTogetherScoring.AcceptAllFilter();
			case leisureOnly:
				return new BeingTogetherScoring.AcceptAllInListFilter( "leisure" );
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}

	public BeingTogetherScoring.Filter getModeFilterForJointScoring() {
		switch ( togetherScoringType ) {
			case allModesAndActs:
				return new BeingTogetherScoring.AcceptAllFilter();
			case leisureOnly:
				return new BeingTogetherScoring.RejectAllFilter();
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}
}
