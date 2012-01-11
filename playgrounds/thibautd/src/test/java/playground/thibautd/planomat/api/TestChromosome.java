/* *********************************************************************** *
 * project: org.matsim.*
 * TestChromosome.java
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

import java.util.ArrayList;
import java.util.List;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;
import org.jgap.InvalidConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the planomat-specific JGAP chromosome.
 *
 * @author thibautd
 */
public class TestChromosome {
	private PlanomatChromosome chromosome;

	@Before
	public void createFixtureChromosome() throws InvalidConfigurationException {
		List<Gene> genes = new ArrayList<Gene>();
		Configuration.reset();
		Configuration conf = new Configuration();

		genes.add( new BooleanGene( conf ) );
		genes.add( new DoubleGene( conf , 0 , 1 ) );

		chromosome =
			new PlanomatChromosome(
				conf,
				genes);
	}

	/**
	 * tests the correct cloning behaviour
	 */
	@Test
	public void testClone() {
		Object clone = chromosome.clone();

		Assert.assertTrue(
				"unexpected clone class: "+clone.getClass().getName(),
				clone instanceof PlanomatChromosome);

		Assert.assertNotSame(
				"clone references the same object as the cloned!",
				chromosome,
				clone);

		Assert.assertEquals(
				"clone does not equals cloned!",
				chromosome,
				clone);

		Gene[] genes1 = chromosome.getGenes();
		Gene[] genes2 = ((PlanomatChromosome) clone).getGenes();

		for (int i=0; i < genes1.length; i++) {
			Assert.assertNotSame(
					"internal genes references for clone are the same as in cloned!",
					genes1[i],
					genes2[i]);
		}
	}

	@Test
	public void testEquals() {
		Assert.assertEquals(
				"chromosome is not equal to itself!",
				chromosome,
				chromosome);
	}

	@Test
	public void testFitnessBehaviour() throws InvalidConfigurationException {
		double value = 1;
		FixedValueFitnessFunction fitness = new FixedValueFitnessFunction( value );
		chromosome.getConfiguration().setFitnessFunction(
				 fitness);

		Assert.assertEquals(
				"problem in fitness",
				value,
				chromosome.getFitnessValue(),
				MatsimTestUtils.EPSILON);

		chromosome.setFitnessValueDirectly( PlanomatFitnessFunction.NO_FITNESS_VALUE );
		value = -10;
		fitness.setValue( value );

		Assert.assertEquals(
				"problem in fitness",
				value,
				chromosome.getFitnessValue(),
				MatsimTestUtils.EPSILON);
	}
}

class FixedValueFitnessFunction extends PlanomatFitnessFunction {
	private double value;

	public FixedValueFitnessFunction(final double value) {
		this.value = value;
	}

	public void setValue(final double value) {
		this.value = value;
	}

	@Override
	protected double evaluate(IChromosome arg0) {
		return value;
	}

	@Override
	public PlanomatChromosome getSampleChomosome()
			throws InvalidConfigurationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void modifyBackPlan(IChromosome chromosome) {
		throw new UnsupportedOperationException();
	}
}
