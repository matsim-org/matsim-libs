/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.simulatedannealing;

import java.util.ArrayList;
import java.util.List;

import floetteroed.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <S>
 *            the solution type
 */
public class SimulatedAnnealing<S> {

	// -------------------- CONSTANT MEMBERS --------------------

	private final SolutionGenerator<S> generator;

	private final SolutionEvaluator<S> evaluator;

	private final List<ProgressListener<S>> progressListeners = new ArrayList<ProgressListener<S>>();

	// -------------------- MEMBERS --------------------

	private boolean verbose = false;

	private boolean recomputeCurrentSolution = false;

	// TODO >>>>> NEW >>>>>
	private double successThreshold = Double.NEGATIVE_INFINITY;
	// TODO <<<<< NEW <<<<<
	
	private S xOpt;

	private double xQOpt;

	// -------------------- CONSTRUCTION --------------------

	public SimulatedAnnealing(final SolutionGenerator<S> generator,
			final SolutionEvaluator<S> evaluator) {
		if (generator == null) {
			throw new IllegalArgumentException("generator is null");
		}
		if (evaluator == null) {
			throw new IllegalArgumentException("evaluator is null");
		}
		this.generator = generator;
		this.evaluator = evaluator;
		this.reset();
		// this.progressListeners.add(new TextOutputProgressListener<S>());
	}

	// -------------------- SETTERS --------------------

	public void addListener(final ProgressListener<S> listener) {
		if (listener != null) {
			this.progressListeners.add(listener);
		}
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public void setRecomputeCurrentSolution(
			final boolean recomputeCurrentSolution) {
		this.recomputeCurrentSolution = recomputeCurrentSolution;
	}

	public void setSuccessThreshold(final double successThreshold) {
		this.successThreshold = successThreshold;
	}

	// -------------------- GETTERS --------------------

	public S getOptimalSolution() {
		return this.xOpt;
	}

	public double getOptimalEvaluation() {
		return this.xQOpt;
	}

	// -------------------- OPTIMIZATION --------------------

	private void msg(final int it, final double xQ) {
		if (this.verbose) {
			System.out.println("it. " + it + ": f(x) = " + xQ + ", fOpt(x) = "
					+ this.xQOpt + ", xOpt = " + this.xOpt);
		}
	}

	private S newRandomFeasibleSolution() {
		S x = null;
		do {
			x = this.generator.randomGeneration();
		} while (!this.evaluator.feasible(x));
		return x;
	}

	private void updateBestSolution(final S x, final double xQ) {
		if (xQ < xQOpt) {
			this.xOpt = this.generator.copy(x);
			this.xQOpt = xQ;
		}
	}

	public void reset() {
		this.xOpt = null;
		this.xQOpt = Double.POSITIVE_INFINITY;
	}

	public double proposeGreediness(final int maxPrepIt) {
		S x = null;
		double xQ = Double.POSITIVE_INFINITY;
		final BasicStatistics stats = new BasicStatistics();
		for (int it = 1; it <= maxPrepIt; it++) {
			x = this.newRandomFeasibleSolution();
			xQ = this.evaluator.evaluation(x);
			this.updateBestSolution(x, xQ);
			this.msg(it, xQ);
			stats.add(xQ);
		}
		return 1.0 / stats.getStddev();
	}

	public void run(final S initialSolution, final int maxIt,
			final double greediness) {
		this.run(initialSolution, maxIt, greediness, maxIt);
	}
	
	public void run(final S initialSolution, final int maxIt,
			final double greediness, int maxFailureIterations) {

		int failures = 0;
		
		S x;
		double xQ;
		if (initialSolution == null) {
			x = this.newRandomFeasibleSolution();
		} else {
			x = initialSolution;
		}
		xQ = this.evaluator.evaluation(x);
		this.updateBestSolution(x, xQ);

		msg(1, xQ);
		for (ProgressListener<S> progressListener : this.progressListeners) {
			progressListener.notifyCurrentState(x, xQ, xQ);
		}

		for (int it = 2; (it <= maxIt) && (xQ > this.successThreshold) && (failures < maxFailureIterations); it++) {
			// TODO >>>>> NEW >>>>>
			S y;
			do {
				y = this.generator.variation(x);
			} while (!this.evaluator.feasible(y));
			// TODO <<<<< NEW <<<<<

			// TODO >>>>> NEW >>>>>
			if (this.recomputeCurrentSolution) {
				xQ = this.evaluator.evaluation(x);
			}
			// TODO <<<<< NEW <<<<<

			double yQ = this.evaluator.evaluation(y);
			this.updateBestSolution(y, yQ);
			
			if (Double.isInfinite(greediness)) {
				if (yQ < xQ) {
					x = y;
					xQ = yQ;
					failures = 0;
				} else {
					failures++;
				}
			} else {
				final double lambda = greediness * Math.log(it);
				final double alpha = Math.exp(lambda * (xQ - yQ));
				if (Math.random() < alpha) {
					x = y;
					xQ = yQ;
					failures = 0;
				} else {
					failures++;
				}				
			}

			this.msg(it, xQ);
			for (ProgressListener<S> progressListener : this.progressListeners) {
				progressListener.notifyCurrentState(x, xQ, yQ);
			}

		}
	}
}
