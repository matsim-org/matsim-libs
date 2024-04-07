package org.matsim.application.prepare.population;

import org.apache.commons.lang3.math.NumberUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

	@CommandLine.Option(names = "--exclude", description = "Activity types that won't be split", split = ",", defaultValue = "")
	private Set<String> exclude = new HashSet<>();


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

	/**
	 * Set activity types to be excluded from splitting.
	 */
	public void setExclude(Set<String> exclude) {
		this.exclude = exclude;
	}

	public static void main(String[] args) {
		new SplitActivityTypesDuration().execute(args);
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

				if (exclude.contains(act.getType()))
					continue;

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

		// skipping plans with just one activity
		if (plan.size() <= 1)
			return;

		Activity firstActivity = plan.get(0);
		Activity lastActivity = plan.get(plan.size() - 1);

		// skip non merge-able
		if (!firstActivity.getType().contains("_") || !lastActivity.getType().contains("_"))
			return;

		int idxFirst = firstActivity.getType().lastIndexOf("_");
		int idxLast = lastActivity.getType().lastIndexOf("_");

		String firstBaseActivity = firstActivity.getType().substring(0, idxFirst);
		String lastBaseActivity = lastActivity.getType().substring(0, idxLast);

		String firstDuration = firstActivity.getType().substring(idxFirst + 1);
		String lastDuration = lastActivity.getType().substring(idxLast + 1);

		if (!NumberUtils.isParsable(firstDuration) || !NumberUtils.isParsable(lastDuration))
			return;

		if (firstBaseActivity.equals(lastBaseActivity)) {
			double mergedDuration = Double.parseDouble(firstDuration) + Double.parseDouble(lastDuration);

			int merged = roundDuration(mergedDuration);

			firstActivity.setType(String.format("%s_%d", firstBaseActivity, merged));
			lastActivity.setType(String.format("%s_%d", lastBaseActivity, merged));
		}
	}
}
