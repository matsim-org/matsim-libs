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
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * An attempt to enforce "best response" behavior.
 *
 * @author Gunnar Flötteröd
 *
 */
public class RemoveAllButSelectedPlan implements BeforeMobsimListener {

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			final Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("Person " + person.getId() + " has selected plan: " + selectedPlan);
			}
			person.getPlans().clear();
			person.addPlan(selectedPlan);
			person.setSelectedPlan(selectedPlan);
		}
	}

}
