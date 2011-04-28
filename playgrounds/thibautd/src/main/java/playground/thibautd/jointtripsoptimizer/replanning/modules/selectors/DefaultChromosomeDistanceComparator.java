/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultChromosomeDistanceComparator.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.selectors;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;

/**
 * distance comparator that uses the discrete distance on discrete variable (0 if
 * a=b, 1 otherwise) and the "distance 1" for continuous variables.
 * @author thibautd
 */
public class DefaultChromosomeDistanceComparator extends ChromosomeDistanceComparator {

	private final double discreteRatio;

	public DefaultChromosomeDistanceComparator(final double discreteRatio) {
		this.discreteRatio = discreteRatio;
	}

	@Override
	protected double getDistance(IChromosome chr1, IChromosome chr2) {
		double distance = 0d;
		int size = chr1.size();

		if ((chr1.getClass() != chr2.getClass()) || (chr2.size() != size)) {
			throw new IllegalArgumentException("incompatible chromosomes");
		}

		Gene[] genes1 = chr1.getGenes();
		Gene[] genes2 = chr2.getGenes();

		for (int i=0; i < size; i++) {
			if (isDiscrete(genes1[i])) {
				distance += 
					this.discreteRatio * getDiscreteDistance(genes1[i], genes2[i]);
			}
			else {
				distance += Math.abs(
						((DoubleGene) genes1[i]).doubleValue() -
						((DoubleGene) genes2[i]).doubleValue());
			}
		}

		return distance;
	}

	private boolean isDiscrete(final Gene gene) {
		return !(gene instanceof DoubleGene);
	}

	private double getDiscreteDistance(final Gene gene1, final Gene gene2) {
		if (gene1 instanceof BooleanGene) {
			return ((gene1.getAllele()).equals(gene2.getAllele()) ? 0 : 1);
		}
		else {
			return 0d;
		}
	}
}

