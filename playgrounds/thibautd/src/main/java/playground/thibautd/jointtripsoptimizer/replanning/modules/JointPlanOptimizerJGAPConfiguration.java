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

import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.event.EventManager;
import org.jgap.Gene;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.GABreeder;
import org.jgap.impl.StockRandomGenerator;
import org.jgap.impl.WeightedRouletteSelector;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * JGAP configuration object for the joint replanning algorithm.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPConfiguration extends Configuration {

	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPConfiguration.class);

	private static final Double DAY_DUR = 24*3600d;

	private static final long serialVersionUID = 1L;

	//TODO: make final
	private int numEpisodes;
	private int numJointEpisodes;
	private JointPlanOptimizerFitnessFunction fitnessFunction;

	public JointPlanOptimizerJGAPConfiguration(
			JointPlan plan,
			JointReplanningConfigGroup configGroup,
			ScoringFunctionFactory scoringFunctionFactory,
			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			long randomSeed
			) {
		super(null);
		Configuration.reset();

		// get info on the plan structure
		this.countEpisodes(plan);

		try {
			// default JGAP objects initializations
			this.setBreeder(new JointPlanOptimizerJGAPBreeder());
			this.setEventManager(new EventManager());

			// seed the default JGAP pseudo-random generator with a matsim random
			// number, so that the simulations are reproducible.
			this.setRandomGenerator(new StockRandomGenerator());
			((StockRandomGenerator) this.getRandomGenerator()).setSeed(randomSeed);

			BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(this);
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			this.addNaturalSelector(bestChromsSelector, false);
			//this.addNaturalSelector(new WeightedRouletteSelector(), false);

			// elitism: unnecessary with BestChromosomesSelector
			// this.setPreservFittestIndividual(true);

			// Chromosome: construction
			Gene[] sampleGenes = new Gene[this.numJointEpisodes + this.numEpisodes];
			for (int i=0; i < this.numJointEpisodes; i++) {
				sampleGenes[i] = new BooleanGene(this);
			}
			for (int i=this.numJointEpisodes;
					i < this.numJointEpisodes + this.numEpisodes; i++) {
				//sampleGenes[i] = new IntegerGene(this, 0, numTimeIntervals);
				sampleGenes[i] = new DoubleGene(this, 0, DAY_DUR);
			}

			log.debug("episodes: "+this.numEpisodes+", joint episodes: "+this.numJointEpisodes);
			this.setSampleChromosome(new JointPlanOptimizerJGAPChromosome(this, sampleGenes));

			// population size
			log.debug("population size set to "+configGroup.getPopulationSize());
			this.setPopulationSize(configGroup.getPopulationSize());

			this.fitnessFunction = new JointPlanOptimizerFitnessFunction(
						plan,
						legTravelTimeEstimatorFactory,
						routingAlgorithm,
						network,
						this.numJointEpisodes,
						this.numEpisodes,
						scoringFunctionFactory);
			this.setFitnessEvaluator(new DefaultFitnessEvaluator());
			this.setFitnessFunction(this.fitnessFunction);

			// discarded chromosomes are "recycled" rather than suppressed.
			// this.setChromosomePool(new ChromosomePool());

			// genetic operators definitions
			// reproduction operator unnecessary: a_candicateChromosomes is obtained
			// by calling a_population.getChromosomes() in Breeder.
			// a call to a_population.getChromosomes().equals(a_candidateChromosomes)
			// in the first genetic operator returns true.
			//this.addGeneticOperator( new ReproductionOperator() );
			this.addGeneticOperator( new JointPlanOptimizerJGAPCrossOver(
						this,
						configGroup,
						this.numJointEpisodes,
						this.numEpisodes) );
			this.addGeneticOperator( new JointPlanOptimizerJGAPMutation(
						this,
						configGroup,
						this.numJointEpisodes + this.numEpisodes));

		} catch (InvalidConfigurationException e) {
			//throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		}
	 }

	/**
	 * Sets the private variables numEpisodes and numJointEpisodes.
	 * an episode corresponds to an activity and its eventual access trip.
	 * a joint episode is an episode which involves a joint trip.
	 */
	@SuppressWarnings("unchecked")
		//TODO: FIXME correct counting of joint episodes
		//(current counting is good if only shared rides are marked as "joint")
	private void countEpisodes(JointPlan plan) {
		Id[] ids = new Id[1];
		ids = plan.getClique().getMembers().keySet().toArray(ids);
		List<JointLeg> alreadyExamined = new ArrayList<JointLeg>();
		List<JointLeg> linkedValues = null;

		 this.numEpisodes = 0;
		 this.numJointEpisodes = 0;

		 for (Id id : ids) {
			//log.debug("id: "+id);
			//log.debug("plan size: "+plan.getIndividualPlan(id).getPlanElements().size());
			for (PlanElement pe : plan.getIndividualPlan(id).getPlanElements()) {
				// count activities for which duration is optimized
				//log.debug(pe instanceof JointActivity ? "oui" : "non");
				if ((pe instanceof JointActivity)&&
						(((JointActivity) pe).getType() != JointActingTypes.PICK_UP)&&
						(((JointActivity) pe).getType() != JointActingTypes.DROP_OFF)) {
					this.numEpisodes++;
				} else if ((pe instanceof JointLeg)&&
						(((JointLeg) pe).getJoint())&&
						(!alreadyExamined.contains(pe))
						) {
					this.numJointEpisodes++;

					linkedValues = (List<JointLeg>) 
						((JointLeg) pe).getLinkedElements().values();

					alreadyExamined.addAll(linkedValues);
				}
			}
		 }
		 //do not count last activity
		 this.numEpisodes--;
	 }

	public JointPlanOptimizerDecoder getDecoder() {
		return this.fitnessFunction.getDecoder();
	}

	public int getNumEpisodes() {
		return this.numEpisodes;
	}

	public int getNumJointEpisodes() {
		return this.numJointEpisodes;
	}

	/**
	 * to avoid multiplying the places where the day duration is defined.
	 * Not very elegant, should be moved somewhere else, for example in the config
	 * group.
	 */
	public double getDayDuration() {
		return this.numJointEpisodes;
	}
}

