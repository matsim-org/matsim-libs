/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
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

package org.matsim.core.config.groups;

import java.util.Collection;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class TimeAllocationMutatorConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "TimeAllocationMutator";
		
	public TimeAllocationMutatorConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MUTATION_RANGE, "Default:1800.0; Defines how many seconds a time mutation can maximally shift a time.");
		comments.put(MUTATION_AFFECTS_DURATION, "Default:true; Defines whether time mutation changes an activity's duration.");
		comments.put(USE_INDIVIDUAL_SETTINGS_FOR_SUBPOPULATIONS, "false; Use individual settings for each subpopulation. "
				+ "If enabled but no settings are found, regular settings are uses as fallback.");
		return comments;
	}
	
	// ---

	private static final String MUTATION_RANGE = "mutationRange";
	private double mutationRange = 1800.0;
	@StringGetter(MUTATION_RANGE)
	public double getMutationRange() {
		return this.mutationRange;
	}
	@StringSetter(MUTATION_RANGE)
	public void setMutationRange(final double val) {
		this.mutationRange = val;
	}
	
	// ---
	
	private static final String MUTATION_AFFECTS_DURATION = "mutationAffectsDuration";
	private boolean affectingDuration = true;
	@StringGetter(MUTATION_AFFECTS_DURATION)
	public boolean isAffectingDuration() {
		return this.affectingDuration;
	}
	@StringSetter(MUTATION_AFFECTS_DURATION)
	public void setAffectingDuration(boolean affectingDuration) {
		this.affectingDuration = affectingDuration;
	}
	
	// ---
	
	private static final String USE_INDIVIDUAL_SETTINGS_FOR_SUBPOPULATIONS = "useIndividualSettingsForSubpopulations";
	private boolean useIndividualSettingsForSubpopulations = false;
	@StringGetter(USE_INDIVIDUAL_SETTINGS_FOR_SUBPOPULATIONS)
	public boolean isUseIndividualSettingsForSubpopulations() {
		return this.useIndividualSettingsForSubpopulations;
	}
	@StringSetter(USE_INDIVIDUAL_SETTINGS_FOR_SUBPOPULATIONS)
	public void setUseIndividualSettingsForSubpopulations(boolean useIndividualSettingsForSubpopulations) {
		this.useIndividualSettingsForSubpopulations = useIndividualSettingsForSubpopulations;
	}
	
	// ---
	
	public TimeAllocationMutatorSubpopulationSettings getTimeAllocationMutatorSubpopulationSettings(String subpopulation) {
		
		if (subpopulation == null) return null;
		
		Collection<? extends ConfigGroup> configGroups = this.getParameterSets(TimeAllocationMutatorSubpopulationSettings.SET_NAME);
		for (ConfigGroup group : configGroups) {
			if (group instanceof TimeAllocationMutatorSubpopulationSettings) {
				TimeAllocationMutatorSubpopulationSettings subpopulationSettings = (TimeAllocationMutatorSubpopulationSettings) group;
				if (subpopulation.equals(subpopulationSettings.subpopulation)) return subpopulationSettings;
			}
		}
		
		return null;
	}
	
	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch (type) {
			case TimeAllocationMutatorSubpopulationSettings.SET_NAME:
				return new TimeAllocationMutatorSubpopulationSettings();
			default:
				throw new IllegalArgumentException("unknown set type '" + type + "'");
		}
	}
	
	@Override
	public void addParameterSet(final ConfigGroup set) {
		switch (set.getName()) {
			case TimeAllocationMutatorSubpopulationSettings.SET_NAME:
				super.addParameterSet(set);
				break;
			default:
				throw new IllegalArgumentException( set.getName() );
		}
	}
	
	public static class TimeAllocationMutatorSubpopulationSettings extends ReflectiveConfigGroup {
		
		public static final String SET_NAME = "subpopulationSettings";
		private static final String MUTATION_RANGE = "mutationRange";
		private static final String MUTATION_AFFECTS_DURATION = "mutationAffectsDuration";
		private static final String SUBPOPULATION = "subpopulation";
		
		private double mutationRange = 1800.0;
		private boolean affectingDuration = true;
		private String subpopulation = null;
		
		public TimeAllocationMutatorSubpopulationSettings() {
			super(SET_NAME);
		}
		
		@Override
		public final Map<String, String> getComments() {
			Map<String,String> comments = super.getComments();
			comments.put(MUTATION_RANGE, "Default:1800.0; Defines how many seconds a time mutation can maximally shift a time.");
			comments.put(MUTATION_AFFECTS_DURATION, "Default:true; Defines whether time mutation changes an activity's duration.");
			comments.put(SUBPOPULATION, "Subpopulation to which the values from this parameter set are applied.");
			return comments;
		}

		@StringGetter(MUTATION_RANGE)
		public double getMutationRange() {
			return this.mutationRange;
		}
		
		@StringSetter(MUTATION_RANGE)
		public void setMutationRange(final double val) {
			this.mutationRange = val;
		}
		
		@StringGetter(MUTATION_AFFECTS_DURATION)
		public boolean isAffectingDuration() {
			return affectingDuration;
		}
		
		@StringSetter(MUTATION_AFFECTS_DURATION)
		public void setAffectingDuration(boolean affectingDuration) {
			this.affectingDuration = affectingDuration;
		}

		@StringSetter(SUBPOPULATION)
		public void setSubpopulation(final String subpopulation) {
			this.subpopulation = subpopulation;
		}

		@StringGetter(SUBPOPULATION)
		public String getSubpopulation() {
			return this.subpopulation;
		}
	}
}