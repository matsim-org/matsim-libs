/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReplanningConfigGroup.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.Collection;

import org.matsim.core.config.experimental.ReflectiveModule.StringGetter;
import org.matsim.core.config.experimental.ReflectiveModule.StringSetter; 
import org.matsim.core.config.Module;
import playground.thibautd.config.ReflectiveNonFlatModule;

/**
 * @author thibautd
 */
public class GroupReplanningConfigGroup extends ReflectiveNonFlatModule {
	public static final String GROUP_NAME = "groupStrategy";

	public static class StrategyParameterSet extends ReflectiveNonFlatModule {
		public static final String SET_NAME = "strategy";

		private String strategyName = null;
		private double weight = 0;
		private boolean isInnovative = true;

		public StrategyParameterSet() {
			super( SET_NAME );
		}

		@StringGetter( "strategyName" )
		public String getStrategyName() {
			return this.strategyName;
		}

		@StringSetter( "strategyName" )
		public void setStrategyName(String strategyName) {
			this.strategyName = strategyName;
		}

		@StringGetter( "weight" )
		public double getWeight() {
			return this.weight;
		}

		@StringSetter( "weight" )
		public void setWeight(double weight) {
			this.weight = weight;
		}

		@StringGetter( "isInnovative" )
		public boolean isInnovative() {
			return this.isInnovative;
		}

		@StringSetter( "isInnovative" )
		public void setIsInnovative(boolean isInnovative) {
			this.isInnovative = isInnovative;
		}
	}

	private Synchro doSynchronize = Synchro.dynamic;
	private boolean checkConsistency = false;
	private int graphWriteInterval = 25;
	private int disableInnovationAfterIter = -1;
	private boolean considerVehicleIncompatibilities = true;
	private double initialTimeMutationTemperature = 24;
	private boolean useLimitedVehicles = true;
	private String locationChoiceActivityType = "leisure";
	private String weightAttribute = null;

	public static enum GroupScoringType {
		sum,
		weightedSum,
		min,
		minLoss,
		whoIsTheBoss;
	}
	private GroupScoringType groupScoringType = GroupScoringType.sum;

	public static enum Synchro {
		dynamic, none, all;
	}

	public GroupReplanningConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	protected Module createParameterSet(final String type) {
		if ( type.equals( StrategyParameterSet.SET_NAME ) ) {
			return new StrategyParameterSet();
		}
		throw new IllegalArgumentException( type );
	}

	// XXX not soooo safe, but should be OK (normally, no other type  can be added for the type)
	@SuppressWarnings("unchecked")
	public Collection<StrategyParameterSet> getStrategyParameterSets() {
		final Collection<? extends Module> sets = getParameterSets( StrategyParameterSet.SET_NAME );
		return (Collection<StrategyParameterSet>) sets;
	}

	@StringGetter( "doSynchronize" )
	public Synchro getSynchronize() {
		return doSynchronize;
	}

	@StringSetter( "doSynchronize" )
	public void setSynchronize(final String v) {
		this.doSynchronize = Synchro.valueOf( v.toLowerCase() );
	}

	@StringGetter( "checkConsistency" )
	public boolean getCheckConsistency() {
		return checkConsistency;
	}

	@StringSetter( "checkConsistency" )
	public void setCheckConsistency(final boolean b) {
		this.checkConsistency = b;
	}

	@StringSetter( "graphWriteInterval" )
	public void setGraphWriteInterval(final int i) {
		this.graphWriteInterval = i;
	}

	@StringGetter( "graphWriteInterval" )
	public int getGraphWriteInterval() {
		return this.graphWriteInterval;
	}

	@StringGetter( "disableInnovationAfterIteration" )
	public int getDisableInnovationAfterIter() {
		return this.disableInnovationAfterIter;
	}

	@StringSetter( "disableInnovationAfterIteration" )
	public void setDisableInnovationAfterIteration(int disableInnovationAfterIter) {
		this.disableInnovationAfterIter = disableInnovationAfterIter;
	}

	@StringGetter( "considerVehicleIncompatibilities" )
	public boolean getConsiderVehicleIncompatibilities() {
		return this.considerVehicleIncompatibilities;
	}

	@StringSetter( "considerVehicleIncompatibilities" )
	public void setConsiderVehicleIncompatibilities(
			final boolean considerVehicleIncompatibilities) {
		this.considerVehicleIncompatibilities = considerVehicleIncompatibilities;
	}

	@StringGetter( "initialTimeMutationTemperature" )
	public double getInitialTimeMutationTemperature() {
		return this.initialTimeMutationTemperature;
	}

	@StringSetter( "initialTimeMutationTemperature" )
	public void setInitialTimeMutationTemperature(
			final double initialTimeMutationTemperature) {
		this.initialTimeMutationTemperature = initialTimeMutationTemperature;
	}

	@StringGetter( "useLimitedVehicles" )
	public boolean getUseLimitedVehicles() {
		return this.useLimitedVehicles;
	}

	@StringSetter( "useLimitedVehicles" )
	public void setUseLimitedVehicles( final boolean useLimitedVehicles ) {
		this.useLimitedVehicles = useLimitedVehicles;
	}

	@StringGetter( "locationChoiceActivity" )
	public String getLocationChoiceActivityType() {
		return this.locationChoiceActivityType;
	}

	@StringSetter( "locationChoiceActivity" )
	public void setLocationChoiceActivityType(String locationChoiceActivityType) {
		this.locationChoiceActivityType = locationChoiceActivityType;
	}

	@StringGetter( "weightAttributeName" )
	public String getWeightAttributeName() {
		return this.weightAttribute;
	}

	@StringSetter( "weightAttributeName" )
	public void setWeightAttributeName(String weightAttribute) {
		this.weightAttribute = weightAttribute;
	}

	@StringGetter( "groupScoringType" )
	public GroupScoringType getGroupScoringType() {
		return this.groupScoringType;
	}

	@StringSetter( "groupScoringType" )
	private void setgetGroupScoringType(final String groupScoringType) {
		this.setgetGroupScoringType( GroupScoringType.valueOf( groupScoringType ) );
	}

	public void setgetGroupScoringType(final GroupScoringType groupScoringType) {
		this.groupScoringType = groupScoringType;
	}
}

