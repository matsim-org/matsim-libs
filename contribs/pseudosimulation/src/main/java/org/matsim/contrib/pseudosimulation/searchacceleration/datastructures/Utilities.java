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
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.RecursiveMovingAverage;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Utilities {

	// -------------------- INNER Entry CLASS --------------------

	public static class Entry {

		public final RecursiveMovingAverage averageRealizedImprovement;
		public final RecursiveMovingAverage averageExpectedImprovement;

		// objects to allow for initial null-value
		private Double previousRealizedUtility = null;
		private Double previousExpectedUtility = null;

		// guaranteed to be set upon construction
		private double currentRealizedUtility;
		private double currentExpectedUtility;

		private Entry(final double newRealizedUtility, final double newExpectedUtility, final int memoryLength) {
			this.currentRealizedUtility = newRealizedUtility;
			this.currentExpectedUtility = newExpectedUtility;
			this.averageExpectedImprovement = new RecursiveMovingAverage(memoryLength);
			this.averageRealizedImprovement = new RecursiveMovingAverage(memoryLength);
		}

		public void updateBeforeReplanning(final double newRealizedUtility, final double newExpectedUtility) {
			this.previousRealizedUtility = this.currentRealizedUtility;
			this.previousExpectedUtility = this.currentExpectedUtility;
			this.currentRealizedUtility = newRealizedUtility;
			this.currentExpectedUtility = newExpectedUtility;

			this.averageExpectedImprovement.add(this.previousExpectedUtility - this.previousRealizedUtility);
			this.averageRealizedImprovement.add(this.currentRealizedUtility - this.previousRealizedUtility);
		}

		public boolean previousDataValid() {
			return ((this.previousRealizedUtility != null) && (this.previousExpectedUtility != null));
		}

		public Double getPreviousRealizedUtility() {
			return previousRealizedUtility;
		}

		public Double getPreviousExpectedUtility() {
			return previousExpectedUtility;
		}

		public double getCurrentRealizedUtility() {
			return currentRealizedUtility;
		}

		public double getCurrentExpectedUtility() {
			return currentExpectedUtility;
		}

		public Double getPreviousExpectedUtilityChange() {
			return this.previousExpectedUtility - this.previousRealizedUtility;
		}

		// public boolean isImprover(final double relativeThreshold) {
		// if (this.averageExpectedImprovement.size() <
		// this.averageExpectedImprovement.memoryLength()) {
		// return true;
		// } else {
		// // TODO may need refinement
		// return (this.averageRealizedImprovement.average() >= relativeThreshold
		// * this.averageExpectedImprovement.average());
		// }
		// }
	}

	// -------------------- INNER SummaryStatistics CLASS --------------------

	public static class SummaryStatistics {

		public final Double currentRealizedUtilitySum;

		public final Double currentExpectedUtilitySum;

		// set to an unmodifiable instance
		public final Map<Id<Person>, Double> personId2currentDeltaUtility;

		public final Double currentDeltaUtilitySum;

		public final boolean previousDataValid;

		public final Double previousRealizedUtilitySum;

		public final Double previousExpectedUtilitySum;

		public final Double realizedUtilityImprovementSum;

		public final Double previousExpectedUtilityImprovementSum;

		// public final Double shareOfImprovers;

		private SummaryStatistics(final double currentRealizedUtilitySum, final double currentExpectedUtilitySum,
				final Map<Id<Person>, Double> personId2currentDeltaUtility, final double currentDeltaUtilitySum,
				final boolean previousDataValid, final Double previousRealizedUtilitySum,
				final Double previousExpectedUtilitySum
				// , final Double shareOfImprovers
				) {

			this.currentRealizedUtilitySum = currentRealizedUtilitySum;
			this.currentExpectedUtilitySum = currentExpectedUtilitySum;
			this.personId2currentDeltaUtility = Collections.unmodifiableMap(personId2currentDeltaUtility);
			this.currentDeltaUtilitySum = currentDeltaUtilitySum;

			this.previousDataValid = previousDataValid;
			if (this.previousDataValid) {
				this.previousRealizedUtilitySum = previousRealizedUtilitySum;
				this.previousExpectedUtilitySum = previousExpectedUtilitySum;
				this.realizedUtilityImprovementSum = this.currentRealizedUtilitySum - this.previousRealizedUtilitySum;
				this.previousExpectedUtilityImprovementSum = this.previousExpectedUtilitySum
						- this.previousRealizedUtilitySum;
				// this.shareOfImprovers = shareOfImprovers;
			} else {
				this.previousRealizedUtilitySum = null;
				this.previousExpectedUtilitySum = null;
				this.realizedUtilityImprovementSum = null;
				this.previousExpectedUtilityImprovementSum = null;
				// this.shareOfImprovers = null;
			}
		}
	}

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	private final int memoryLength;

	// -------------------- CONSTRUCTION --------------------

	public Utilities(final int memoryLength) {
		this.memoryLength = memoryLength;
	}

	// -------------------- CONTENT ACCESS --------------------

	public void update(final Id<Person> personId, final Double newRealizedUtility, final double newExpectedUtility) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			this.personId2entry.put(personId, new Entry(newRealizedUtility, newExpectedUtility, this.memoryLength));
		} else {
			entry.updateBeforeReplanning(newRealizedUtility, newExpectedUtility);
		}
	}

	public Entry getUtilities(final Id<Person> personId) {
		return this.personId2entry.get(personId);
	}

	public SummaryStatistics newSummaryStatistics() {

		if (this.personId2entry.size() == 0) {

			return null;

		} else {

			double currentRealizedUtilitySum = 0.0;
			double currentExpectedUtilitySum = 0.0;
			Map<Id<Person>, Double> personId2currentDeltaUtility = new LinkedHashMap<>();
			double currentDeltaUtilitySum = 0.0;

			boolean previousDataValid = true;
			double previousRealizedUtilitySum = 0.0;
			double previousExpectedUtilitySum = 0.0;
//			double improverCnt = 0.0;

			for (Map.Entry<Id<Person>, Entry> mapEntry : this.personId2entry.entrySet()) {
				final Id<Person> personId = mapEntry.getKey();
				final Entry entry = mapEntry.getValue();

				currentRealizedUtilitySum += entry.getCurrentRealizedUtility();
				currentExpectedUtilitySum += entry.getCurrentExpectedUtility();
				final double currentDeltaUtility = entry.getCurrentExpectedUtility()
						- entry.getCurrentRealizedUtility();
				personId2currentDeltaUtility.put(personId, currentDeltaUtility);
				currentDeltaUtilitySum += currentDeltaUtility;

				previousDataValid &= entry.previousDataValid();
				if (previousDataValid) {
					previousRealizedUtilitySum += entry.getPreviousRealizedUtility();
					previousExpectedUtilitySum += entry.getPreviousExpectedUtility();
//					if (entry.isImprover(relativeImprovementThreshold)) {
//						improverCnt++;
//					}
				}
			}

			final int cnt = this.personId2entry.size();
			System.out.println(cnt);
			System.out.println(previousDataValid);
			System.out.println(currentRealizedUtilitySum);
			System.out.println(currentExpectedUtilitySum);
			System.out.println(previousRealizedUtilitySum);
			System.out.println(previousExpectedUtilitySum);
			return new SummaryStatistics(currentRealizedUtilitySum, currentExpectedUtilitySum,
					personId2currentDeltaUtility, currentDeltaUtilitySum, previousDataValid,
					previousDataValid ? previousRealizedUtilitySum : null,
					previousDataValid ? previousExpectedUtilitySum : null
//							, previousDataValid ? improverCnt : null
									);
		}
	}
}
