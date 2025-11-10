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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import se.vti.utils.misc.math.BasicStatistics;
import se.vti.utils.misc.math.SetUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class EmulationErrorAnalyzer {

	// -------------------- MEMBERS --------------------

	private Map<Id<Person>, Double> personId2simulatedScore = null;
	private Map<Id<Person>, Double> personId2emulatedScore = null;

	private BasicStatistics meanErrorStats = null;
	private BasicStatistics meanAbsErrorStats = null;

	// -------------------- CONSTRUCTION --------------------

	EmulationErrorAnalyzer() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setEmulatedScores(final Population population) {
		this.personId2emulatedScore = population.getPersons().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getSelectedPlan().getScore()));
	}

	public void setEmulatedScores(final Map<Id<Person>, Double> personId2emulatedScore) {
		this.personId2emulatedScore = new LinkedHashMap<>(personId2emulatedScore);
	}

	public void setSimulatedScores(final Population population) {
		this.personId2simulatedScore = new LinkedHashMap<>();
		for (Person person : population.getPersons().values()) {
			this.personId2simulatedScore.put(person.getId(), person.getSelectedPlan().getScore());
		}
	}

	public Double getMeanError() {
		if (this.meanErrorStats != null) {
			return this.meanErrorStats.getAvg();
		} else {
			return null;
		}
	}

	public Double getMeanAbsError() {
		if (this.meanAbsErrorStats != null) {
			return this.meanAbsErrorStats.getAvg();
		} else {
			return null;
		}
	}

	public void update(Population population) {
		this.meanErrorStats = new BasicStatistics();
		this.meanAbsErrorStats = new BasicStatistics();
		final Set<Id<Person>> allPersonIds = SetUtils.union(this.personId2emulatedScore.keySet(),
				this.personId2simulatedScore.keySet());
		for (Id<Person> personId : allPersonIds) {
			final double err = this.personId2emulatedScore.get(personId) - this.personId2simulatedScore.get(personId);
			this.meanErrorStats.add(err);
			this.meanAbsErrorStats.add(Math.abs(err));
		}
	}
}
