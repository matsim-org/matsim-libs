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
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.GABreeder;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;

import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.PlanElement;

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
	private static final Double CO_PROB = 0.6d;

	private static final long serialVersionUID = 1L;

	private int numEpisodes;
	private int numJointEpisodes;

	public JointPlanOptimizerJGAPConfiguration(
			JointPlan plan,
			JointReplanningConfigGroup configGroup
			) {
		super(null);
		Configuration.reset();

		// get info on the plan structure
		int numTimeIntervals = configGroup.getNumTimeIntervals();
		this.countEpisodes(plan);

		try {
			// default JGAP objects initializations
			this.setBreeder(new GABreeder());
			this.setEventManager(new EventManager());

			BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(this);
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			this.addNaturalSelector(bestChromsSelector, false);

			this.setPreservFittestIndividual(true);

			// Chromosome: construction
			Gene[] sampleGenes = new Gene[this.numJointEpisodes + this.numEpisodes];
			for (int i=0; i < this.numJointEpisodes; i++) {
				sampleGenes[i] = new BooleanGene(this);
			}
			for (int i=this.numJointEpisodes;
					i < this.numJointEpisodes + this.numEpisodes; i++) {
				sampleGenes[i] = new IntegerGene(this, 0, numTimeIntervals);
			}
			this.setSampleChromosome(new JointPlanOptimizerJGAPChromosome(this, sampleGenes));

			// population size
			this.setPopulationSize(configGroup.getPopulationSize());

			this.setFitnessEvaluator(new DefaultFitnessEvaluator());

			// genetic operators definitions
			this.setChromosomePool(new ChromosomePool());
			// TODO: include when implemented
			// this.addGeneticOperator(new JointPlanOptimizerJGAPCrossOver(this, CO_PROB));
			this.addGeneticOperator(new CrossoverOperator(this, CO_PROB));
			this.addGeneticOperator(new MutationOperator(this, this.getPopulationSize()));

		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	 }

	/**
	 * Sets the private variables numEpisodes and numJointEpisodes.
	 * an episode corresponds to an activity and its eventual access trip.
	 * a joint episode is an episode which involves a joint trip.
	 */
	@SuppressWarnings("unchecked")
	private void countEpisodes(JointPlan plan) {
		Id[] ids = (Id[]) plan.getClique().getMembers().keySet().toArray();
		List<JointLeg> alreadyExamined = new ArrayList<JointLeg>();
		List<JointLeg> linkedValues = null;

		 this.numEpisodes = 0;
		 this.numJointEpisodes = 0;
		 
		 for(Id id : ids) {
			for(PlanElement pe : plan.getIndividualPlan(id).getPlanElements()) {
				if (pe instanceof JointActivity) {
					this.numEpisodes++;
				} else if (
						(((JointLeg) pe).getJoint())&&
						(!alreadyExamined.contains((JointLeg) pe))
						) {
					this.numJointEpisodes++;

					linkedValues = (List<JointLeg>) 
						((JointLeg) pe).getLinkedElements().values();

					alreadyExamined.addAll(linkedValues);
				}
			}
		 }
	 }
}

