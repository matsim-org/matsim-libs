package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@CommandLine.Command(
		name = "split-activity-types-duration",
		description = "Split activity types by duration",
		mixinStandardHelpOptions = true,
		showDefaultValues = true
)
public class SplitActivityTypesDuration implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output population", required = true)
	private Path output;

	@CommandLine.Option(names = {"--activity-bin-size", "--abs"}, description = "Activity types are extended so that they belong to a typical duration. This parameter influences the number of typical duration classes.")
	private int activityBinSize = 600;

	@CommandLine.Option(names = {"--max-typical-duration", "--mtd"}, description = "Max duration of activities for which a typical activity duration type is created in seconds.")
	private int maxTypicalDuration = 86400;

	@CommandLine.Option(names = {"--end-time-to-duration"}, description = "Remove the end time and encode as duration for activities shorter than this value.")
	private int endTimeToDuration = 1800;

	@CommandLine.Option(names = "--stage-activity-handling", description = "Define how stage activities are handled", required = false, defaultValue = "ExcludeStageActivities")
	private TripStructureUtils.StageActivityHandling stageActivityHandling = TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

	@CommandLine.Option(names = "--subpopulation", description = "Only apply to certain subpopulation")
	private String subpopulation;

	public static void main(String[] args) {
		new SplitActivityTypesDuration().execute(args);
	}

	/**
	 * Default Constructor needed for cli usage.
	 */
	public SplitActivityTypesDuration() {
	}

	/**
	 * Create a new instance of this algorithm without cli usage.
	 */
	public SplitActivityTypesDuration(int activityBinSize, int maxTypicalDuration, int endTimeToDuration) {
		this.activityBinSize = activityBinSize;
		this.maxTypicalDuration = maxTypicalDuration;
		this.endTimeToDuration = endTimeToDuration;
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		if (subpopulation != null && !Objects.equals(PopulationUtils.getSubpopulation(person), subpopulation))
			return;

		for (Plan plan : person.getPlans()) {

			List<Activity> activities = TripStructureUtils.getActivities(plan, stageActivityHandling);

			for (Activity act : activities) {

				double duration;
				if (act.getMaximumDuration().isDefined())
					duration = act.getMaximumDuration().seconds();
				else
					duration = act.getEndTime().orElse(maxTypicalDuration) - act.getStartTime().orElse(0);

				String newType = String.format("%s_%d", act.getType(), roundDuration(duration));
				act.setType(newType);

				if (duration <= endTimeToDuration && act.getEndTime().isDefined()) {
					act.setEndTimeUndefined();
					act.setMaximumDuration(duration);
				}
			}

			mergeOvernightActivities(activities);
		}
	}

	/**
	 * Round duration according to bin size.
	 */
	private int roundDuration(double duration) {

		final int maxCategories = maxTypicalDuration / activityBinSize;

		int durationCategoryNr = (int) Math.round((duration / activityBinSize));

		if (durationCategoryNr <= 0) {
			durationCategoryNr = 1;
		}

		if (durationCategoryNr >= maxCategories) {
			durationCategoryNr = maxCategories;
		}

		return durationCategoryNr * activityBinSize;
	}

	private void mergeOvernightActivities(List<Activity> plan) {

		if (plan.size() > 1) {
			Activity firstActivity = plan.get(0);
			Activity lastActivity = plan.get(plan.size() - 1);

			String firstBaseActivity = firstActivity.getType().split("_")[0];
			String lastBaseActivity = lastActivity.getType().split("_")[0];

			if (firstBaseActivity.equals(lastBaseActivity)) {
				double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);

				int merged = roundDuration(mergedDuration);

				firstActivity.setType(String.format("%s_%d", firstBaseActivity, merged));
				lastActivity.setType(String.format("%s_%d", lastBaseActivity, merged));
			}
		}  // skipping plans with just one activity
	}
}
