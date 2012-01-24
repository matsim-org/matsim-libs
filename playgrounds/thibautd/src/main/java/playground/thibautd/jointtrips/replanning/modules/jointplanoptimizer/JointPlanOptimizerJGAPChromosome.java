/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPChromosome.java
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

import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.IChromosomePool;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;

import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerFitnessFunction;

/**
 * Extends org.jgap.Chromosome so that it can take negative fitness values.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPChromosome extends Chromosome {
	private static final long serialVersionUID = 1L;
	private static final double EPSILON = 1E-7;

	private final int nBooleanGenes;
	private final int nDoubleGenes;
	private final int nModeGenes;
	private final double dayDuration;
	private final List<Integer> nDurationGenes;

	public JointPlanOptimizerJGAPChromosome(
			final Configuration a_configuration,
			final Gene[] genes) throws InvalidConfigurationException {
		super(a_configuration, genes);
		super.m_fitnessValue = JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE;

		try {
			this.nBooleanGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumJointEpisodes();
			this.nDoubleGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumEpisodes();
			this.nModeGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumModeGenes();
			this.dayDuration = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getDayDuration();
			this.nDurationGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNDurationGenesPerIndiv();
		} catch (ClassCastException e) {
			throw new InvalidConfigurationException("JointPlanOptimizer chromosomes "+
					"must be initialized with JointPlanOptimizerJGAPConfiguration");
		}
	}

	public JointPlanOptimizerJGAPChromosome(
			final Configuration a_configuration) throws InvalidConfigurationException {
		super(a_configuration);
		super.m_fitnessValue = JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE;

		try {
			this.nBooleanGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumJointEpisodes();
			this.nDoubleGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumEpisodes();
			this.nModeGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNumModeGenes();
			this.dayDuration = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getDayDuration();
			this.nDurationGenes = ((JointPlanOptimizerJGAPConfiguration) a_configuration).getNDurationGenesPerIndiv();
		} catch (ClassCastException e) {
			throw new InvalidConfigurationException("JointPlanOptimizer chromosomes "+
					"must be initialized with JointPlanOptimizerJGAPConfiguration");
		}
	}

	/**
	 * Returns a deep clone of the chromosome, but with its fitness set to the
	 * "no fitness" value.
	 */
	@Override
	public synchronized Object clone() {
		// Before doing anything, make sure that a Configuration object
		// has been set on this Chromosome. If not, then throw an
		// IllegalStateException.
		// ------------------------------------------------------------
		if (getConfiguration() == null) {
			throw new IllegalStateException(
					"The active Configuration object must be set on this " +
			"Chromosome prior to invocation of the clone() method.");
		}
		IChromosome copy = null;
		// Now, first see if we can pull a Chromosome from the pool and just
		// set its gene values (alleles) appropriately.
		// ------------------------------------------------------------
		IChromosomePool pool = getConfiguration().getChromosomePool();
		if (pool != null) {
			copy = pool.acquireChromosome();
			if (copy != null) {
				Gene[] genes = copy.getGenes();
				for (int i = 0; i < size(); i++) {
					genes[i].setAllele(getGene(i).getAllele());
				}
			}
		}
		try {
			if (copy == null) {
				// We couldn't fetch a Chromosome from the pool, so we need to create
				// a new one. First we make a copy of each of the Genes. We explicity
				// use the Gene at each respective gene location (locus) to create the
				// new Gene that is to occupy that same locus in the new Chromosome.
				// -------------------------------------------------------------------
				int size = size();
				if (size > 0) {
					Gene[] copyOfGenes = new Gene[size];
					for (int i = 0; i < copyOfGenes.length; i++) {
						copyOfGenes[i] = getGene(i).newGene();
						copyOfGenes[i].setAllele(getGene(i).getAllele());
					}
					// Now construct a new Chromosome with the copies of the genes and
					// return it. Also clone the IApplicationData object.
					// ---------------------------------------------------------------
					/**@todo clone Config!*/
					copy = new JointPlanOptimizerJGAPChromosome(getConfiguration(), copyOfGenes);
				}
				else {
					copy = new JointPlanOptimizerJGAPChromosome(getConfiguration());
				}
				// do NOT clone the fitness, as this function is mainly used to
				// create new chromosomes to be modified, and thus re-evaluated.
				//copy.setFitnessValue(m_fitnessValue);
				copy.setFitnessValueDirectly(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE);
			}
			// Clone constraint checker.
			// -------------------------
			copy.setConstraintChecker(getConstraintChecker());
		}
		catch (InvalidConfigurationException iex) {
			throw new IllegalStateException(iex.getMessage());
		}
		// Also clone the IApplicationData object.
		// ---------------------------------------
		try {
			copy.setApplicationData(cloneObject(getApplicationData()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage());
		}
		return copy;
	}

	@Override
	public boolean isHandlerFor(
			final Object a_obj,
			final Class a_class) {
		return (a_class == JointPlanOptimizerJGAPChromosome.class);
	}

	@Override
	public Object perform(
			final Object a_obj,
			final Class a_class,
			final Object a_params)
	throws Exception {
		throw new UnsupportedOperationException("cannot create a random chromosome via perform");
	}

	public static IChromosome randomInitialChromosome() 
			throws InvalidConfigurationException {
		throw new UnsupportedOperationException("cannot create a random chromosome"
				+" in a static way, as the constraints depend on the corresponding plan.");
	}

	@Override
	public double getFitnessValue() {
		if ((JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE != super.m_fitnessValue)) {
			return super.m_fitnessValue;
		}
		return super.calcFitnessValue();
	}

	@Override
	public void setFitnessValue(final double a_newFitnessValue) {
		if (
				(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE != a_newFitnessValue) &&
				(Math.abs(m_fitnessValue - a_newFitnessValue) > 0.0000001)) {

			super.m_fitnessValue = a_newFitnessValue;
		}
	}

	/**
	 * Exactly the same as the Chromosome method, but with a more efficient
	 * implementation (less getter calls). Pass from 6% of CPU time used to 3%.
	 */
	public int hashCode() {
		Gene[] genes = getGenes();
		int geneHashcode;
		int hashCode = 1;
		if (genes != null) {
			for (int i = 0; i < genes.length; i++) {
				Gene gene = genes[i];
				if (gene == null) {
					geneHashcode = -55;
				}
				else {
					geneHashcode = gene.hashCode();
				}
				hashCode = 31 * hashCode + geneHashcode;
			}
		}
		return hashCode;
	}
}

