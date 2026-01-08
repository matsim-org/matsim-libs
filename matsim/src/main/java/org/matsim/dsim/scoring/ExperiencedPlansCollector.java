package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ExperiencedPlansService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExperiencedPlansCollector implements ExperiencedPlansService {

	private final Map<Id<Person>, Plan> plans = new ConcurrentHashMap<>();

	private final Config config;
	private final Population population;
	private final Network network;

	@Inject
	public ExperiencedPlansCollector(Config config, Population population, Network network) {
		this.config = config;
		this.population = population;
		this.network = network;
	}

	public void addExperiencedPlan(Id<Person> personId, Plan plan) {
		if (!population.getPersons().containsKey(personId)) return;

		plans.put(personId, plan);
	}

	@Override
	public void writeExperiencedPlans(String filename) {

		var tmpPop = PopulationUtils.createPopulation(config, network);
		for (var entry : plans.entrySet()) {
			var person = population.getFactory().createPerson(entry.getKey());
			if (person == null) {
				continue;
			}
			var origPerson = population.getPersons().get(entry.getKey());

			for (var attrEntry : origPerson.getAttributes().getAsMap().entrySet()) {
				person.getAttributes().putAttribute(attrEntry.getKey(), attrEntry.getValue());
				// note that this is not a completely deep copy.  Should not be a problem since we only write to file, but in the end we never know.  kai, oct'25
			}
			entry.getValue().setScore(origPerson.getSelectedPlan().getScore());
			// yyyy this is somewhat dangerous ... since there is no guarantee that this is indeed the correct plan.
			// ... up to here.
			// There is EquilTwoAgentsTest, where I switched on the experienced plans writing in the scoring config.
			// W/o the code lines above, the person attributes are not written.  W/ the code lines, they are written.
			// This is, evidently, not a true regression test, but at least I had a look if the functionality works at all. kai, oct'25

			Plan plan = entry.getValue();
			person.addPlan(plan);
			tmpPop.addPerson(person);
		}
		new PopulationWriter(tmpPop, null).write(filename);
	}

	@Override
	public Map<Id<Person>, Plan> getExperiencedPlans() {
		return plans;
	}

	@Override
	public void finishIteration() {
		// nothing to do here.
	}
}
