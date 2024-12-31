package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlanActivity;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This is the current default implementation of the charging plan innovator. It
 * traverses an agent's regular plan and finds all potential slots where the
 * agent may charge along a leg or during a sequence of activities. For each of
 * the viable slots, a charging activity is created with probability 50%. For
 * the generated slots, the ChargingProvider is then used to find a viable
 * charger. Those charging plans are created from scratch, i.e., there is no
 * dependence on the existing charging plan.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class RandomChargingPlanInnovator implements ChargingPlanInnovator {
	private final ChargerProvider chargerProvider;
	private final ChargingSlotFinder candidateFinder;
	private final TimeInterpretation timeInterpretation;

	private final Random random;

	private final double minimumActivityChargingDuration;
	private final double maximumActivityChargingDuration;

	private final double minimumEnrouteDriveTime;
	private final double minimumEnrouteChargingDuration;
	private final double maximumEnrouteChargingDuration;

	public RandomChargingPlanInnovator(ChargerProvider chargerProvider, ChargingSlotFinder candidateFinder,
			TimeInterpretation timeInterpretation, StrategicChargingConfigGroup config) {
		this.chargerProvider = chargerProvider;
		this.timeInterpretation = timeInterpretation;
		this.minimumActivityChargingDuration = config.minimumActivityChargingDuration;
		this.maximumActivityChargingDuration = config.maximumActivityChargingDuration;
		this.minimumEnrouteDriveTime = config.minimumEnrouteDriveTime;
		this.minimumEnrouteChargingDuration = config.minimumEnrouteChargingDuration;
		this.maximumEnrouteChargingDuration = config.maximumEnrouteChargingDuration;
		this.candidateFinder = candidateFinder;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
		ChargingPlan chargingPlan = new ChargingPlan();

		// set up some lookups
		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
				StageActivityHandling.ExcludeStageActivities);
		Map<Activity, Double> startTimes = new HashMap<>();
		Map<Activity, Double> endTimes = new HashMap<>();

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		for (PlanElement element : plan.getPlanElements()) {
			double startTime = timeTracker.getTime().seconds();
			timeTracker.addElement(element);

			if (element instanceof Activity activity) {
				if (!TripStructureUtils.isStageActivityType(activity.getType())) {
					if (activity == activities.get(0)) {
						startTime = Double.NEGATIVE_INFINITY;
					}

					final double endTime;
					if (activity == activities.get(activities.size() - 1)) {
						endTime = Double.POSITIVE_INFINITY;
					} else {
						endTime = timeTracker.getTime().orElse(Double.POSITIVE_INFINITY);
					}

					startTimes.put(activity, startTime);
					endTimes.put(activity, endTime);
				}
			}
		}

		// first, select activity-based slots
		List<ActivityBasedCandidate> activityBased = candidateFinder.findActivityBased(person, plan);

		// remove slots that are too short
		activityBased.removeIf(candidate -> {
			double duration = endTimes.get(candidate.endActivity()) - startTimes.get(candidate.startActivity());
			return duration < minimumActivityChargingDuration || duration > maximumActivityChargingDuration;
		});

		// track which ones are selected
		List<ActivityBasedCandidate> selectedActivityBased = new LinkedList<>();

		// construct activities
		for (ActivityBasedCandidate candidate : activityBased) {
			if (random.nextBoolean()) {
				// find chargers
				List<ChargerSpecification> chargers = new LinkedList<>(
						chargerProvider.findChargers(plan.getPerson(), plan,
								new ChargerRequest(candidate.startActivity(), candidate.endActivity())));

				if (chargers.size() > 0) {
					ChargerSpecification charger = chargers.get(random.nextInt(chargers.size()));

					int startActivityIndex = activities.indexOf(candidate.startActivity());
					int endActivityIndex = activities.indexOf(candidate.endActivity());

					chargingPlan.addChargingActivity(new ChargingPlanActivity(startActivityIndex, endActivityIndex,
							charger.getId()));

					selectedActivityBased.add(candidate);
				}
			}
		}

		// second, find leg-based slots
		List<LegBasedCandidate> legBased = candidateFinder.findLegBased(person, plan);

		// remove if too short
		legBased.removeIf(candidate -> {
			return candidate.leg().getTravelTime().seconds() < minimumEnrouteDriveTime;
		});

		// reduce slots that are incompatible with the selected activity-based ones
		candidateFinder.reduceLegBased(legBased, selectedActivityBased, plan.getPlanElements());

		// construct legs
		for (LegBasedCandidate candidate : legBased) {
			if (random.nextBoolean()) {
				double duration = minimumEnrouteChargingDuration;
				duration += (maximumEnrouteChargingDuration - minimumEnrouteChargingDuration) * random.nextDouble();

				// find chargers
				List<ChargerSpecification> chargers = new LinkedList<>(
						chargerProvider.findChargers(plan.getPerson(), plan,
								new ChargerRequest(candidate.leg(), duration)));

				if (chargers.size() > 0) {
					ChargerSpecification charger = chargers.get(random.nextInt(chargers.size()));
					int followingActivityIndex = activities.indexOf(candidate.followingActivity());

					chargingPlan.addChargingActivity(new ChargingPlanActivity(followingActivityIndex,
							duration,
							charger.getId()));
				}
			}
		}

		return chargingPlan;
	}
}
