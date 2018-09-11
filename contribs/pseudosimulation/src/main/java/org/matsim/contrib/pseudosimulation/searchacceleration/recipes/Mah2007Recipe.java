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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Mah2007Recipe implements ReplannerIdentifierRecipe {

	// -------------------- MEMBERS --------------------

	private Set<Id<Person>> replannerIds = new LinkedHashSet<>();

	// -------------------- UTILITIES --------------------

	private class ByUtilityGainComparator implements Comparator<Map.Entry<?, Double>> {
		@Override
		public int compare(final Entry<?, Double> o1, final Entry<?, Double> o2) {
			return o2.getValue().compareTo(o1.getValue()); // largest values first
		}
	}

	// -------------------- CONSTRUCTION --------------------

	public Mah2007Recipe(final Map<Id<Person>, Double> personId2utilityGain, final double meanLambda) {
		final List<Map.Entry<Id<Person>, Double>> entryList = new ArrayList<>(personId2utilityGain.entrySet());
		Collections.sort(entryList, new ByUtilityGainComparator());
		for (int i = 0; i < meanLambda * personId2utilityGain.size(); i++) {
			this.replannerIds.add(entryList.get(i).getKey());
		}
	}

	// --------------- IMPLEMENTATION OF ReplannerIdentifierRecipe ---------------

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo) {
		return this.replannerIds.contains(personId);
	}

}
