package org.matsim.core.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * Test implementation of {@link ScenarioFileFormat} that adds a single dummy person.
 */
public class DummyScenarioFileFormat implements ScenarioFileFormat {

	@Override
	public Set<String> getSupportedExtensions() {
		return Set.of("dummypop");
	}

	@Override
	public void readPopulation(URL url, Scenario scenario, String inputCRS, String targetCRS,
							   Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		Population population = scenario.getPopulation();
		Person person = population.getFactory().createPerson(Id.createPersonId("dummy_person_from_spi"));
		Plan plan = population.getFactory().createPlan();
		person.addPlan(plan);
		population.addPerson(person);
	}
}