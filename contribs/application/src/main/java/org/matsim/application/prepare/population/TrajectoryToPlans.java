package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;


/**
 * Creates the population from the original input data.
 *
 * "activity split by duration" refers to our convention to have activity types work_3600, work_7200 etc.
 *
 * "trajectory-to-plans"  seems a bit mis-named because everything are plans.  What it seems to do is to remove routes and modes.
 *
 * @author rakow
 */
@CommandLine.Command(
        name = "trajectory-to-plans",
        description = "Create population including down-sampling, and activity split by duration",
        showDefaultValues = true
)
public class TrajectoryToPlans implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(TrajectoryToPlans.class);

    @CommandLine.Option(names = "--name", description = "Base name of the output files", required = true)
    private String name;

    @CommandLine.Option(names = "--sample-size", description = "Sample size of the given input data in (0, 1]", required = true)
    private double sampleSize;

	@Deprecated
    @CommandLine.Option(names = "--samples", description = "Target sample sizes relative to a 100% population (range: (0,1]). These values are absolute shares, NOT relative to --sample-size. Example: with --sample-size=0.25 and --samples=0.1 0.05, the tool will create a 10% and 5% sample of the full population. Deprecated: Use the separate down-sampling instead.", arity = "1..*")
    private List<Double> samples;

    @CommandLine.Option(names = "--population", description = "Input original population file", required = true)
    private Path population;

    @CommandLine.Mixin
    private final CrsOptions crs = new CrsOptions();

    @CommandLine.Option(names = "--attributes", description = "Input person attributes file")
    private Path attributes;

    @CommandLine.Option(names = {"--activity-bin-size", "--abs"}, description = "Activity types are extended so that they belong to a typical duration. This parameter influences the number of typical duration classes. The default is 600s")
    private int activityBinSize = 600;

    @CommandLine.Option(names = {"--max-typical-duration", "--mtd"}, description = "Max duration of activities for which a typical activity duration type is created in seconds. Default is 86400s (24h). if set to 0, activities are not split.")
    private Integer maxTypicalDuration = null;

    @CommandLine.Option(names = "--output", description = "Output folder", defaultValue = "scenarios/input")
    private Path output;

    public TrajectoryToPlans() {
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new TrajectoryToPlans()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        Config config = ConfigUtils.createConfig();

        if (attributes != null) {
            log.info("Using attributes files {}", attributes);
            config.plans().setInputPersonAttributeFile(attributes.toString());
        }

        config.plans().setInputFile(population.toString());

        config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Files.createDirectories(output);

        // Clear wrong coordinate system
        scenario.getPopulation().getAttributes().clear();

		scenario.getPopulation().getPersons().forEach((k, v) -> {

			if (PopulationUtils.getSubpopulation(v) == null)
				PopulationUtils.putSubpopulation(v, "person");
		});
		// (if a <person/> does not yet have a subpopulation attribute, tag it as a "person".  kai, feb'2024)

        if (crs.getTargetCRS() != null) {
            ProjectionUtils.putCRS(scenario.getPopulation(), crs.getTargetCRS());
            log.info("Setting crs to: {}", ProjectionUtils.getCRS(scenario.getPopulation()));
        }
        // (if set by command line)

        if (crs.getInputCRS() != null) {
            ProjectionUtils.putCRS(scenario.getPopulation(), crs.getInputCRS());
            log.info("Setting crs to: {}", ProjectionUtils.getCRS(scenario.getPopulation()));
        }
        // (if set by command line, this will overwrite the above targetCRS.  How is this to be interpreted?  kai, feb'2024)

        // Persist the population scale so follow-up down-sampling updates it consistently.
        ScenarioUtils.putScale(scenario.getPopulation(), sampleSize);

        if ( maxTypicalDuration==null ){
            throw new RuntimeException( "maxTypicalDuration needs to be set explicitly.  The old default was 86400, which would run splitActivityTypesBasedOnDuration, " +
                                                "which, however, is deprecated.  Normally, it should be set to 0, which means that splitActivityTypesBasedOnDuration is skipped.  Then, " +
                                                "use separate class SplitActivityTypesDuration.  kai (with input from Simon M.), nov'25" );
        }

        if (maxTypicalDuration > 0) {
            splitActivityTypesBasedOnDuration(scenario.getPopulation());
        }

        PopulationUtils.writePopulation(scenario.getPopulation(),
                output.resolve(String.format("%s-%dpct.plans.xml.gz", name, Math.round(sampleSize * 100))).toString());

        if (samples == null) {
            log.info("No sub samples requested. Done.");
            return 0;
        }

		if (sampleSize <= 0 || sampleSize > 1)
			throw new IllegalArgumentException("--sample-size must be in (0,1]");

		for (double s : samples) {
			if (s <= 0 || s > 1)
				throw new IllegalArgumentException("All --samples must be in (0,1]");
			if (s > sampleSize)
				throw new IllegalArgumentException("Target sample " + s + " is larger than input sample size " + sampleSize);
		}

        samples.sort(Comparator.comparingDouble(Double::doubleValue).reversed());

        for (Double sample : samples) {

            // down-sample previous samples
            PopulationUtils.sampleDown(scenario.getPopulation(), sample / sampleSize);
            sampleSize = sample;

            log.info("Creating {} sample", sampleSize);
            PopulationUtils.writePopulation(scenario.getPopulation(),
                    output.resolve(String.format("%s-%dpct.plans.xml.gz", name, Math.round(sampleSize * 100))).toString());
        }

        return 0;
    }

    /**
     * @deprecated  use {@link org.matsim.application.prepare.population.SplitActivityTypesDuration} instead. <br>
     * Split activities into typical durations to improve value of travel time savings calculation.
     */
    @Deprecated
    private void splitActivityTypesBasedOnDuration(Population population) {


        // Calculate activity durations for the next step
        for (Person p : population.getPersons().values()) {

            for (Plan plan : p.getPlans()) {
                for (PlanElement el : plan.getPlanElements()) {

                    if (!(el instanceof Activity))
                        continue;

                    Activity act = (Activity) el;
                    double duration = act.getEndTime().orElse(24 * 3600) - act.getStartTime().orElse(0);
                    String newType = String.format("%s_%d", act.getType(), roundDuration(duration));
                    act.setType(newType);

                }

                mergeOvernightActivities(plan);
            }
        }
    }

	@Deprecated
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

	@Deprecated
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
