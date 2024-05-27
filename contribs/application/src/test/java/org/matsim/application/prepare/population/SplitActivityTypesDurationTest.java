package org.matsim.application.prepare.population;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;


public class SplitActivityTypesDurationTest {


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void split() {

		Path input = Path.of(utils.getPackageInputDirectory(), "persons.xml");

		Path output = Path.of(utils.getOutputDirectory(), "persons-act-split.xml");

		new SplitActivityTypesDuration().execute(
				"--input", input.toString(),
				"--output", output.toString()
		);

		Population population = PopulationUtils.readPopulation(output.toString());

		for (Person person : population.getPersons().values()) {

			List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

			for (Activity act : activities) {
				Assertions.assertThat(act.getType())
						.matches(".+_\\d+");

				if (act.getType().contains("_1800"))
					Assertions.assertThat(act.getMaximumDuration().isDefined());

			}
		}
	}
}
