/* *********************************************************************** *
 * project: org.matsim.*
 * ImprovementDelayMonitor.java
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
 * An {@link EvolutionMonitor} using a maximum number of iterations without
 * improvement (and an optionnal maximum number of iterations).
 *
 * @author thibautd
 */
public class ImprovementDelayMonitor<T> implements EvolutionMonitor<T>, StartListener<T> {
	private final int maxIters;
	private final int delay;
	private double lastBestScore = Double.NEGATIVE_INFINITY;
	private int lastImprovingIter = -1;

	public ImprovementDelayMonitor(
			final int delay,
			final int maxIters) {
		this.delay = delay;
		this.maxIters = maxIters;
	}

	@Override
	public boolean continueIterations(
			final int iteration,
			final Solution<? extends T> newBest,
			final double newBestScore) {
		if (iteration >= maxIters) {
			return false;
		}

		if (newBestScore > lastBestScore) {
			lastImprovingIter = iteration;
			lastBestScore = newBestScore;
		}

		return iteration - lastImprovingIter < delay;
	}

	@Override
	public void notifyStart(
			final Solution<? extends T> startSolution,
			final double startScore) {
		lastBestScore = startScore;
	}
}

