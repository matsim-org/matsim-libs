package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "set-car-avail", description = "Set car availability to true above a certain age.", showDefaultValues = true)
public class SetCarAvailabilityByAge implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(SetCarAvailabilityByAge.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population")
	private Path output;

	@CommandLine.Option(names = "--age", description = "Agents with age greater or equal will have car availability set to always", defaultValue = "18")
	private int age;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	public static void main(String[] args) {
		new SetCarAvailabilityByAge().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		for (Person person : population.getPersons().values()) {
			run(person);
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}


	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (!subpopulation.isEmpty() && !subpop.equals(subpopulation)) return;

		Integer age = PersonUtils.getAge(person);

		if (age == null)
			return;

		if (age >= this.age) {
			PersonUtils.setCarAvail(person, "always");

			if ("no".equals(PersonUtils.getLicense(person)))
				PersonUtils.setLicence(person, "yes");
		}
	}
}
