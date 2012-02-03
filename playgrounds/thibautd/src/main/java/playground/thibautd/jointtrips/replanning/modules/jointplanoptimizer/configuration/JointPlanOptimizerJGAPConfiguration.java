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
package playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.configuration;

import java.lang.Exception;

import org.apache.log4j.Logger;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.GeneticOperator;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.TournamentSelector;
import org.jgap.InvalidConfigurationException;
import org.jgap.audit.IEvolutionMonitor;
import org.jgap.event.EventManager;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.StockRandomGenerator;

import playground.thibautd.jointtrips.config.JointReplanningConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerDecoder;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizerJGAPBreeder;
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
			final JointPlanOptimizerSemanticsBuilder semanticsBuilder,
			final JointPlanOptimizerProcessBuilder processBuilder,
			final JointReplanningConfigGroup configGroup,
			final String outputPath,
			final long randomSeed) {
		super(null);
		Configuration.reset();

		int nMembers = plan.getIndividualPlans().size();

		try {
			// /////////////////////////////////////////////////////////////////
			// 1 - initialise non-configurable parts
			// /////////////////////////////////////////////////////////////////
			this.setBreeder(new JointPlanOptimizerJGAPBreeder());
			this.setEventManager(new EventManager());
			this.setChromosomePool(new ChromosomePool());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed(randomSeed);

			// /////////////////////////////////////////////////////////////////
			// 2 - initialise configurable semantics
			// /////////////////////////////////////////////////////////////////
			// semantics: builder calls, in the order defined in the javadoc of the interface
			this.setSampleChromosome( semanticsBuilder.createSampleChromosome( plan , this ) );
			this.constraints = semanticsBuilder.createConstraintsManager( plan , this );
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.fitnessFunction = semanticsBuilder.createFitnessFunction( plan , this );
			this.setFitnessFunction( fitnessFunction );

			// /////////////////////////////////////////////////////////////////
			// 3 - initialise configurable process
			// /////////////////////////////////////////////////////////////////
			this.setPopulationSize( processBuilder.getPopulationSize( plan , this ) );
			this.monitor = processBuilder.createEvolutionMonitor( plan , this );
			this.addNaturalSelector(
					processBuilder.createNaturalSelector( plan , this ),
					false );

			// not configurable, but must go here: between population size and other
			// operators.
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


			for (GeneticOperator op : processBuilder.createGeneticOperators( plan , this )) {
				this.addGeneticOperator( op );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( "exception thrown at configuration init" , e);
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

