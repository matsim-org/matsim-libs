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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import playground.thibautd.socnetsim.scoring.BeingTogetherScoring;

public class ScoringFunctionConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "scoringFunction";
	private boolean useKtiScoring = false;
	private double marginalUtilityOfBeingTogether_h = 0;
	private double marginalUtilityOfBeingDriver_h = -3;
	private double marginalUtilityOfBeingPassenger_h = -3;
	private double constantDriver = 0;
	private double constantPassenger = 0;
	private String activityTypeForContactInDesires = "leisure";
	private String internalizationNetworkFile = null;
	private boolean useLocationChoiceEpsilons = false;

	public static enum TogetherScoringForm {
		linear,
		logarithmic;
	}
	private TogetherScoringForm togetherScoringForm = TogetherScoringForm.linear;
	
	static enum TogetherScoringType {
		allModesAndActs,
		actsRestricted;
	}
	private TogetherScoringType togetherScoringType = TogetherScoringType.allModesAndActs;
	private Set<String> joinableActivityTypes = new HashSet<String>( Arrays.asList( "leisure" , "l" ) );

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

	@StringGetter( "useLocationChoiceEpsilons" )
	public boolean isUseLocationChoiceEpsilons() {
		return this.useLocationChoiceEpsilons;
	}

	@StringSetter( "useLocationChoiceEpsilons" )
	public void setUseLocationChoiceEpsilons(final boolean useLocationChoiceEpsilons) {
		this.useLocationChoiceEpsilons = useLocationChoiceEpsilons;
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
			case actsRestricted:
				return new BeingTogetherScoring.AcceptAllInListFilter( getJoinableActivityTypes() );
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}

	public BeingTogetherScoring.Filter getModeFilterForJointScoring() {
		switch ( togetherScoringType ) {
			case allModesAndActs:
				return new BeingTogetherScoring.AcceptAllFilter();
			case actsRestricted:
				return new BeingTogetherScoring.RejectAllFilter();
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}

	@StringGetter( "joinableActivityTypes" )
	private String getJoinableActivityTypesString() {
		return CollectionUtils.setToString( getJoinableActivityTypes() );
	}
	
	public Set<String> getJoinableActivityTypes() {
		return joinableActivityTypes;
	}

	@StringSetter( "joinableActivityTypes" )
	private void setJoinableActivityTypes(final String v) {
		setJoinableActivityTypes( CollectionUtils.stringToSet( v ) );
	}
	
	public void setJoinableActivityTypes(final Set<String> joinableActivityTypes) {
		this.joinableActivityTypes = joinableActivityTypes;
	}

	@StringGetter( "constantDriver" )
	public double getConstantDriver() {
		return this.constantDriver;
	}

	@StringSetter( "constantDriver" )
	public void setConstantDriver(double constantDriver) {
		this.constantDriver = constantDriver;
	}

	@StringGetter( "constantPassenger" )
	public double getConstantPassenger() {
		return this.constantPassenger;
	}

	@StringSetter( "constantPassenger" )
	public void setConstantPassenger(double constantPassenger) {
		this.constantPassenger = constantPassenger;
	}

	@StringGetter( "activityTypeForContactInDesires" )
	public String getActivityTypeForContactInDesires() {
		return activityTypeForContactInDesires;
	}

	@StringSetter( "activityTypeForContactInDesires" )
	public void setActivityTypeForContactInDesires(
			String activityTypeForContactInDesires) {
		this.activityTypeForContactInDesires = activityTypeForContactInDesires;
	}
	
	@StringGetter( "internalizationNetworkFile" )
	public String getInternalizationNetworkFile() {
		return this.internalizationNetworkFile;
	}

	@StringSetter( "internalizationNetworkFile" )
	public void setInternalizationNetworkFile(
			final String internalizationNetworkFile) {
		this.internalizationNetworkFile = internalizationNetworkFile;
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();
		
		map.put( "activityTypeForContactInDesires" ,
				"the type of the activity from which typical duration will be taken for the log form.\n"+
				"This can be a real or a \"dummy\" activity type." );
		
		return map;
	}
}
