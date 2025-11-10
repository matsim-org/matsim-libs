/**
 * org.matsim.contrib.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim;

import java.util.LinkedList;
import java.util.function.Function;

/**
 * 
 * @author GunnarF
 *
 */
class AmbitionLevelBasedEtaSchedule {

	// -------------------- CONSTANTS --------------------

	private final int minAverageIterations;

	private final double averageFraction;

	private final Function<Integer, Double> iterationToTargetRelaxationRate;

	// -------------------- MEMBERS --------------------

	private Double initialGap = null;

	private final LinkedList<Double> gaps = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	AmbitionLevelBasedEtaSchedule(final int minAverageIterations, final double averageFraction,
			final Function<Integer, Double> iterationToTargetRelaxationRate) {
		this.minAverageIterations = minAverageIterations;
		this.averageFraction = averageFraction;
		this.iterationToTargetRelaxationRate = iterationToTargetRelaxationRate;
	}

	// -------------------- IMPLEMENTATION --------------------

	void registerGap(final double gap) {
		this.gaps.addFirst(gap);
	}

	double getEta(final int iteration, final boolean constrain) {
		final double etaMSA = this.iterationToTargetRelaxationRate.apply(iteration);
		final int averageIts = (int) (this.averageFraction * iteration + 1e-3);
		final double eta;
		if (averageIts < this.minAverageIterations) {
			eta = etaMSA;
		} else {
			final double meanGap = this.gaps.subList(0, averageIts).stream().mapToDouble(g -> g).average()
					.getAsDouble();
			if (this.initialGap == null) {
				this.initialGap = meanGap;
			}
			eta = etaMSA * (this.initialGap / meanGap);
		}
		if (constrain) {
			return Math.max(0.0, Math.min(1.0, eta));
		} else {
			return eta;
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		AmbitionLevelBasedEtaSchedule ags = new AmbitionLevelBasedEtaSchedule(5, 0.5, it -> Math.pow(1.0 + it, -0.5));

		System.out.println("gap\teta");
		for (int it = 0; it < 1000; it++) {
			double gap = 200.0 * Math.pow(1 + it, -0.25);
			gap += 0.2 * (Math.random() - 0.5) * gap;

			ags.registerGap(gap);

			System.out.println(gap + "\t" + ags.getEta(it, false));
		}
	}
}
