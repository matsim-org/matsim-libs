/**
 * se.vti.atap
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

import se.vti.utils.misc.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
abstract class AbstractReplannerSelector {

	// -------------------- CONSTANTS --------------------

	protected final Function<Integer, Double> iterationToStepSize;

	// -------------------- MEMBERS --------------------

	private Double targetReplanningRate = null;

	private Double realizedReplanningRate = null;

	private Double meanReplannerFilteredGap = null;

	private Double meanFilteredGap = null;

	private boolean hasReplannedBefore = false;

	private Integer replanIteration = null;

	// -------------------- CONSTRUCTION --------------------

	static AbstractReplannerSelector newReplannerSelector(final ATAPConfigGroup greedoConfig) {

		if (ATAPConfigGroup.ReplannerIdentifierType.UNIFORM.equals(greedoConfig.getReplannerIdentifier())) {
			return new BasicReplannerSelector(false, greedoConfig.newIterationToTargetReplanningRate());

		} else if (ATAPConfigGroup.ReplannerIdentifierType.SORTING.equals(greedoConfig.getReplannerIdentifier())) {
			return new BasicReplannerSelector(true, greedoConfig.newIterationToTargetReplanningRate());

		} else if (ATAPConfigGroup.ReplannerIdentifierType.ATAP_APPROXIMATE_DISTANCE
				.equals(greedoConfig.getReplannerIdentifier())) {
			return new UpperBoundReplannerSelector(greedoConfig);

		} else if (ATAPConfigGroup.ReplannerIdentifierType.ATAP_EXACT_DISTANCE
				.equals(greedoConfig.getReplannerIdentifier())) {
			return new AtomicUpperBoundReplannerSelector(greedoConfig.newIterationToTargetReplanningRate(),
					greedoConfig.newDistanceTransformation());

		} else if (ATAPConfigGroup.ReplannerIdentifierType.DO_NOTHING.equals(greedoConfig.getReplannerIdentifier())) {
			return new AbstractReplannerSelector(null) {
				@Override
				Set<Id<Person>> selectReplanners(Map<Id<Person>, Double> personId2filteredGap, int replanIteration) {
					return Collections.emptySet();
				}

				@Override
				Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> personId2filteredGap) {
					return null;
				}
			};

		} else {
			throw new RuntimeException("Unknown replanner selector type: " + greedoConfig.getReplannerIdentifier());
		}
	}

	AbstractReplannerSelector(final Function<Integer, Double> iterationToStepSize) {
		this.iterationToStepSize = iterationToStepSize;
	}

	// -------------------- PARTIAL IMPLEMENTATION --------------------

	Double getTargetReplanningRate() {
		return this.targetReplanningRate;
	}

	Double getRealizedReplanningRate() {
		return this.realizedReplanningRate;
	}

	Double getMeanFilteredGap() {
		return this.meanFilteredGap;
	}

	Double getMeanReplannerFilteredGap() {
		return this.meanReplannerFilteredGap;
	}

	Integer getReplanIteration() {
		return this.replanIteration;
	}

	void setDistanceToReplannedPopulation(final PopulationDistance populationDistance) {
		// default implementation does nothing
	}

	Set<Id<Person>> selectReplanners(final Map<Id<Person>, Double> personId2filteredGap, final int replanIteration) {
		this.replanIteration = replanIteration;

		final Set<Id<Person>> replannerIds;
		if (this.hasReplannedBefore) {
			this.targetReplanningRate = this.iterationToStepSize.apply(replanIteration);
			replannerIds = this.selectReplannersHook(personId2filteredGap);
		} else {
			this.targetReplanningRate = 1.0;
			replannerIds = new LinkedHashSet<>(personId2filteredGap.keySet());
			this.hasReplannedBefore = true;
		}

		this.meanFilteredGap = personId2filteredGap.values().stream().mapToDouble(g -> g).average().getAsDouble();
		this.realizedReplanningRate = ((double) replannerIds.size()) / personId2filteredGap.size();
		if (replannerIds.size() == 0) {
			this.meanReplannerFilteredGap = null;
		} else {
			this.meanReplannerFilteredGap = replannerIds.stream().mapToDouble(id -> personId2filteredGap.get(id))
					.average().getAsDouble();
		}

		return replannerIds;
	}

	List<Tuple<Id<Person>, Double>> toList(final Map<Id<Person>, Double> personId2Gap,
			final boolean sortByDescendingGap) {
		final List<Tuple<Id<Person>, Double>> personIdAndGap = new ArrayList<>(personId2Gap.size());
		personId2Gap.entrySet().forEach(e -> personIdAndGap.add(new Tuple<>(e.getKey(), e.getValue())));
		Collections.shuffle(personIdAndGap, MatsimRandom.getRandom());

		if (sortByDescendingGap) {
			Collections.sort(personIdAndGap, new Comparator<Tuple<Id<Person>, Double>>() {
				@Override
				public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
				}
			});
		}

		return personIdAndGap;
	}

	abstract Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> personId2filteredGap);
}
