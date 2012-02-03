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
import org.jgap.IGeneConstraintChecker;
import org.jgap.IInitializer;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;

import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.configuration.JointPlanOptimizerJGAPConfiguration;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.fitness.JointPlanOptimizerFitnessFunction;
import playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.geneticoperators.ConstraintsManager;

/**
 * Extends org.jgap.Chromosome so that it can take negative fitness values.
 * @author thibautd
 */
public class JointPlanOptimizerJGAPChromosome implements IChromosome, IInitializer {
	private static final long serialVersionUID = 1L;

	private final Chromosome delegate;

	public JointPlanOptimizerJGAPChromosome(
			final Configuration configuration) throws InvalidConfigurationException {
		this( new Chromosome( configuration ) );
	}

	public JointPlanOptimizerJGAPChromosome(
			final Configuration configuration,
			final List<Gene> genes) throws InvalidConfigurationException {
		this( configuration , genes.toArray( new Gene[0] ) );
	}

	public JointPlanOptimizerJGAPChromosome(
			final Configuration configuration,
			final Gene[] genes) throws InvalidConfigurationException {
		this( new Chromosome( configuration , genes ) );
	}

	// provide the constructors with constaint checker?

	private JointPlanOptimizerJGAPChromosome(final Chromosome delegate) throws InvalidConfigurationException {
		if ( !(delegate.getConfiguration() instanceof JointPlanOptimizerJGAPConfiguration) ) {
			throw new InvalidConfigurationException(
					"a "+this.getClass()+" instance must be initialised with a "
					+"configuration of type JointPlanOptimizerJGAPConfiguration."
					+" Got a "+delegate.getConfiguration().getClass()+" instead." );
		}
		this.delegate = delegate;
		delegate.setFitnessValueDirectly( JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE );
	}

	// /////////////////////////////////////////////////////////////////////////
	// modified methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @see Chromosome#clone()
	 */
	@Override
	public Object clone() {
		try {
			// the fitness will be invalidated. As we clone only to modify afterwards,
			// it is not a big deal.
			return new JointPlanOptimizerJGAPChromosome( (Chromosome) delegate.clone() );
		}
		catch (InvalidConfigurationException e) {
			throw new RuntimeException( e );
		}
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
		 double fit = delegate.getFitnessValueDirectly();

		 if (fit == JointPlanOptimizerFitnessFunction.NO_FITNESS_VALUE) {
			 fit = delegate.getConfiguration().getFitnessFunction().getFitnessValue( delegate );
			 delegate.setFitnessValueDirectly( fit );
		 }

		 return fit;
	}

	/**
	 * @see Chromosome#equals(Object)
	 */
	@Override
	public boolean equals(final Object other) {
		// as the equality is interface based, this should be ok
		return delegate.equals( other );
	}

	@Override
	public boolean isHandlerFor(
			final Object a_obj,
			final Class a_class) {
		return getClass().equals( a_class );
	}

	/**
	 * Creates a new random chromosome, respecting the constraints defined by the
	 * {@link ConstraintsManager} obtained from the configuration.
	 */
	@Override
	public Object perform(
			final Object a_obj,
			final Class a_class,
			final Object a_params) throws Exception {
		return createRandomChromosome();
	}

	public IChromosome createRandomChromosome() throws InvalidConfigurationException {
		IChromosome newChrom = new JointPlanOptimizerJGAPChromosome( (Chromosome) delegate.clone() );
		JointPlanOptimizerJGAPConfiguration conf =
			(JointPlanOptimizerJGAPConfiguration) newChrom.getConfiguration();
		RandomGenerator random = conf.getRandomGenerator();

		for (Gene gene : newChrom.getGenes()) {
			if ( !(gene instanceof DoubleGene) ) {
				gene.setToRandomValue( random );
			}
		}

		conf.getConstraintsManager().randomiseChromosome( newChrom );

		return newChrom;
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

