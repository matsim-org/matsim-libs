/* *********************************************************************** *
 * project: org.matsim.*
 * JgapBreeder.java
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
package playground.thibautd.gaparamoptimizer;

import java.util.ArrayList;
import java.util.List;

import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.GABreeder;
import org.jgap.Population;

/**
 * @author thibautd
 */
public class JgapBreeder extends GABreeder {
	private static final long serialVersionUID = 1L;
	//private static final int N_THREADS = 20;

	@Override
	protected void updateChromosomes(
			final Population a_pop,
			final Configuration a_conf) {
		List<IChromosome> toScore = new ArrayList<IChromosome>();

		for (Object chrom : a_pop.getChromosomes()) {
			if (((IChromosome) chrom).getFitnessValueDirectly() == FitnessFunction.NO_FITNESS_VALUE) {
				toScore.add((IChromosome) chrom);
			}
		}

		//int step = toScore.size() / N_THREADS;
		//int first = 0;

		// launch fitness computing in threads
		List<Thread> threads = new ArrayList<Thread>();
		//for (int i=0; i < N_THREADS; i++) {
		//	Thread thread = new Thread(new Scorer(toScore.subList(first, first + step), a_conf));
		//	threads.add(thread);
		//	thread.start();
		//	first += step;
		//}

		for (IChromosome chrom : toScore) {
			Thread thread = new Thread(new IndividualScorer(chrom, a_conf));
			threads.add(thread);
			thread.start();
		}

		// wait for all threads to be dead before exiting
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

class IndividualScorer implements Runnable {
	private final IChromosome toScore;
	private final ParameterOptimizerFitness fitness;

	public IndividualScorer(
			final IChromosome toScore,
			final Configuration jgapConfig) {
		this.toScore = toScore;
		this.fitness = ((ParameterOptimizerFitness) jgapConfig.getFitnessFunction()).clone();
	}

	@Override
	public void run() {
		toScore.setFitnessValueDirectly(fitness.evaluate(toScore));
	}
}

//class Scorer implements Runnable {
//	private final List<IChromosome> toScore;
//	private final ParameterOptimizerFitness fitness;
//
//	public Scorer(
//			final List<IChromosome> toScore,
//			final Configuration jgapConfig) {
//		this.toScore = toScore;
//		this.fitness = ((ParameterOptimizerFitness) jgapConfig.getFitnessFunction()).clone();
//	}
//
//	@Override
//	public void run() {
//		for (IChromosome chrom : toScore) {
//			chrom.setFitnessValueDirectly(fitness.evaluate(chrom));
//		}
//	}
//}
