package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CommandLine.Command(
		name = "close-trajectories",
		description = "Close agents' trajectories by inserting a home activity at the end of a plan if certain conditions are met.",
		mixinStandardHelpOptions = true
)
public class CloseTrajectories implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CloseTrajectories.class);

	@CommandLine.Parameters(arity = "1", description = "Path to population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Desired output path", required = true)
	private Path output;

	@CommandLine.Option(names = "--threshold", description = "Home activity is only inserted if travel time would be below threshold in minutes", defaultValue = "30")
	private int threshold;

	@CommandLine.Option(names = "--min-duration", description = "Minimum duration the last activity and home activity needs to have in minutes", defaultValue = "30")
	private int minDuration;

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input path {} does not exist.", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());
		PopulationFactory f = population.getFactory();

		int added = 0;

		for (Person person : population.getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			List<Leg> legs = TripStructureUtils.getLegs(plan);

			Optional<Activity> home = activities.stream().filter(act -> act.getType().startsWith("home")).findFirst();

			// persons without home are not considered
			if (home.isEmpty())
				continue;

			Coord homeCoord = home.get().getCoord();
			Activity lastAct = activities.get(activities.size() - 1);
			Leg lastLeg = legs.get(legs.size() - 1);

			if (lastAct.getType().startsWith("home") || lastAct.getStartTime().isUndefined())
				continue;

			// distance with beeline factor
			double dist = CoordUtils.calcEuclideanDistance(homeCoord, lastAct.getCoord()) * 1.4;

			double time = dist / 13.8889;

			// skip if home activity is too far away
			if (time > threshold * 60)
				continue;

			TimeInterpretation timeInterpretation = TimeInterpretation.create(ActivityDurationInterpretation.tryEndTimeThenDuration);
			double endTime = timeInterpretation.decideOnActivityEndTime(lastAct, lastAct.getStartTime().orElse(Double.NaN))
					.orElseThrow(() -> new IllegalStateException("Could not determine end time"));

			// don't insert when the last activity is too short
			if (endTime - lastAct.getStartTime().orElse(Double.NaN) < minDuration * 60)
				continue;

			// don't insert if home activity would be too short
			// travel time is not considered here
			if (86400 - endTime < minDuration * 60)
				continue;

			Leg leg = f.createLeg(lastLeg.getMode());
			for (Map.Entry<String, Object> e : lastLeg.getAttributes().getAsMap().entrySet()) {
				leg.getAttributes().putAttribute(e.getKey(), e.getValue());
			}

			Activity homeAct = f.createActivityFromCoord("home", homeCoord);

			plan.addLeg(leg);
			plan.addActivity(homeAct);

			added++;
		}

		log.info("Closed {} trajectories", added);

		if (output.getParent() != null && !Files.exists(output.getParent()))
			Files.createDirectories(output.getParent());

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
