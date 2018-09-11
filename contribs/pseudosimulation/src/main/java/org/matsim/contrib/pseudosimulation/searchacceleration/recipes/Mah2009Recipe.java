/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.recipes;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Mah2009Recipe implements ReplannerIdentifierRecipe {

	// -------------------- CONSTANTS --------------------

	private final static double minGain = 1e-9;

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Double> person2utilityGain;

	private final double meanLambda;

	private final double replanProbaConstant;

	// -------------------- CONSTRUCTION --------------------

	public Mah2009Recipe(final Map<Id<Person>, Double> person2utilityGain, final double meanLambda) {

		// TODO Inefficient; sort population according once beforehand.

		double tmpReplanProbaConstant = Double.NEGATIVE_INFINITY;
		for (Double gain : person2utilityGain.values()) {
			tmpReplanProbaConstant = Math.max(tmpReplanProbaConstant, 1.0 / meanLambda / Math.max(minGain, gain));
		}

		double replanProbaSum;
		do {

			double smallestInvalidUtilityGain = Double.POSITIVE_INFINITY;
			int invalidCnt = 0;
			replanProbaSum = 0.0;

			for (Double gain : person2utilityGain.values()) {
				final double truncatedGain = Math.max(minGain, gain);
				final double proba = tmpReplanProbaConstant * meanLambda * truncatedGain;
				replanProbaSum += Math.min(1.0, proba);
				if (proba > 1.0) {
					invalidCnt++;
					smallestInvalidUtilityGain = Math.min(smallestInvalidUtilityGain, truncatedGain);
				}
			}

			// System.out.println("c = " + replanProbaConstant + ", #invalid = " +
			// invalidCnt
			// + ", smallestInvalidUtilityGain = " + smallestInvalidUtilityGain + ", avg.
			// repl. proba = "
			// + (replanProbaSum / person2utilityGain.size()));

			if (invalidCnt > 0) {
				tmpReplanProbaConstant = (1.0 - 1e-9) / meanLambda / smallestInvalidUtilityGain;
			}

		} while (replanProbaSum > meanLambda * person2utilityGain.size());

		// System.out.println("terminating");
		// System.exit(0);

		this.replanProbaConstant = tmpReplanProbaConstant;
		this.person2utilityGain = person2utilityGain;
		this.meanLambda = meanLambda;
	}

	// --------------- IMPLEMENTATION OF ReplannerIdentifierRecipe ---------------

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo) {
		final double replanProba = this.replanProbaConstant * this.meanLambda * this.person2utilityGain.get(personId);
		return (MatsimRandom.getRandom().nextDouble() < replanProba);
	}
}
