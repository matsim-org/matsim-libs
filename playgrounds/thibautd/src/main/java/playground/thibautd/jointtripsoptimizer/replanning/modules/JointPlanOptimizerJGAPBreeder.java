/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatGABreeder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;

import org.jgap.BulkFitnessFunction;
import org.jgap.Configuration;
import org.jgap.IChromosome;
import org.jgap.Population;
import org.jgap.event.GeneticEvent;
import org.jgap.impl.GABreeder;

/**
 * Modifies jgap's GABreeder so that negative fitness values are allowed.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPBreeder extends GABreeder {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPBreeder.class);


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Evolves the population. This is done with the following properties:
	 * -allows negative fitnesses
	 * -does NOT reinitialize fitnesses of chromosomes. Genetic operators
	 *  have the responsability to deliver new borned with "no fitness"
	 *  fitness values (This is automatic if the new borned are generated
	 *  with JointPlanOptimizerJGAPChromosome.clone() method).
	 */
	@Override
	public Population evolve(Population a_pop, Configuration a_conf) {
	    Population pop = a_pop;
	    int originalPopSize = a_conf.getPopulationSize();
	    IChromosome fittest = null;
	    // If first generation: Set age to one to allow genetic operations,
	    // see CrossoverOperator for an illustration.
	    // ----------------------------------------------------------------
	    if (a_conf.getGenerationNr() == 0) {
	      int size = pop.size();
	      for (int i = 0; i < size; i++) {
	        IChromosome chrom = pop.getChromosome(i);
	        chrom.increaseAge();
	      }
	    }
	    else {
	      // Select fittest chromosome in case it should be preserved and we are
	      // not in the very first generation.
	      // -------------------------------------------------------------------
	      if (a_conf.isPreserveFittestIndividual()) {
	        /**@todo utilize jobs. In pop do also utilize jobs, especially for fitness
	         * computation*/
	        fittest = pop.determineFittestChromosome(0, pop.size() - 1);
	      }
	    }
	    if (a_conf.getGenerationNr() > 0) {
	      // Adjust population size to configured size (if wanted).
	      // Theoretically, this should be done at the end of this method.
	      // But for optimization issues it is not. If it is the last call to
	      // evolve() then the resulting population possibly contains more
	      // chromosomes than the wanted number. But this is no bad thing as
	      // more alternatives mean better chances having a fit candidate.
	      // If it is not the last call to evolve() then the next call will
	      // ensure the correct population size by calling keepPopSizeConstant.
	      // ------------------------------------------------------------------
	      keepPopSizeConstant(pop, a_conf);
	    }
	    // Ensure fitness value of all chromosomes is udpated.
	    // ---------------------------------------------------
	    updateChromosomes(pop, a_conf);
	    // Apply certain NaturalSelectors before GeneticOperators will be executed.
	    // ------------------------------------------------------------------------
	    pop = applyNaturalSelectors(a_conf, pop, true);
	    // Execute all of the Genetic Operators.
	    // -------------------------------------
	    applyGeneticOperators(a_conf, pop);
	    // Reset fitness value of genetically operated chromosomes.
	    // ----------------------------------------------------------------
	    int currentPopSize = pop.size();
	    for (int i = originalPopSize; i < currentPopSize; i++) {
	      IChromosome chrom = pop.getChromosome(i);
		  //do NOT reset fitness value, as:
		  // - the JointPlanOptimizerChromosome.clone() method does it
		  // - "parents" fitness does not change, and thus does not have to be
		  //    evaluated again.
		  //-------------------------------------------------------------------
	      //chrom.setFitnessValueDirectly(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE);
	      // Mark chromosome as new-born.
	      // ----------------------------
	      chrom.resetAge();
	      // Mark chromosome as being operated on.
	      // -------------------------------------
	      chrom.increaseOperatedOn();
	    }
	    // Increase age of all chromosomes which are not modified by genetic
	    // operations.
	    // -----------------------------------------------------------------
	    int size = Math.min(originalPopSize, currentPopSize);
	    for (int i = 0; i < size; i++) {
	      IChromosome chrom = pop.getChromosome(i);
	      chrom.increaseAge();
	      // Mark chromosome as not being operated on.
	      // -----------------------------------------
	      chrom.resetOperatedOn();
	    }
	    // Ensure fitness value of all chromosomes is udpated.
	    // ---------------------------------------------------
	    updateChromosomes(pop, a_conf);

	    // Apply certain NaturalSelectors after GeneticOperators have been applied.
	    // ------------------------------------------------------------------------

	    pop = applyNaturalSelectors(a_conf, pop, false);

	    //BulkFitnessFunction bulkFunction = a_conf.getBulkFitnessFunction();
	    //if (bulkFunction != null) {
	    //  /**@todo utilize jobs: bulk fitness function is not so important for a
	    //   * prototype! */
	    //  bulkFunction.evaluate(pop);
	    //}
	    // Fill up population randomly if size dropped below specified percentage
	    // of original size.
	    // ----------------------------------------------------------------------
	    if (a_conf.getMinimumPopSizePercent() > 0) {
		  log.warn("GA population decreases!");
		  // do not use this, as the chromosome factory is not set to respect constraints
	      //int sizeWanted = a_conf.getPopulationSize();
	      //int popSize;
	      //int minSize = (int) Math.round(sizeWanted *
	      //                               (double) a_conf.getMinimumPopSizePercent()
	      //                               / 100);
	      //popSize = pop.size();
	      //if (popSize < minSize) {
	      //  IChromosome newChrom;
	      //  IChromosome sampleChrom = a_conf.getSampleChromosome();
	      //  Class sampleChromClass = sampleChrom.getClass();
	      //  IInitializer chromIniter = a_conf.getJGAPFactory().
	      //      getInitializerFor(sampleChrom, sampleChromClass);
	      //  while (pop.size() < minSize) {
	      //    try {
	      //      /**@todo utilize jobs as initialization may be time-consuming as
	      //       * invalid combinations may have to be filtered out*/
	      //      newChrom = (IChromosome) chromIniter.perform(sampleChrom,
	      //          sampleChromClass, null);
	      //      pop.addChromosome(newChrom);
	      //    } catch (Exception ex) {
	      //      throw new RuntimeException(ex);
	      //    }
	      //  }
	      //}
	    }
	    reAddFittest(pop, fittest);
	    // Increase number of generations.
	    // -------------------------------
	    a_conf.incrementGenerationNr();
	    // Fire an event to indicate we've performed an evolution.
	    // -------------------------------------------------------
	    a_conf.getEventManager().fireGeneticEvent(
	        new GeneticEvent(GeneticEvent.GENOTYPE_EVOLVED_EVENT, this));
	    return pop;
	}

	@Override
	public Object clone() {
		log.warn("GA breeder cloned. This is unexpected.");
		return new JointPlanOptimizerJGAPBreeder();
	}

	@Override
	public Population getLastPopulation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Configuration getLastConfiguration() {
		throw new UnsupportedOperationException();
	}
}
