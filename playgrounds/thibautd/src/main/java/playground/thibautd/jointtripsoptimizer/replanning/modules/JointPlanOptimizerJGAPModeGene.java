/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerJGAPModeGene.java
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jgap.BaseGene;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;
import org.jgap.UnsupportedRepresentationException;

/**
 * A JGAP gene for mode choice optimisation.
 *
 * The value of the gene corresponds to an ordered list of modes, sorted by 
 * "preference". This ensures that for any alleles combination, there always
 * exists a corresponding feasible chain.
 *
 * @author thibautd
 */
public class JointPlanOptimizerJGAPModeGene extends BaseGene {

	private static final long serialVersionUID = 1L;
	private final List<String> geneValue;

	public JointPlanOptimizerJGAPModeGene(
			final Configuration a_configuration,
			final List<String> possibleModes)
			throws InvalidConfigurationException {
		super(a_configuration);
		this.geneValue = new ArrayList<String>(possibleModes);
	}

	public JointPlanOptimizerJGAPModeGene(
			final JointPlanOptimizerJGAPModeGene toCopy)
			throws InvalidConfigurationException {
		this(toCopy.getConfiguration(), toCopy.geneValue);
	}

	/**
	 * {@inheritDoc}
	 * @see BaseGene#getInternalValue()
	 */
	@Override
	protected Object getInternalValue() {
		return this.geneValue;
	}

	public List<String> getListValue() {
		return this.geneValue;
	}

	/**
	 * {@inheritDoc}
	 * @see BaseGene#newGeneInternal()
	 */
	@Override
	protected Gene newGeneInternal() {
		try {
			return new JointPlanOptimizerJGAPModeGene(
					this.getConfiguration(),
					new ArrayList<String>(this.geneValue));
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException("Got an InvalidConfigurationException while"
					+" creating a new gene from an existing JointPlanOptimizerJGAPModeGene:"
					+" this should never occur!");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Gene#setAllele(Object)
	 */
	@Override
	public void setAllele(Object a_newValue) {
		if ( (!(a_newValue instanceof List<?>)) ||
				(!(((List) a_newValue).get(0) instanceof String)) ) {
			throw new IllegalArgumentException("the allele of a mode gene must be"
					+" a list of strings");
		}

		this.geneValue.clear();

		for (Object value : (List) a_newValue) {
			this.geneValue.add((String) value);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Gene#getPersistentRepresentation()
	 */
	@Override
	public String getPersistentRepresentation()
		throws UnsupportedOperationException {
		// we do not care about XML Marshalling of the genetic population.
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * @see Gene#setValueFromPersistentRepresentation(String)
	 */
	@Override
	public void setValueFromPersistentRepresentation(String a_representation)
		throws UnsupportedOperationException, UnsupportedRepresentationException {
		// we do not care about XML Marshalling of the genetic population.
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 * @see Gene#applyMutation(int,double)
	 */
	@Override
	public void applyMutation(int index, double a_percentage) {
		// does not make sense here. Use setToRandomValue to mutate.
		throw new UnsupportedOperationException();
	}

	@Override
	public void setToRandomValue(RandomGenerator a_numberGenerator) {
		Collections.shuffle(
				this.geneValue,
				(Random) a_numberGenerator);
	}

	/**
	 * The ordering corresponds to the ordering on the String representation
	 * of the alleles.
	 *
	 * returns Integer.MIN_VALUE if the two objects are not instances of the same class.
	 */
	@Override
	public int compareTo(Object o) {
		return ((o.getClass() == this.getClass()) ?
				(this.toString().compareTo(o.toString())) :
				Integer.MIN_VALUE);
	}

	@Override
	public String toString() {
		return this.geneValue.toString();
	}
}

