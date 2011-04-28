/* *********************************************************************** *
 * project: org.matsim.*
 * ChromosomeDistanceComparator.java
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgap.IChromosome;

/**
 * Comparator aimed at sorting chromosomes according to their distance to a given
 * chromosome.
 * @author thibautd
 */
public abstract class ChromosomeDistanceComparator implements Comparator<IChromosome> {

	private final Map<IChromosome, Double> distanceMap = 
		new HashMap<IChromosome, Double>();

	public void setComparisonData(
			final IChromosome newBorn, 
			final List<IChromosome> window) {
		distanceMap.clear();

		for (IChromosome chrom : window) {
			distanceMap.put(chrom, getDistance(newBorn, chrom));
		}
	}

	@Override
	public int compare(
			final IChromosome chr1,
			final IChromosome chr2) {
		double d1, d2;

		try {
			d1 = this.distanceMap.get(chr1);
			d2 = this.distanceMap.get(chr2);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("can only compare chromosomes in"
					+" the comparison data");
		}

		if (chr1.equals(chr2)) {
			return 0;
		}
		return (d1 > d2 ? 1 : -1);
	}

	/**
	 * Defines the distance.
	 */
	protected abstract double getDistance(IChromosome chr1, IChromosome chr2);
}

