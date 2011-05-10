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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.audit.IEvolutionMonitor;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.event.EventManager;
import org.jgap.Gene;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.StockRandomGenerator;
import org.jgap.impl.ThresholdSelector;
import org.jgap.impl.TournamentSelector;
import org.jgap.impl.WeightedRouletteSelector;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.CrossOverRateCalculator;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPCrossOver;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPEnhancedSpx;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPInPlaceMutation;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPMutation;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerJGAPSpx;
import playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators.JointPlanOptimizerPopulationAnalysisOperator;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.DefaultChromosomeDistanceComparator;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.RestrictedTournamentSelector;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.TabuAndEvolutionMonitor;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.TabuBestFitnessSelector;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.TabuMonitor;
import playground.thibautd.jointtripsoptimizer.replanning.modules.selectors.TabuRestrictedTournamentSelector;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * JGAP configuration object for the joint replanning algorithm.
 *
 * @todo improve substantially:
 *   - correct the number of "toggle" chromosomes
 *   - include a "engagement/desengagement" functionality
 *   - make possible the joint trips where several passengers have different O/D
 * @author thibautd
 */
public class JointPlanOptimizerJGAPConfiguration extends Configuration {

	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPConfiguration.class);

	private static final Double DAY_DUR = 24*3600d;

	private static final long serialVersionUID = 1L;

	//TODO: make final
	private int numEpisodes;
	private int numToggleGenes;
	private int numModeGenes;
	/**
	 * stores the number of duration genes relatives to each individual plan in
	 * the joint plan.
	 */
	private final List<Integer> nDurationGenes = new ArrayList<Integer>();
	private final JointPlanOptimizerFitnessFunction fitnessFunction;
	private final TabuAndEvolutionMonitor monitor;

	private final boolean optimizeToggle;

	public JointPlanOptimizerJGAPConfiguration(
			JointPlan plan,
			JointReplanningConfigGroup configGroup,
			ScoringFunctionFactory scoringFunctionFactory,
			JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			String outputPath,
			long randomSeed
			) {
		super(null);
		Configuration.reset();

		this.optimizeToggle = configGroup.getOptimizeToggle();

		// get info on the plan structure
		this.countEpisodes(plan);

		this.monitor = new TabuAndEvolutionMonitor(this, configGroup);

		try {
			// default JGAP objects initializations
			this.setBreeder(new JointPlanOptimizerJGAPBreeder());
			this.setEventManager(new EventManager());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed(randomSeed);

			// this selector ensures that a certain proportion of the genes (named
			// "Threshold") is selected using a "best first" strategy.
			// The rest of the new generation is selected randomly, with replacement.
			//ThresholdSelector selector =
			//	new ThresholdSelector(this, configGroup.getSelectionThreshold());
			//	this reimplementation allows to choose whether to use replacement
			//	or not (TODO: import from config group)
			//JointPlanOptimizerJGAPThresholdSelector selector = 
			//	new JointPlanOptimizerJGAPThresholdSelector(
			//			this, 
			//			configGroup.getSelectionThreshold());
			//selector.setDoubletteChromosomesAllowed(configGroup.getAllowDoublettes());
			//TournamentSelector selector = new TournamentSelector(this, 2, 0.6d);
			RestrictedTournamentSelector selector = 
				new TabuRestrictedTournamentSelector(
						this,
						new DefaultChromosomeDistanceComparator(
							configGroup.getDiscreteDistanceScale()),
						this.monitor); 
			//TabuBestFitnessSelector selector =
			//	new TabuBestFitnessSelector(this, configGroup);
			this.addNaturalSelector(selector, false);

			this.setPreservFittestIndividual(false);

			// Chromosome: construction
			Gene[] sampleGenes =
				new Gene[this.numToggleGenes + this.numEpisodes +this.numModeGenes];
			for (int i=0; i < this.numToggleGenes; i++) {
				sampleGenes[i] = new BooleanGene(this);
			}
			for (int i=this.numToggleGenes;
					i < this.numToggleGenes + this.numEpisodes; i++) {
				//sampleGenes[i] = new IntegerGene(this, 0, numTimeIntervals);
				sampleGenes[i] = new DoubleGene(this, 0, DAY_DUR);
			}
			for (int i=this.numToggleGenes + this.numEpisodes;
					i < this.numToggleGenes + this.numEpisodes + this.numModeGenes;
					i++) {
				sampleGenes[i] =
					new JointPlanOptimizerJGAPModeGene(this, configGroup.getAvailableModes());
			}

			log.debug("duration genes: "+this.numEpisodes+
					", toggle genes: "+this.numToggleGenes+
					", mode genes: "+this.numModeGenes);
			this.setSampleChromosome(new JointPlanOptimizerJGAPChromosome(this, sampleGenes));

			// population size: the SPX cross-over requires at least one chromosome
			// per double dimension.
			int popSize = Math.max(configGroup.getPopulationSize(), this.numEpisodes + 1);
			log.debug("population size set to "+popSize);
			this.setPopulationSize(popSize);

			this.fitnessFunction = new JointPlanOptimizerFitnessFunction(
						plan,
						configGroup,
						legTravelTimeEstimatorFactory,
						routingAlgorithm,
						network,
						this.numToggleGenes,
						this.numEpisodes,
						scoringFunctionFactory);
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.setFitnessFunction(this.fitnessFunction);

			// discarded chromosomes are "recycled" rather than suppressed.
			this.setChromosomePool(new ChromosomePool());

			// genetic operators definitions
			// TODO: use SPX if more individuals than double genes,
			// use GENOCOP COs otherwise, loging a warning.
			if (configGroup.getPlotFitness()) {
				this.addGeneticOperator( new JointPlanOptimizerPopulationAnalysisOperator(
							this,
							configGroup.getMaxIterations(),
							outputPath));
			}
			if (configGroup.getIsDynamicCO()) {
				this.addGeneticOperator( new JointPlanOptimizerJGAPCrossOver(
							this,
							configGroup,
							new CrossOverRateCalculator(configGroup, outputPath),
							this.numToggleGenes,
							this.numEpisodes,
							this.numModeGenes,
							this.nDurationGenes) );
			}
			else {
				this.addGeneticOperator( new JointPlanOptimizerJGAPCrossOver(
							this,
							configGroup,
							this.numToggleGenes,
							this.numEpisodes,
							this.numModeGenes,
							this.nDurationGenes) );
			}
			//this.addGeneticOperator( new JointPlanOptimizerJGAPSpx(
			//			this,
			//			configGroup,
			//			this.numToggleGenes,
			//			this.numEpisodes,
			//			this.nDurationGenes) );
			this.addGeneticOperator( new JointPlanOptimizerJGAPEnhancedSpx(
						this,
						configGroup,
						this.numToggleGenes,
						this.numEpisodes,
						this.nDurationGenes) );
			this.addGeneticOperator( new JointPlanOptimizerJGAPMutation(
						this,
						configGroup,
						this.numToggleGenes + this.numEpisodes + this.numModeGenes,
						this.nDurationGenes));
			//this.addGeneticOperator( new JointPlanOptimizerJGAPInPlaceMutation(
			//			this,
			//			configGroup,
			//			this.numToggleGenes + this.numEpisodes + this.numModeGenes,
			//			this.nDurationGenes));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	 }

	/**
	 * Sets the private variables numEpisodes and numToggleGenes.
	 * an episode corresponds to an activity and its eventual access trip.
	 * a joint episode is an episode which involves a joint trip.
	 */
	private void countEpisodes(JointPlan plan) {
		PlanAnalyzeSubtours analyseSubtours = new PlanAnalyzeSubtours();
		Id[] ids = new Id[1];
		ids = plan.getClique().getMembers().keySet().toArray(ids);
		//List<JointLeg> alreadyExamined = new ArrayList<JointLeg>();
		Plan currentPlan;
		int currentNDurationGenes;
		boolean sharedRideExamined = false;

		this.numEpisodes = 0;
		this.numToggleGenes = 0;
		this.numModeGenes = 0;
		this.nDurationGenes.clear();

		for (Id id : ids) {
			currentPlan = plan.getIndividualPlan(id);
			currentNDurationGenes = 0;
			//TODO: use indices (and suppress the booleans)
			for (PlanElement pe : currentPlan.getPlanElements()) {
				// count activities for which duration is optimized
				if ((pe instanceof JointActivity)&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP))&&
						(!((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF)) ) {
					currentNDurationGenes++;

					if (sharedRideExamined) {
						//reset the marker
						sharedRideExamined = false;
					}
				} else if ((this.optimizeToggle)&&
						(pe instanceof JointLeg)&&
						(((JointLeg) pe).getJoint())&&
						//(!alreadyExamined.contains(pe))
						(((JointLeg) pe).getMode().equals(JointActingTypes.PASSENGER))&&
						(!sharedRideExamined)
						) {
					// we are on the first shared ride of a passenger ride
					this.numToggleGenes++;
					sharedRideExamined = true;

					//alreadyExamined.addAll(
					//		((JointLeg) pe).getLinkedElements().values());
				}
			}
			//do not count last activity
			currentNDurationGenes--;
			this.numEpisodes += currentNDurationGenes;
			this.nDurationGenes.add(currentNDurationGenes);

			//finally, count subtours
			analyseSubtours.run(currentPlan);
			this.numModeGenes += analyseSubtours.getNumSubtours();
		 }
	 }

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fitnessFunction.getDecoder();
	}

	public int getNumEpisodes() {
		return this.numEpisodes;
	}

	public int getNumJointEpisodes() {
		return this.numToggleGenes;
	}

	public int getNumModeGenes() {
		return this.numModeGenes;
	}

	public List<Integer> getNDurationGenesPerIndiv() {
		return this.nDurationGenes;
	}

	/**
	 * to avoid multiplying the places where the day duration is defined.
	 * Not very elegant, should be moved somewhere else, for example in the config
	 * group.
	 */
	public double getDayDuration() {
		return DAY_DUR;
	}

	public TabuMonitor getTabuMonitor() {
		return this.monitor;
	}

	public IEvolutionMonitor getEvolutionMonitor() {
		return this.monitor;
	}
}

