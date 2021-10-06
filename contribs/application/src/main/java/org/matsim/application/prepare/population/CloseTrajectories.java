package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;

import org.matsim.core.utils.misc.OptionalTime;
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

	@CommandLine.Option(names = "--threshold", description = "Home activity is only inserted if travel time would be below threshold in minutes.", defaultValue = "30")
	private int threshold;

	@CommandLine.Option(names = "--min-duration", description = "Minimum duration the last activity and home activity needs to have in minutes. " +
			"Can be 0 to disable or if this information is not available.", defaultValue = "30")
	private int minDuration;

	@CommandLine.Option(names = "--act-duration", description = "If the last activity has no duration, it will be assigned one.", defaultValue = "0")
	private double actDuration;

	@CommandLine.Option(names = "--end-time", description = "Set end-time for the inserted home activities." +
			"Can be 0 to leave it undefined.", defaultValue = "0")
	private double endTime;

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input path {} does not exist.", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());
		PopulationFactory f = population.getFactory();
		TimeInterpretation timeInterpretation = TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration, PlansConfigGroup.TripDurationHandling.ignoreDelays);

		int added = 0;

		for (Person person : population.getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			List<Leg> legs = TripStructureUtils.getLegs(plan);

			Optional<Activity> home = activities.stream().filter(act -> act.getType().startsWith("home")).findFirst();

			// persons without home or legs are not considered
			if (home.isEmpty() || legs.isEmpty())
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

			if (minDuration > 0) {

				OptionalTime optionalTime = timeInterpretation.decideOnActivityEndTime(lastAct, lastAct.getStartTime().orElse(Double.NaN));

				if (optionalTime.isUndefined())
					continue;

				double endTime = optionalTime.orElse(Double.NaN);

				// don't insert when the last activity is too short
				if (endTime - lastAct.getStartTime().orElse(Double.NaN) < minDuration * 60)
					continue;

				// don't insert if home activity would be too short
				// travel time is not considered here
				if (86400 - endTime < minDuration * 60)
					continue;
			}

			Leg leg = f.createLeg(lastLeg.getMode());
			for (Map.Entry<String, Object> e : lastLeg.getAttributes().getAsMap().entrySet()) {
				leg.getAttributes().putAttribute(e.getKey(), e.getValue());
			}

			Activity homeAct = f.createActivityFromCoord("home", homeCoord);

			if (lastAct.getEndTime().isUndefined() && actDuration > 0) {
				lastAct.setEndTime(lastAct.getStartTime().seconds() + actDuration);
			}

			//if (lastAct.getStartTime().isDefined() && actDuration > 0) {
			//	homeAct.setStartTime(lastAct.getStartTime().seconds() + time * 1.3 + actDuration);
			//}

			if (endTime > 0)
				homeAct.setEndTime(endTime);

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
