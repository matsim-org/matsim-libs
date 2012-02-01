/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPConfiguration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.InvalidConfigurationException;
import org.jgap.audit.IEvolutionMonitor;
import org.jgap.event.EventManager;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.StockRandomGenerator;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.AbstractJointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.JointPlanOptimizerPopulationAnalysisOperator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.DefaultChromosomeDistanceComparator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.HammingChromosomeDistanceComparator;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.selectors.RestrictedTournamentSelector;

/**
 * JGAP {@link Configuration} object for the joint replanning algorithm.
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPConfiguration extends Configuration {

	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPConfiguration.class);

	public static final double DAY_DUR = 24*3600d;

	private static final long serialVersionUID = 1L;

	private final AbstractJointPlanOptimizerFitnessFunction fitnessFunction;
	private final IEvolutionMonitor monitor;
	private final JointPlanOptimizerPopulationAnalysisOperator populationAnalysis;
	private final ConstraintsManager constraints;

	public JointPlanOptimizerJGAPConfiguration(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final JointPlanOptimizerSemanticsBuilder semanticsBuilder,
			final String outputPath,
			final long randomSeed) {
		super(null);
		Configuration.reset();

		int nMembers = plan.getIndividualPlans().size();

		try {
			// default JGAP objects initializations
			this.setBreeder(new JointPlanOptimizerJGAPBreeder());
			this.setEventManager(new EventManager());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed(randomSeed);

			this.monitor =
				new JointPlanOptimizerJGAPEvolutionMonitor(this, configGroup, nMembers);

			// semantics: builder calls, in the order defined in the javadoc of the interface
			this.setSampleChromosome( semanticsBuilder.createSampleChromosome( plan , this ) );
			constraints = semanticsBuilder.createConstraintsManager( plan , this );
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.fitnessFunction = semanticsBuilder.createFitnessFunction( plan , this );
			this.setFitnessFunction( fitnessFunction );

			// population size: the SPX cross-over requires at least one chromosome
			// per double dimension.
			int popSize = Math.min(
					(int) Math.ceil(
						configGroup.getPopulationIntercept() +
						configGroup.getPopulationCoef() * getSampleChromosome().size()),
					configGroup.getMaxPopulationSize());
			this.setPopulationSize( Math.max(2, popSize) );

			// discarded chromosomes are "recycled" rather than suppressed.
			this.setChromosomePool(new ChromosomePool());

			// selector
			RestrictedTournamentSelector selector = 
				new RestrictedTournamentSelector(
						this,
						configGroup,
						(configGroup.getUseOnlyHammingDistanceInRTS() ?
							 new HammingChromosomeDistanceComparator() :
							 new DefaultChromosomeDistanceComparator(
								configGroup.getDiscreteDistanceScale())));
			this.addNaturalSelector(selector, false);

			// do not try to reintroduce fittest: RTS never eliminates it
			this.setPreservFittestIndividual(false);

			// genetic operators definitions
			if (configGroup.getPlotFitness()) {
				this.populationAnalysis = new JointPlanOptimizerPopulationAnalysisOperator(
							this,
							configGroup.getMaxIterations(),
							nMembers,
							outputPath);
				this.addGeneticOperator(this.populationAnalysis);
			}
			else {
				this.populationAnalysis = null;
			}
			this.addGeneticOperator( new JointPlanOptimizerJGAPCrossOver(
						this,
						configGroup,
						constraints));

			this.addGeneticOperator( new JointPlanOptimizerJGAPMutation(
						this,
						configGroup,
						constraints));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	 }

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fitnessFunction.getDecoder();
	}

	/**
	 * to avoid multiplying the places where the day duration is defined.
	 * Not very elegant, should be moved somewhere else, for example in the config
	 * group.
	 */
	public double getDayDuration() {
		return DAY_DUR;
	}

	public IEvolutionMonitor getEvolutionMonitor() {
		return this.monitor;
	}

	public ConstraintsManager getConstraintsManager() {
		return constraints;
	}

	/**
	 * To call at the end, performs all necessary ending procedure
	 * (as outputing fitness graphs if needed).
	 */
	public void finish() {
		if (this.populationAnalysis != null) {
			this.populationAnalysis.finish();
		}
	}
}

