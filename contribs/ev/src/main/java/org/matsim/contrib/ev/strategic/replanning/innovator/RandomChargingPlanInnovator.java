package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.timing.TimeInterpretation;

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

	private final double activityInclusionProbability;
	private final double legInclusionProbability;

	public RandomChargingPlanInnovator(ChargerProvider chargerProvider, ChargingSlotFinder candidateFinder,
			TimeInterpretation timeInterpretation, StrategicChargingConfigGroup config, Parameters parameters) {
		this.chargerProvider = chargerProvider;
		this.timeInterpretation = timeInterpretation;
		this.minimumActivityChargingDuration = config.getMinimumActivityChargingDuration();
		this.maximumActivityChargingDuration = config.getMaximumActivityChargingDuration();
		this.minimumEnrouteDriveTime = config.getMinimumEnrouteDriveTime();
		this.minimumEnrouteChargingDuration = config.getMinimumEnrouteChargingDuration();
		this.maximumEnrouteChargingDuration = config.getMaximumEnrouteChargingDuration();
		this.activityInclusionProbability = parameters.getActivityInclusionProbability();
		this.legInclusionProbability = parameters.getLegInclusionProbability();
		this.candidateFinder = candidateFinder;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
		// set up the helper
		InnovationHelper helper = InnovationHelper.build(plan, timeInterpretation, candidateFinder);

		// first, obtain activity-based slots
		List<ActivityBasedCandidate> activityBased = helper.findActivityBased();

		// remove slots that are too short
		helper.filterByDuration(activityBased, minimumActivityChargingDuration, maximumActivityChargingDuration);

		// select activity-based slots
		for (ActivityBasedCandidate candidate : activityBased) {
			if (random.nextDouble() <= activityInclusionProbability) {
				// find chargers
				List<ChargerSpecification> chargers = new LinkedList<>(
						chargerProvider.findChargers(plan.getPerson(), plan,
								new ChargerRequest(candidate.startActivity(), candidate.endActivity())));

				if (chargers.size() > 0) {
					ChargerSpecification charger = chargers.get(random.nextInt(chargers.size()));
					helper.push(candidate, charger);
				}
			}
		}

		// second, obtain leg-based slots
		List<LegBasedCandidate> legBased = helper.findLegBased();

		// remove if too short
		helper.filterByDriveTime(legBased, minimumEnrouteDriveTime);

		// select leg-based slots
		for (LegBasedCandidate candidate : legBased) {
			if (random.nextDouble() <= legInclusionProbability) {
				double duration = minimumEnrouteChargingDuration;
				duration += (maximumEnrouteChargingDuration - minimumEnrouteChargingDuration) * random.nextDouble();

				// find chargers
				List<ChargerSpecification> chargers = new LinkedList<>(
						chargerProvider.findChargers(plan.getPerson(), plan,
								new ChargerRequest(candidate.leg(), duration)));

				if (chargers.size() > 0) {
					ChargerSpecification charger = chargers.get(random.nextInt(chargers.size()));
					helper.push(candidate, duration, charger);
				}
			}
		}

		return helper.getChargingPlan();
	}

	static public class Parameters extends ReflectiveConfigGroup implements ChargingInnovationParameters {
		static public final String SET_NAME = "innovation:random";

		public Parameters() {
			super(SET_NAME);
		}

		@Parameter
		private double activityInclusionProbability = 0.5;

		@Parameter
		private double legInclusionProbability = 0.5;

		public double getActivityInclusionProbability() {
			return activityInclusionProbability;
		}

		public void setActivityInclusionProbability(double val) {
			activityInclusionProbability = val;
		}

		public double getLegInclusionProbability() {
			return legInclusionProbability;
		}

		public void setLegInclusionProbability(double val) {
			legInclusionProbability = val;
		}
	}
}
