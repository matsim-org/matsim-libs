/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatChromosome.java
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
package playground.thibautd.planomat.api;

import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.IGeneConstraintChecker;
import org.jgap.InvalidConfigurationException;

/**
 * Implementation of a Jgap chromosome which allows negative
 * fitness values.
 * To provide a sample chromosome, initialise an instance of this class
 * with the desired sequence of chromosomes.
 *
 * @author thibautd
 */
// TODO: test!
public class PlanomatChromosome implements IChromosome {
	private static final long serialVersionUID = 1L;

	private final Chromosome delegate;

	public PlanomatChromosome(
			final Configuration configuration) throws InvalidConfigurationException {
		this( new Chromosome( configuration ) );
	}

	public PlanomatChromosome(
			final Configuration configuration,
			final List<Gene> genes) throws InvalidConfigurationException {
		this( configuration , genes.toArray( new Gene[0] ) );
	}

	public PlanomatChromosome(
			final Configuration configuration,
			final Gene[] genes) throws InvalidConfigurationException {
		this( new Chromosome( configuration , genes ) );
	}

	// provide the constructors with constaint checker?

	private PlanomatChromosome(final Chromosome delegate) {
		this.delegate = delegate;
	}

	// /////////////////////////////////////////////////////////////////////////
	// modified methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @see Chromosome#clone()
	 */
	@Override
	public Object clone() {
		return new PlanomatChromosome( (Chromosome) delegate.clone() );
	}

	/**
	 * @see org.jgap.IChromosome#setFitnessValue(double)
	 */
	@Override
	public void setFitnessValue(final double fitness) {
		delegate.setFitnessValueDirectly( fitness );
	}

	/**
	 * @see org.jgap.IChromosome#getFitnessValue()
	 */
	@Override
	public double getFitnessValue() {
		// should be OK not to modify
		return delegate.getFitnessValue();
	}

	/**
	 * @see Chromosome#equals(Object)
	 */
	@Override
	public boolean equals(final Object other) {
		// as the equality is interface based, this should be ok
		return delegate.equals( other );
	}

	/**
	 * @param conf 
	 * @return 
	 * @throws InvalidConfigurationException 
	 * @see Chromosome#randomInitialChromosome(Configuration)
	 */
	public static IChromosome randomInitialChromosome(final Configuration conf)
		throws InvalidConfigurationException {
		return new PlanomatChromosome( (Chromosome) Chromosome.randomInitialChromosome(conf) );
	}

	/**
	 * @see Chromosome#compareTo(Object)
	 */
	@Override
	public int compareTo(final Object arg0) {
		// as the comparison is interface based, this should be ok
		return delegate.compareTo(arg0);
	}

	// /////////////////////////////////////////////////////////////////////////
	// delegate methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @see org.jgap.IChromosome#getGene(int)
	 */
	@Override
	public Gene getGene(final int index) {
		return delegate.getGene( index );
	}

	/**
	 * @see org.jgap.IChromosome#getGenes()
	 */
	@Override
	public Gene[] getGenes() {
		return delegate.getGenes();
	}

	/**
	 * @see org.jgap.IChromosome#setGenes(Gene[])
	 */
	@Override
	public void setGenes(final Gene[] genes)
		throws InvalidConfigurationException {
		delegate.setGenes(genes);
	}

	/**
	 * @see org.jgap.IChromosome#size()
	 */
	@Override
	public int size() {
		return delegate.size();
	}

	/**
	 * @see org.jgap.IChromosome#setFitnessValueDirectly(double)
	 */
	@Override
	public void setFitnessValueDirectly(final double arg0) {
		delegate.setFitnessValueDirectly(arg0);
	}

	/**
	 * @see org.jgap.IChromosome#getFitnessValueDirectly()
	 */
	@Override
	public double getFitnessValueDirectly() {
		return delegate.getFitnessValueDirectly();
	}

	/**
	 * @see Chromosome#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}

	/**
	 * @see Chromosome#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @see org.jgap.IChromosome#setIsSelectedForNextGeneration(boolean)
	 */
	@Override
	public void setIsSelectedForNextGeneration(final boolean arg0) {
		delegate.setIsSelectedForNextGeneration(arg0);
	}

	/**
	 * @see org.jgap.IChromosome#isSelectedForNextGeneration()
	 */
	@Override
	public boolean isSelectedForNextGeneration() {
		return delegate.isSelectedForNextGeneration();
	}

	/**
	 * @see org.jgap.IChromosome#setConstraintChecker(IGeneConstraintChecker)
	 */
	@Override
	public void setConstraintChecker(final IGeneConstraintChecker arg0)
		throws InvalidConfigurationException {
		delegate.setConstraintChecker(arg0);
	}

	/**
	 * @see org.jgap.IChromosome#setApplicationData(Object)
	 */
	@Override
	public void setApplicationData(Object arg0) {
		delegate.setApplicationData(arg0);
	}

	/**
	 * @see org.jgap.IChromosome#getApplicationData()
	 */
	@Override
	public Object getApplicationData() {
		return delegate.getApplicationData();
	}

	/**
	 * @see org.jgap.IChromosome#cleanup()
	 */
	@Override
	public void cleanup() {
		delegate.cleanup();
	}

	/**
	 * @see org.jgap.IChromosome#getConfiguration()
	 */
	@Override
	public Configuration getConfiguration() {
		return delegate.getConfiguration();
	}

	/**
	 * @see org.jgap.IChromosome#increaseAge()
	 */
	@Override
	public void increaseAge() {
		delegate.increaseAge();
	}

	/**
	 * @see org.jgap.IChromosome#resetAge()
	 */
	@Override
	public void resetAge() {
		delegate.resetAge();
	}

	/**
	 * @see org.jgap.IChromosome#setAge(int)
	 */
	@Override
	public void setAge(int arg0) {
		delegate.setAge(arg0);
	}

	/**
	 * @see org.jgap.IChromosome#getAge()
	 */
	@Override
	public int getAge() {
		return delegate.getAge();
	}

	/**
	 * @see org.jgap.IChromosome#increaseOperatedOn()
	 */
	@Override
	public void increaseOperatedOn() {
		delegate.increaseOperatedOn();
	}

	/**
	 * @see org.jgap.IChromosome#resetOperatedOn()
	 */
	@Override
	public void resetOperatedOn() {
		delegate.resetOperatedOn();
	}

	/**
	 * @see org.jgap.IChromosome#operatedOn()
	 */
	@Override
	public int operatedOn() {
		return delegate.operatedOn();
	}
}

