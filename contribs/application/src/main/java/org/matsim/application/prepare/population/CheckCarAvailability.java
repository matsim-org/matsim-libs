package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "check-car-avail", description = "Check and fix violation of car availability.", showDefaultValues = true)
public class CheckCarAvailability implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(CheckCarAvailability.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population")
	private Path output;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--mode", description = "Substitute mode in case of violation", defaultValue = TransportMode.ride)
	private String substMode;

	private int violations;

	public static void main(String[] args) {
		new CheckCarAvailability().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		init();

		for (Person person : population.getPersons().values()) {
			run(person);
		}

		if (violations > 0) {
			log.warn("Fixed {} car availability violations.", violations);
		} else
			log.info("No violations occurred.");

		if (output == null) {
			log.info("Not writing to output.");

		} else {

			if (output.getParent() != null) Files.createDirectories(output.getParent());

			log.info("Writing to {}", output);

			PopulationUtils.writePopulation(population, output.toString());
		}


		return 0;
	}

	/**
	 * Needs to be called before {@link #run(Person)}
	 */
	public void init() {
		violations = 0;
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (!subpopulation.isEmpty() && !subpop.equals(subpopulation)) return;

		if (PersonUtils.canUseCar(person)) return;

		boolean correct = true;
		for (Plan plan : person.getPlans()) {
			if (!checkPlan(plan)) {
				correct = false;
			}
		}

		if (!correct) {
			log.warn("Violation in person {}", person.getId());
			violations++;
		}

	}

	public int getViolations() {
		return violations;
	}

	/**
	 * Check plan for violations and also fix them.
	 */
	private boolean checkPlan(Plan plan) {

		boolean correct = true;

		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
			for (Leg leg : trip.getLegsOnly()) {

				if (TransportMode.car.equals(leg.getMode()) || TransportMode.car.equals(TripStructureUtils.getRoutingMode(leg))) {
					correct = false;

					// replace this whole trip now
					List<PlanElement> planElements = plan.getPlanElements();
					List<PlanElement> fullTrip = planElements.subList(planElements.indexOf(trip.getOriginActivity()) + 1, planElements.indexOf(trip.getDestinationActivity()));
					fullTrip.clear();

					Leg l = PopulationUtils.createLeg(substMode);
					TripStructureUtils.setRoutingMode(l, substMode);
					fullTrip.add(l);

					break;
				}
			}
		}

		return correct;
	}
}
