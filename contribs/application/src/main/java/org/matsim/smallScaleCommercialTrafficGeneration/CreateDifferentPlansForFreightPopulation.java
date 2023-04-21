package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

@CommandLine.Command(name = "generate-plan-variants-for-freight", description = "Generates variants of plans for a population of freight agents", showDefaultValues = true)

public class CreateDifferentPlansForFreightPopulation implements MATSimAppCommand {

	private enum PlanVariantStrategy{changeStartingTimes}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the population", defaultValue = "output/testOutput/testPopulation_new.xml.gz")
	private Path populationPath;
	@CommandLine.Option(names = "--outputPopulationPath", description = "Path to the outputPopulation", defaultValue = "output/testOutput/testPopulationVariant.xml.gz", required = true)
	private Path outputPopulationPath;
	@CommandLine.Option(names = "--numberOfPlanVariants", description = "Set the number of plan variants", required = true, defaultValue = "5")
	private static int numberOfPlanVariants;
	@CommandLine.Option(names = "--earliestTourStartTime", description = "Set earliest tour startTime in seconds", defaultValue = "21600")
	private static int earliestTourStartTime;
	@CommandLine.Option(names = "--latestTourStartTime", description = "Set latest tour startTime in seconds", defaultValue = "50400")
	private static int latestTourStartTime;
	@CommandLine.Option(names = "--typicalTourDuration", description = "Set typical tour duration in seconds", defaultValue = "28800")
	private static int typicalTourDuration;
	@CommandLine.Option(names = "--seed", description = "Set seed", defaultValue = "4411")
	private static int seed;
	private static final SplittableRandom rnd  = new SplittableRandom(seed);
	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateDifferentPlansForFreightPopulation()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		PlanVariantStrategy selectedPlanVariantStrategy = PlanVariantStrategy.changeStartingTimes;

		Population population = PopulationUtils.readPopulation(populationPath.toString());
		createPlanVariants(selectedPlanVariantStrategy, population);
		PopulationUtils.writePopulation(population, outputPopulationPath.toString());
		return null;
	}
	public static void createPlanVariantsForPopulations(String planVariantStrategy, Population population, int selectedNumberOfPlanVariants, int selectedEarliestTourStartTime, int selectedLatestTourStartTime, int selectedTypicalTourDuration){
		PlanVariantStrategy selectedPlanVariantStrategy;
		switch (planVariantStrategy) {
			case ("changeStartingTimes") -> {
				selectedPlanVariantStrategy = PlanVariantStrategy.changeStartingTimes;
			}
			default -> throw new RuntimeException("No possible PlanVariantStrategy selected. Possible strategies are: " + Arrays.toString(PlanVariantStrategy.values()));
		}
		numberOfPlanVariants = selectedNumberOfPlanVariants;
		earliestTourStartTime = selectedEarliestTourStartTime;
		latestTourStartTime = selectedLatestTourStartTime;
		typicalTourDuration = selectedTypicalTourDuration;
		createPlanVariants(selectedPlanVariantStrategy, population);
	}
	private static void createPlanVariants(PlanVariantStrategy selectedPlanVariantStrategy, Population population) {
		for (Person person: population.getPersons().values()) {
			switch (selectedPlanVariantStrategy){
				case changeStartingTimes -> {

					double initTourStart = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getEndTime().seconds();
					ArrayList<Double> timeVariants = new ArrayList<>(List.of(initTourStart));
					for (int i = person.getPlans().size(); i < numberOfPlanVariants; i++) {
						Plan newPLan = person.createCopyOfSelectedPlanAndMakeSelected();
						person.setSelectedPlan(person.getPlans().get(0));
						double variantStartTime = createStartTimeVariante(timeVariants);
						double variantEndTime = variantStartTime + typicalTourDuration;
						PopulationUtils.getFirstActivity(newPLan).setEndTime(variantStartTime);
						PopulationUtils.getLastActivity(newPLan).setStartTime(variantEndTime);
					}
				}
				default -> throw new RuntimeException("No possible PlanVariantStrategy selected");
			}
		}
	}

	private static double createStartTimeVariante(List<Double> timeVariants) {
		double vehicleStartTime = 0;
		while (vehicleStartTime == 0) {
			double possibleVehicleStartTime = rnd.nextInt(earliestTourStartTime, latestTourStartTime);
			if(checkPossibleVehicleStartTime(possibleVehicleStartTime, timeVariants))
				vehicleStartTime = possibleVehicleStartTime;
		}
		timeVariants.add(vehicleStartTime);
		return vehicleStartTime;
	}

	/** Checks if the new starting time has always a minimum 30 minutes difference to all other possible startimes.
	 * @param possibleVehicleStartTime
	 * @param timeVariants
	 * @return
	 */
	private static boolean checkPossibleVehicleStartTime(double possibleVehicleStartTime, List<Double> timeVariants) {
		for (double usedTimes: timeVariants) {
			if (Math.abs((possibleVehicleStartTime-usedTimes)) > 1800)
				return true;
		}
		return false;
	}
}
