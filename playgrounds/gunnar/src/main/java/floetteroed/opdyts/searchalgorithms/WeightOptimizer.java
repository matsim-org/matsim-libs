/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.opdyts.searchalgorithms;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WeightOptimizer {

	// -------------------- CONSTANTS --------------------

	private final double sigmaFactor;

	// -------------------- MEMBERS --------------------

	private int it = 0;

	private double finalEquilGapSum = 0.0;

	private double finalUnifGapSum = 0.0;

	private double finalSigma2sum = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public WeightOptimizer(final double sigmaFactor) {
		this.sigmaFactor = sigmaFactor;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION --------------------

	public double[] updateWeights(double finalEquilGap, double finalUnifGap,
			double finalSigma) {

		this.it++;
		this.finalEquilGapSum += finalEquilGap;
		this.finalUnifGapSum += finalUnifGap;
		this.finalSigma2sum += finalSigma * finalSigma;

		finalEquilGap = this.finalEquilGapSum / this.it;
		finalUnifGap = this.finalUnifGapSum / this.it;
		finalSigma = Math.sqrt(this.finalSigma2sum / this.it);

		/*
		 * Two criteria implemented here:
		 * 
		 * v * eg + w * ug = f * sigma
		 * 
		 * v * eg = w * ug
		 * 
		 * =>
		 * 
		 * 2 * {v * eg, w * ug} = f * sigma
		 * 
		 * =>
		 * 
		 * {v, w} = (f * sigma) / (2 * {eg, ug})
		 */
		final double equilGapWeight = (this.sigmaFactor * finalSigma)
				/ (2.0 * finalEquilGap + 1e-8);
		final double unifGapWeight = (this.sigmaFactor * finalSigma)
				/ (2.0 * finalUnifGap + 1e-8);
		return new double[] { equilGapWeight, unifGapWeight };
	}
}
