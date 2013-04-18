/* *********************************************************************** *
 * project: org.matsim.*
 * IterationNumberMonitor.java
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
package playground.thibautd.tsplanoptimizer.framework;

/**
 * A simple {@link EvolutionMonitor} which uses a fixed number of iterations.
 * @author thibautd
 */
public class IterationNumberMonitor implements EvolutionMonitor<Object> {
	private final int n;

	public IterationNumberMonitor(final int numberOfIterations) {
		this.n = numberOfIterations;
	}

	@Override
	public boolean continueIterations(
			final int iteration,
			final Solution<?> newBest,
			final double newBestScore) {
		return iteration <= n;
	}
}

