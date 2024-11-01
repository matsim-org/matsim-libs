package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@CommandLine.Command(name = "generate-plan-variants-for-freight", description = "Generates variants of plans for a population of freight agents", showDefaultValues = true)

public class CreateDifferentPlansForFreightPopulation implements MATSimAppCommand {

	private enum PlanVariantStrategy {changeStartingTimes, activityOrderVariation}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the population")
	private Path populationPath;
	@CommandLine.Option(names = "--outputPopulationPath", description = "Path to the outputPopulation", required = true)
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
	private static final Random rnd = new Random(seed);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateDifferentPlansForFreightPopulation()).execute(args));
	}

	@Override
	public Integer call() {

		PlanVariantStrategy selectedPlanVariantStrategy = PlanVariantStrategy.activityOrderVariation;

		Population population = PopulationUtils.readPopulation(populationPath.toString());
		createPlanVariants(selectedPlanVariantStrategy, population);
		PopulationUtils.writePopulation(population, outputPopulationPath.toString());
		return null;
	}

	/**
	 * Creates alternative plans (n = selectedNumberOfPlanVariants) by changing the start time (and end time) of the tour.
	 *
	 * @param population
	 * @param selectedNumberOfPlanVariants
	 * @param selectedEarliestTourStartTime
	 * @param selectedLatestTourStartTime
	 * @param selectedTypicalTourDuration
	 */
	public static void createMorePlansWithDifferentStartTimes(Population population, int selectedNumberOfPlanVariants,
															  int selectedEarliestTourStartTime, int selectedLatestTourStartTime,
															  int selectedTypicalTourDuration) {
		PlanVariantStrategy selectedPlanVariantStrategy = PlanVariantStrategy.changeStartingTimes;
		numberOfPlanVariants = selectedNumberOfPlanVariants;
		earliestTourStartTime = selectedEarliestTourStartTime;
		latestTourStartTime = selectedLatestTourStartTime;
		typicalTourDuration = selectedTypicalTourDuration;
		createPlanVariants(selectedPlanVariantStrategy, population);
	}

	/**
	 * Creates alternative plans (n = selectedNumberOfPlanVariants) by changing the order of the activities.
	 *
	 * @param population
	 * @param selectedNumberOfPlanVariants
	 */
	public static void createMorePlansWithDifferentActivityOrder(Population population, int selectedNumberOfPlanVariants) {
		PlanVariantStrategy selectedPlanVariantStrategy = PlanVariantStrategy.activityOrderVariation;
		numberOfPlanVariants = selectedNumberOfPlanVariants;
		createPlanVariants(selectedPlanVariantStrategy, population);
	}

	private static void createPlanVariants(PlanVariantStrategy selectedPlanVariantStrategy, Population population) {
		for (Person person : population.getPersons().values()) {
			switch (selectedPlanVariantStrategy) {
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
				case activityOrderVariation -> {

					List<Integer> activityIndexList = new ArrayList<>();
					int count = 0;
					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
						if (planElement instanceof Activity activity) {
							if (activity.getType().equals("service")) {
								activityIndexList.add(count);
							}
						}
						count++;
					}
					for (int i = person.getPlans().size(); i < numberOfPlanVariants; i++) {
						if (activityIndexList.size() < 2)
							continue;
						List<Integer> activityNewIndexList = new ArrayList<Integer>(activityIndexList);
						Collections.shuffle(activityNewIndexList, rnd);
						List<Integer> alreadySwapedActivity = new ArrayList<>();
						Plan newPLan = person.createCopyOfSelectedPlanAndMakeSelected();
						person.setSelectedPlan(person.getPlans().get(0));
						for (int j = 0; j < activityIndexList.size(); j++) {
							if (alreadySwapedActivity.contains(activityIndexList.get(j)))
								continue;
							Collections.swap(newPLan.getPlanElements(), activityIndexList.get(j), activityNewIndexList.get(j));
							alreadySwapedActivity.add(activityNewIndexList.get(j));
						}
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
			if (checkPossibleVehicleStartTime(possibleVehicleStartTime, timeVariants))
				vehicleStartTime = possibleVehicleStartTime;
		}
		timeVariants.add(vehicleStartTime);
		return vehicleStartTime;
	}

	/**
	 * Checks if the new starting time has always a minimum 30 minutes difference to all other possible startimes.
	 *
	 * @param possibleVehicleStartTime
	 * @param timeVariants
	 * @return
	 */
	private static boolean checkPossibleVehicleStartTime(double possibleVehicleStartTime, List<Double> timeVariants) {
		for (double usedTimes : timeVariants) {
			if (Math.abs((possibleVehicleStartTime - usedTimes)) > 1800)
				return true;
		}
		return false;
	}
}
