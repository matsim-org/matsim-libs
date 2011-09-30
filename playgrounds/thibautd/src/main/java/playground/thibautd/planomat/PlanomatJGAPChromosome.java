/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatJGAPChromosome.java
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

package playground.thibautd.planomat;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.IChromosomePool;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;

public class PlanomatJGAPChromosome extends Chromosome {

	private static final long serialVersionUID = 1L;

//	private boolean isHashValueComputed = false;
//	private int hashValue;

	public PlanomatJGAPChromosome(Configuration a_configuration, Gene[] genes) throws InvalidConfigurationException {
		super(a_configuration, genes);
		super.m_fitnessValue = PlanomatFitnessFunctionWrapper.NO_FITNESS_VALUE;
	}

	public PlanomatJGAPChromosome(Configuration a_configuration) throws InvalidConfigurationException {
		super(a_configuration);
		super.m_fitnessValue = PlanomatFitnessFunctionWrapper.NO_FITNESS_VALUE;
	}

//	// else compute hash and save it in this.hashValue
//	Checksum hashAlgo = new Adler32();
//	for (Gene gene : this.getGenes()) {
//	int i = ((IntegerGene) gene).intValue();
//	hashAlgo.update(i);
//	}
//	this.hashValue = (int) hashAlgo.getValue();

//	return this.hashValue;
//	}

//	@Override
//	public int hashCode() {
//		if (!this.isHashValueComputed) {
//			this.hashValue = super.hashCode();
//			this.isHashValueComputed = true;
//		}
//		return this.hashValue;
//	}
//
//	@Override
//	public boolean equals(Object other) {
//		return (this.hashCode() == other.hashCode());
//	}

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
					copy = new PlanomatJGAPChromosome(getConfiguration(), copyOfGenes);
				}
				else {
					copy = new PlanomatJGAPChromosome(getConfiguration());
				}
				copy.setFitnessValue(m_fitnessValue);
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
		return (a_class == PlanomatJGAPChromosome.class);
	}

	@Override
	public Object perform(Object a_obj, Class a_class, Object a_params)
	throws Exception {
		return randomInitialPlanomatJGAPChromosome();
	}

	/**
	 * Copied from Chromosome.randomInitialChromosome, but just returning a PlanomatJGAPChromosome instance rather than a org.jgap.Chromosome instance
	 *
	 * @param a_configuration
	 * @return
	 * @throws InvalidConfigurationException
	 */
	public IChromosome randomInitialPlanomatJGAPChromosome()
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
		IChromosomePool pool = getConfiguration().getChromosomePool();
		if (pool != null) {
			IChromosome randomChromosome = pool.acquireChromosome();
			if (randomChromosome != null) {
				Gene[] genes = randomChromosome.getGenes();
				RandomGenerator generator = getConfiguration().getRandomGenerator();
				for (int i = 0; i < genes.length; i++) {
					genes[i].setToRandomValue(generator);
				}
				randomChromosome.setFitnessValueDirectly(PlanomatFitnessFunctionWrapper.NO_FITNESS_VALUE);
				return randomChromosome;
			}
		}
		// If we got this far, then we weren't able to get a Chromosome from
		// the pool, so we have to construct a new instance and build it from
		// scratch.
		// ------------------------------------------------------------------
		IChromosome sampleChromosome = getConfiguration().getSampleChromosome();
		Gene[] sampleGenes = sampleChromosome.getGenes();
		Gene[] newGenes = new Gene[sampleGenes.length];
		RandomGenerator generator = getConfiguration().getRandomGenerator();
		for (int i = 0; i < newGenes.length; i++) {
			// We use the newGene() method on each of the genes in the
			// sample Chromosome to generate our new Gene instances for
			// the Chromosome we're returning. This guarantees that the
			// new Genes are setup with all of the correct internal state
			// for the respective gene position they're going to inhabit.
			// -----------------------------------------------------------
			newGenes[i] = sampleGenes[i].newGene();
			// Set the gene's value (allele) to a random value.
			// ------------------------------------------------
			newGenes[i].setToRandomValue(generator);
		}
		// Finally, construct the new chromosome with the new random
		// genes values and return it.
		// ---------------------------------------------------------
		return new PlanomatJGAPChromosome(getConfiguration(), newGenes);

	}

	@Override
	public double getFitnessValue() {
		if ((PlanomatFitnessFunctionWrapper.NO_FITNESS_VALUE != super.m_fitnessValue)) {
			return super.m_fitnessValue;
		}
		return super.calcFitnessValue();
	}

	@Override
	public void setFitnessValue(double a_newFitnessValue) {
		if (
				(PlanomatFitnessFunctionWrapper.NO_FITNESS_VALUE != a_newFitnessValue) &&
				(Math.abs(m_fitnessValue - a_newFitnessValue) > 0.0000001)) {

			m_fitnessValue = a_newFitnessValue;
		}
	}

}
