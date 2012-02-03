/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerRTSProcessBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jgap.GeneticOperator;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelector;
import org.jgap.audit.IEvolutionMonitor;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPEvolutionMonitor;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerPopulationAnalysisOperator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.DefaultChromosomeDistanceComparator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.HammingChromosomeDistanceComparator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.RestrictedTournamentSelector;

/**
 *
 * @author thibautd
 */
public class JointPlanOptimizerRTSProcessBuilder implements JointPlanOptimizerProcessBuilder {
	private final JointReplanningConfigGroup configGroup;

	public JointPlanOptimizerRTSProcessBuilder(
			final JointReplanningConfigGroup configGroup) {
		this.configGroup = configGroup;
	}

	@Override
	public int getPopulationSize(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) {
		int popSize = Math.min(
				(int) Math.ceil(
					configGroup.getPopulationIntercept() +
					configGroup.getPopulationCoef() * configuration.getSampleChromosome().size()),
				configGroup.getMaxPopulationSize());
		return Math.max(2, popSize);
	}

	@Override
	public IEvolutionMonitor createEvolutionMonitor(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) {
		return new JointPlanOptimizerJGAPEvolutionMonitor(
				configuration,
				configGroup,
				plan.getIndividualPlans().size());
	}

	@Override
	public NaturalSelector createNaturalSelector(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) throws InvalidConfigurationException {
		return new RestrictedTournamentSelector(
					configuration,
					configGroup,
					(configGroup.getUseOnlyHammingDistanceInRTS() ?
						 new HammingChromosomeDistanceComparator() :
						 new DefaultChromosomeDistanceComparator(
							configGroup.getDiscreteDistanceScale())));
	}

	@Override
	public Collection<GeneticOperator> createGeneticOperators(
			final JointPlan plan,
			final JointPlanOptimizerJGAPConfiguration configuration) {
		List<GeneticOperator> operators = new ArrayList<GeneticOperator>( 2 );
		ConstraintsManager constraints = configuration.getConstraintsManager();
		
		operators.add( new JointPlanOptimizerJGAPCrossOver(
					configuration,
					configGroup,
					constraints));

		operators.add( new JointPlanOptimizerJGAPMutation(
					configuration,
					configGroup,
					constraints));

		return operators;
	}
}

