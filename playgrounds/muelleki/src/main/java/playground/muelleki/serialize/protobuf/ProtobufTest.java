package playground.muelleki.serialize.protobuf;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.muelleki.serialize.protobuf.gen.Protos;

public class ProtobufTest {

	public static void main(String[] args) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));

		Population pop = scenario.getPopulation();

		for (Person person : pop.getPersons().values()) {
			Protos.Person.Builder builder = Protos.Person.newBuilder().setId(
					person.getId().toString());

			for (Plan plan : person.getPlans()) {
				builder.addPlans(buildPlan(plan));
			}
		}
	}

	private static Protos.Plan buildPlan(Plan plan) {
		return null;
	}
}
