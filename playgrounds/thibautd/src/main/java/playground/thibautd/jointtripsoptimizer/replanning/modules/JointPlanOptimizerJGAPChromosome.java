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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.List;

import org.apache.log4j.Logger;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.IChromosomePool;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;

/**
 * Extends org.jgap.Chromosome so that it can take negative fitness values.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPChromosome extends Chromosome {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerJGAPChromosome.class);

	private static final long serialVersionUID = 1L;

	private final int nBooleanGenes;
	private final int nDoubleGenes;
	private final int nModeGenes;
	private final double dayDuration;
	private final List<Integer> nDurationGenes;

	public JointPlanOptimizerJGAPChromosome(Configuration a_configuration, Gene[] genes) throws InvalidConfigurationException {
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

	public JointPlanOptimizerJGAPChromosome(Configuration a_configuration) throws InvalidConfigurationException {
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
	public boolean isHandlerFor(Object a_obj, Class a_class) {
		return (a_class == JointPlanOptimizerJGAPChromosome.class);
	}

	@Override
	public Object perform(Object a_obj, Class a_class, Object a_params)
	throws Exception {
		return randomInitialJointPlanOptimizerJGAPChromosome();
	}

	/**
	 * Copied from Chromosome.randomInitialChromosome, but just returning a JointPlanOptimizerJGAPChromosome instance rather than a org.jgap.Chromosome instance
	 *
	 * @param a_configuration
	 * @return
	 * @throws InvalidConfigurationException
	 */
	public IChromosome randomInitialJointPlanOptimizerJGAPChromosome()
			throws InvalidConfigurationException {
		// Sanity check: make sure the given configuration isn't null.
		// -----------------------------------------------------------
		if (getConfiguration() == null) {
			throw new IllegalArgumentException(
			"Configuration instance must not be null");
		}
		// Lock the configuration settings so that they can't be changed
		// from now on.
		// -------------------------------------------------------------
		getConfiguration().lockSettings();
		// First see if we can get a Chromosome instance from the pool.
		// If we can, we'll randomize its gene values (alleles) and then
		// return it.
		// ------------------------------------------------------------
		//IChromosomePool pool = getConfiguration().getChromosomePool();
		//if (pool != null) {
		//	IChromosome randomChromosome = pool.acquireChromosome();
		//	if (randomChromosome != null) {
		//		Gene[] genes = randomChromosome.getGenes();
		//		RandomGenerator generator = getConfiguration().getRandomGenerator();
		//		for (int i = 0; i < genes.length; i++) {
		//			genes[i].setToRandomValue(generator);
		//		}
		//		randomChromosome.setFitnessValueDirectly(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE);
		//		return randomChromosome;
		//	}
		//}
		// If we got this far, then we weren't able to get a Chromosome from
		// the pool, so we have to construct a new instance and build it from
		// scratch.
		// ------------------------------------------------------------------
		//IChromosome sampleChromosome = getConfiguration().getSampleChromosome();
		//Gene[] sampleGenes = sampleChromosome.getGenes();
		//Gene[] newGenes = new Gene[sampleGenes.length];
		Gene[] newGenes = new Gene[this.nBooleanGenes + this.nDoubleGenes + this.nModeGenes];
		DoubleGene newDoubleGene;
		JointPlanOptimizerJGAPModeGene newModeGene;
		Double[] randomDurations = new Double[this.nDoubleGenes + 1];
		RandomGenerator generator = getConfiguration().getRandomGenerator();
		double scalingFactor = 0d;
		int countDoubleGenes = 0;

		// create a random chromosome, taking into account the structure of a
		// JointPlan chromosome and the time constraints
		for (int j=0; j < this.nBooleanGenes; j++) {
			newGenes[j] = new BooleanGene(this.getConfiguration(),
					generator.nextBoolean());
		}
		
		for (int planLength : this.nDurationGenes) {
			scalingFactor = 0d;
			for (int j=0; j <= planLength; j++) {
				randomDurations[j] = generator.nextDouble();
				scalingFactor += randomDurations[j];
			}

			scalingFactor = this.dayDuration / scalingFactor;

			for (int j=0; j < planLength; j++) {
				newDoubleGene =  new DoubleGene(this.getConfiguration(), 0d, this.dayDuration);
				newDoubleGene.setAllele(scalingFactor * randomDurations[j]);

				newGenes[this.nBooleanGenes + countDoubleGenes] = newDoubleGene;
				countDoubleGenes++;
			}
		}

		if (this.nModeGenes > 0) {
			List<String> possibleModes = ((JointPlanOptimizerJGAPModeGene) 
					this.getGene(this.nBooleanGenes + this.nDoubleGenes)).getListValue();
			for (int j=0; j < this.nModeGenes; j++) {
				//TODO create and initialize to a random value
				newModeGene = new JointPlanOptimizerJGAPModeGene(
						this.getConfiguration(),
						possibleModes);
				newModeGene.setToRandomValue(generator);
				newGenes[this.nBooleanGenes + this.nDoubleGenes + j] =
					newModeGene;
			}
		}

		// Finally, construct the new chromosome with the new random
		// genes values and return it.
		// ---------------------------------------------------------
		return new JointPlanOptimizerJGAPChromosome(getConfiguration(), newGenes);

	}

	public static IChromosome randomInitialChromosome() 
			throws InvalidConfigurationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getFitnessValue() {
		if ((JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE != super.m_fitnessValue)) {
			return super.m_fitnessValue;
		}
		return super.calcFitnessValue();
	}

	@Override
	public void setFitnessValue(double a_newFitnessValue) {
		if (
				(JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE != a_newFitnessValue) &&
				(Math.abs(m_fitnessValue - a_newFitnessValue) > 0.0000001)) {

			super.m_fitnessValue = a_newFitnessValue;
		}
	}
}

