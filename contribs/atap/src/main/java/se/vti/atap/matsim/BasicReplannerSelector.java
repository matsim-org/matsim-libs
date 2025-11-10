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
class BasicReplannerSelector extends AbstractReplannerSelector {

	private final boolean sortByGap;

	// -------------------- CONSTRUCTION --------------------

	BasicReplannerSelector(final boolean sortByGap, final Function<Integer, Double> iterationToTargetReplanningRate) {
		super(iterationToTargetReplanningRate);
		this.sortByGap = sortByGap;
	}

	// --------------- IMPLEMENTATION OF AbstractReplannerSelector ---------------

	@Override
	public Set<Id<Person>> selectReplannersHook(final Map<Id<Person>, Double> personId2Gap) {

		final List<Tuple<Id<Person>, Double>> personIdAndGap = new ArrayList<>(personId2Gap.size());
		personId2Gap.entrySet().forEach(e -> personIdAndGap.add(new Tuple<>(e.getKey(), e.getValue())));
		Collections.shuffle(personIdAndGap, MatsimRandom.getRandom());

		if (this.sortByGap) {
			Collections.sort(personIdAndGap, new Comparator<Tuple<Id<Person>, Double>>() {
				@Override
				public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
				}
			});
		}

		final int replannerCnt = Math.max(1, (int) (this.getTargetReplanningRate() * personIdAndGap.size()));
		final Set<Id<Person>> replannerIds = new LinkedHashSet<>(replannerCnt);
		personIdAndGap.subList(0, replannerCnt).forEach(t -> replannerIds.add(t.getA()));

		return replannerIds;
	}
}
