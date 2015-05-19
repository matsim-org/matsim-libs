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
package playground.thibautd.socnetsim.usage.replanning;

import java.util.Collection;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class GroupReplanningConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "groupStrategy";

	public static class StrategyParameterSet extends ReflectiveConfigGroup {
		public static final String SET_NAME = "strategy";

		private String strategyName = null;
		private double weight = 0;
		private boolean isInnovative = true;
		private String subpopulation = null;

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

		@StringGetter( "subpopulation" )
		public String getSubpopulation() {
			return subpopulation;
		}

		@StringSetter( "subpopulation" )
		public void setSubpopulation(String subpopulation) {
			this.subpopulation = subpopulation;
		}
	}

	private boolean checkConsistency = false;
	private int graphWriteInterval = 25;
	private int disableInnovationAfterIter = -1;
	private boolean considerVehicleIncompatibilities = false;
	private double initialTimeMutationTemperature = 24;
	private boolean useLimitedVehicles = true;
	private String locationChoiceActivityType = "leisure";
	private String weightAttribute = null;
	private String selectorForRemoval = "MinimumSum";
	private String selectorForModification = "WeakRandomSelection";
	private double internalizationRatio = 0;

	private int maxPlansPerComposition = 5;
	private int maxPlansPerAgent = 50;

	public GroupReplanningConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		if ( type.equals( StrategyParameterSet.SET_NAME ) ) {
			return new StrategyParameterSet();
		}
		throw new IllegalArgumentException( type );
	}

	public void addStrategyParameterSet(final StrategyParameterSet set) {
		addParameterSet( set );
	}

	// XXX not soooo safe, but should be OK (normally, no other type  can be added for the type)
	@SuppressWarnings("unchecked")
	public Collection<StrategyParameterSet> getStrategyParameterSets() {
		final Collection<? extends ConfigGroup> sets = getParameterSets( StrategyParameterSet.SET_NAME );
		return (Collection<StrategyParameterSet>) sets;
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

	@StringGetter( "selectorForRemoval" )
	public String getSelectorForRemoval() {
		return this.selectorForRemoval;
	}

	@StringSetter( "selectorForRemoval" )
	public void setSelectorForRemoval(final String selectorForRemoval) {
		this.selectorForRemoval = selectorForRemoval;
	}

	@StringGetter( "weightAttributeName" )
	public String getWeightAttributeName() {
		return this.weightAttribute;
	}

	@StringSetter( "weightAttributeName" )
	public void setWeightAttributeName(String weightAttribute) {
		this.weightAttribute = weightAttribute;
	}

	@StringGetter( "selectorForModification" )
	public String getSelectorForModification() {
		return this.selectorForModification;
	}

	@StringSetter( "selectorForModification" )
	public void setSelectorForModification(String selectorForModification) {
		this.selectorForModification = selectorForModification;
	}

	@StringGetter( "internalizationRatio" )
	public double getInternalizationRatio() {
		return this.internalizationRatio;
	}

	@StringSetter( "internalizationRatio" )
	public void setInternalizationRatio(double internalizationRatio) {
		this.internalizationRatio = internalizationRatio;
	}

	@StringGetter( "maxPlansPerAgent" )
	public int getMaxPlansPerAgent() {
		return this.maxPlansPerAgent;
	}

	@StringSetter( "maxPlansPerAgent" )
	public void setMaxPlansPerAgent(int maxPlansPerAgent) {
		if ( maxPlansPerAgent < 1 ) throw new IllegalArgumentException( maxPlansPerAgent+" too small" );
		this.maxPlansPerAgent = maxPlansPerAgent;
	}

	@StringGetter( "maxPlansPerComposition" )
	public int getMaxPlansPerComposition() {
		return this.maxPlansPerComposition;
	}

	@StringSetter( "maxPlansPerComposition" )
	public void setMaxPlansPerComposition(int maxPlansPerComposition) {
		if ( maxPlansPerComposition < 1 ) throw new IllegalArgumentException( maxPlansPerComposition+" too small" );
		this.maxPlansPerComposition = maxPlansPerComposition;
	}


}

