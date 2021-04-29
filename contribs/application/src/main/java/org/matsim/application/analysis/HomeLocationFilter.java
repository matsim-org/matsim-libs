package org.matsim.application.analysis;

import org.apache.log4j.Logger;
import org.matsim.analysis.AgentFilter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;

import java.util.HashSet;
import java.util.Set;

public class HomeLocationFilter implements AgentFilter {

	private final Set<Id<Person>> consider = new HashSet<>();
	private static final String HOME_ACTIVITY_TYPE_PREFIX = "home";

	public HomeLocationFilter(ShpOptions analysisAreaShapeFile, String inputCRS, Population population) {

		ShpOptions.Index index = analysisAreaShapeFile.createIndex(inputCRS, "not used");

		for (Person person : population.getPersons().values()) {

			for (PlanElement el : person.getSelectedPlan().getPlanElements()) {
				if (el instanceof Activity && ((Activity) el).getType().startsWith(HOME_ACTIVITY_TYPE_PREFIX)) {

					Coord coord = ((Activity) el).getCoord();
					if (index.contains(coord)) {
						consider.add(person.getId());
						break;
					}
				}
			}
		}
	}

	/**
	 * Number of agents in the zone.
	 */
	public int size() {
		return consider.size();
	}

	@Override
	public boolean considerAgent(Person person) {
		return consider.contains(person.getId());
	}

	@Override
	public String toFileName() {
		return "homeLocation";
	}

}
