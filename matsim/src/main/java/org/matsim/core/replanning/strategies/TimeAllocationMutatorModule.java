/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
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

package org.matsim.core.replanning.strategies;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup.TimeAllocationMutatorSubpopulationSettings;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.PlanMutateTimeAllocationSimplified;
import org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

/**
 * Wraps the {@link org.matsim.core.population.algorithms.PlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link PlanStrategyModule} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.core.population.algorithms.PlanMutateTimeAllocation
 */
class TimeAllocationMutatorModule extends AbstractMultithreadedModule{

	private static final Logger log = Logger.getLogger( TimeAllocationMutatorModule.class );
	
	private final double mutationRange;
	private final boolean affectingDuration;
	private final String subpopulationAttribute;
	private final Map<String, Double> subpopulationMutationRanges;
	private final Map<String, Boolean> subpopulationAffectingDuration;
	private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange").
	 */
	@Deprecated
	TimeAllocationMutatorModule( Config config, Provider<TripRouter> tripRouterProvider, final double mutationRange, boolean affectingDuration ) {
		super(config.global());
		this.affectingDuration = affectingDuration;
		this.mutationRange = mutationRange;
		this.activityDurationInterpretation = (config.plans().getActivityDurationInterpretation());
		this.subpopulationAttribute = null;
		this.subpopulationMutationRanges = null;
		this.subpopulationAffectingDuration = null;
		log.warn("deprecated constructor was used - individual time allocation mutator settings for subpopulations is not supported!");
	}
	
	TimeAllocationMutatorModule( Provider<TripRouter> tripRouterProvider, PlansConfigGroup plansConfigGroup, TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup, GlobalConfigGroup globalConfigGroup ) {
		this(tripRouterProvider, plansConfigGroup, timeAllocationMutatorConfigGroup, globalConfigGroup, null);
	}
	
	TimeAllocationMutatorModule( Provider<TripRouter> tripRouterProvider, PlansConfigGroup plansConfigGroup, TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup, GlobalConfigGroup globalConfigGroup,
							final Population population ) {
		super(globalConfigGroup);
		this.activityDurationInterpretation = plansConfigGroup.getActivityDurationInterpretation();
		this.mutationRange = timeAllocationMutatorConfigGroup.getMutationRange();
		this.affectingDuration = timeAllocationMutatorConfigGroup.isAffectingDuration();
		
		// in case we have subpopulations and individual settings for them
		if (plansConfigGroup.getSubpopulationAttributeName() != null && timeAllocationMutatorConfigGroup.isUseIndividualSettingsForSubpopulations() && population != null) {
			this.subpopulationAttribute = plansConfigGroup.getSubpopulationAttributeName();
			this.subpopulationMutationRanges = new HashMap<>();
			this.subpopulationAffectingDuration = new HashMap<>();

			Collection<? extends ConfigGroup> settings = timeAllocationMutatorConfigGroup.getParameterSets(TimeAllocationMutatorSubpopulationSettings.SET_NAME);
			for (ConfigGroup group : settings) {
				TimeAllocationMutatorSubpopulationSettings subpopulationSettings = (TimeAllocationMutatorSubpopulationSettings) group;
				String subpopulation = subpopulationSettings.getSubpopulation();
				this.subpopulationMutationRanges.put(subpopulation, subpopulationSettings.getMutationRange());
				this.subpopulationAffectingDuration.put(subpopulation, subpopulationSettings.isAffectingDuration());
				log.info("Found individual time mutator settings for subpopulation: " + subpopulation);
			}
		} else {
			this.subpopulationAttribute = null;
			this.subpopulationMutationRanges = null;
			this.subpopulationAffectingDuration = null;
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm pmta;
		switch (this.activityDurationInterpretation) {
		case minOfDurationAndEndTime:
			pmta = new TripPlanMutateTimeAllocation(this.mutationRange, this.affectingDuration, MatsimRandom.getLocalInstance(),
					this.subpopulationAttribute, this.subpopulationMutationRanges, this.subpopulationAffectingDuration);
			break;
		default:
			pmta = new PlanMutateTimeAllocationSimplified(
					// TODO: is StageActivityHandling.ExcludeStageActivities right here?
					StageActivityHandling.ExcludeStageActivities, this.mutationRange, this.affectingDuration, MatsimRandom.getLocalInstance());
		}
		return pmta;
	}
}
