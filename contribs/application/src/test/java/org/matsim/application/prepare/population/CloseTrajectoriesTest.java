package org.matsim.application.prepare.population;

import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CloseTrajectoriesTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void main() {

		Path input = Path.of(utils.getPackageInputDirectory(), "persons.xml");

		Path output = Path.of(utils.getOutputDirectory(), "persons-with-home.xml");

		new CloseTrajectories().execute(
				input.toString(),
				"--threshold", "60",
				"--min-duration", "0",
				"--output", output.toString()
		);

		Population population = PopulationUtils.readPopulation(output.toString());

		for (Person person : population.getPersons().values()) {

			List<Activity> acts = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			assertThat(acts).last().satisfies(new Condition<>(act -> act.getType().startsWith("home"), "Is home activity"));

		}


	}
}