package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
		name = "split-activity-types-duration",
		description = "Split activity types by duration",
		mixinStandardHelpOptions = true,
		showDefaultValues = true
)
public class SplitActivityTypesDuration implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Output population", required = false)
	private Path output;

	@CommandLine.Option(names = {"--activity-bin-size", "--abs"}, description = "Activity types are extended so that they belong to a typical duration. This parameter influences the number of typical duration classes.")
	private int activityBinSize = 600;

	@CommandLine.Option(names = {"--max-typical-duration", "--mtd"}, description = "Max duration of activities for which a typical activity duration type is created in seconds.")
	private int maxTypicalDuration = 86400;

	@CommandLine.Option(names = {"--remove-end-time"}, description = "Remove the end time and encode as duration for activities shorter shorter than this value.")
	private int removeEndTime = 1800;

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
	public SplitActivityTypesDuration(int activityBinSize, int maxTypicalDuration, int removeEndTime) {
		this.activityBinSize = activityBinSize;
		this.maxTypicalDuration = maxTypicalDuration;
		this.removeEndTime = removeEndTime;
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

		for (Plan plan : person.getPlans()) {
			for (PlanElement el : plan.getPlanElements()) {

				if (!(el instanceof Activity act))
					continue;

				double duration;
				if (act.getMaximumDuration().isDefined())
					duration = act.getMaximumDuration().seconds();
				else
					duration = act.getEndTime().orElse(maxTypicalDuration) - act.getStartTime().orElse(0);

				String newType = String.format("%s_%d", act.getType(), roundDuration(duration));
				act.setType(newType);

				if (duration <= removeEndTime && act.getEndTime().isDefined()) {
					act.setEndTimeUndefined();
					act.setMaximumDuration(duration);
				}
			}

			mergeOvernightActivities(plan);
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

	private void mergeOvernightActivities(Plan plan) {

		if (plan.getPlanElements().size() > 1) {
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

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
