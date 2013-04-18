/* *********************************************************************** *
 * project: org.matsim.*
 * EvolutionPlotter.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.charts.XYLineChart;

/**
 * A {@link AppliedMoveListener} which plots the evolution of the fitness,
 * for debugging/analysis purpose.
 *
 * @author thibautd
 */
public class EvolutionPlotter<T> implements AppliedMoveListener<T> , StartListener<T> , EndListener<T> {
	private final String outputFile;
	private final String title;
	private final List<Double> values = new ArrayList<Double>();

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	public EvolutionPlotter(
			final String title,
			final String outputFile) {
		this.outputFile = outputFile;
		this.title = title;
	}

	@Override
	public void notifyMove(
			final Solution<? extends T> currentSolution,
			final Move toApply,
			final double newFitness) {
		values.add( toApply != null ? newFitness : Double.NaN );
	}

	@Override
	public void notifyEnd(
			final Solution<? extends T> bestSolution,
			final double bestScore,
			final int nIterations) {
		double[] iterationNumbers = new double[ values.size() ];
		double[] scores =  new double[ values.size() ];

		int count = 0;
		for (Double value : values) {
			iterationNumbers[ count ] = count;
			scores[ count ] = value;
			count++;
		}

		XYLineChart chart = new XYLineChart( title , "iteration" , "score" );
		chart.addSeries( title , iterationNumbers , scores );
		chart.saveAsPng( outputFile , WIDTH , HEIGHT );
	}

	@Override
	public void notifyStart(
			final Solution<? extends T> startSolution,
			final double startScore) {
		values.add( startScore );
	}
}

