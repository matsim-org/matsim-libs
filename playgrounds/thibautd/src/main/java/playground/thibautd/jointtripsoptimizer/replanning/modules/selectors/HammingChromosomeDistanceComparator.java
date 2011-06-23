/* *********************************************************************** *
 * project: org.matsim.*
 * HammingChromosomeDistanceComparator.java
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

/**
 * {@link ChromosomeDistanceComparator} using only the hamming distance on toggle
 * genes.
 *
 * @author thibautd
 */
public class HammingChromosomeDistanceComparator  extends ChromosomeDistanceComparator {

	/**
	 * @return the number of BooleanGene which values are different in the two 
	 * chromosomes
	 */
	@Override
	protected double getDistance(final IChromosome chr1, final IChromosome chr2) {
		double distance = 0d;
		int size = chr1.size();

		if ((chr1.getClass() != chr2.getClass()) || (chr2.size() != size)) {
			throw new IllegalArgumentException("incompatible chromosomes");
		}

		Gene[] genes1 = chr1.getGenes();
		Gene[] genes2 = chr2.getGenes();

		for (int i=0; i < size; i++) {
			if (isDiscrete(genes1[i])) {
				distance += getDiscreteDistance(genes1[i], genes2[i]);
			}
		}

		return distance;
	}

	private boolean isDiscrete(final Gene gene) {
		return (gene instanceof BooleanGene);
	}

	private double getDiscreteDistance(final Gene gene1, final Gene gene2) {
		return ((gene1.getAllele()).equals(gene2.getAllele()) ? 0 : 1);
	}
}

