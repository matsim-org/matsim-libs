package org.matsim.contrib.ev.strategic.replanning.innovator;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservability;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.ChargerSelector;
import org.matsim.contrib.ev.strategic.reservation.StrategicChargingReservationEngine;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.ActivityBasedCandidate;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder.LegBasedCandidate;
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
	private final ChargerSelector.Factory selectorFactory;
	private final ChargerReservability chargerReservability;

	private final Random random;

	private final double minimumActivityChargingDuration;
	private final double maximumActivityChargingDuration;

	private final double minimumEnrouteDriveTime;
	private final double minimumEnrouteChargingDuration;
	private final double maximumEnrouteChargingDuration;

	private final double activityInclusionProbability;
	private final double legInclusionProbability;
	private final double reservationProbability;

	public RandomChargingPlanInnovator(ChargerProvider chargerProvider, ChargingSlotFinder candidateFinder,
			TimeInterpretation timeInterpretation, StrategicChargingConfigGroup config, Parameters parameters,
			ChargerSelector.Factory selectorFactory, ChargerReservability chargerReservability) {
		this.chargerProvider = chargerProvider;
		this.timeInterpretation = timeInterpretation;

		this.minimumActivityChargingDuration = config.getMinimumActivityChargingDuration();
		this.maximumActivityChargingDuration = config.getMaximumActivityChargingDuration();
		this.minimumEnrouteDriveTime = config.getMinimumEnrouteDriveTime();
		this.minimumEnrouteChargingDuration = config.getMinimumEnrouteChargingDuration();
		this.maximumEnrouteChargingDuration = config.getMaximumEnrouteChargingDuration();
		this.activityInclusionProbability = parameters.getActivityInclusionProbability();
		this.legInclusionProbability = parameters.getLegInclusionProbability();
		this.reservationProbability = parameters.getReservationProbability();
		this.candidateFinder = candidateFinder;
		this.random = MatsimRandom.getLocalInstance();
		this.selectorFactory = selectorFactory;
		this.chargerReservability = chargerReservability;
	}

	@Override
	public ChargingPlan createChargingPlan(Person person, Plan plan, ChargingPlans chargingPlans) {
		// set up the helper
		InnovationHelper helper = InnovationHelper.build(plan, timeInterpretation, candidateFinder,
				chargerReservability);
		ChargerSelector chargerSelector = selectorFactory.create(person, plan, random, helper);

		// first, obtain activity-based slots
		List<ActivityBasedCandidate> activityBased = helper.findActivityBased();

		// remove slots that are too short
		helper.filterByDuration(activityBased, minimumActivityChargingDuration, maximumActivityChargingDuration);

		// select activity-based slots
		for (ActivityBasedCandidate candidate : activityBased) {
			if (random.nextDouble() <= activityInclusionProbability) {
				boolean withReservation = StrategicChargingReservationEngine.getReservationSlack(person) != null
						&& random.nextDouble() < reservationProbability;

				// find chargers
				ChargerSpecification charger = helper.selectCharger(candidate, chargerProvider, chargerSelector,
						withReservation);

				// integrate into plan
				helper.push(candidate, charger, withReservation);
			}
		}

		// second, obtain leg-based slots
		List<LegBasedCandidate> legBased = helper.findLegBased();

		// remove if too short
		helper.filterByDriveTime(legBased, minimumEnrouteDriveTime);

		// select leg-based slots
		for (LegBasedCandidate candidate : legBased) {
			if (random.nextDouble() <= legInclusionProbability) {
				// define duration
				double duration = minimumEnrouteChargingDuration;
				duration += (maximumEnrouteChargingDuration - minimumEnrouteChargingDuration) * random.nextDouble();

				boolean withReservation = StrategicChargingReservationEngine.getReservationSlack(person) != null
						&& random.nextDouble() < reservationProbability;

				// find charger
				ChargerSpecification charger = helper.selectCharger(candidate, duration, chargerProvider,
						chargerSelector, withReservation);

				// integrate into plan
				helper.push(candidate, duration, charger, withReservation);
			}
		}

		return helper.getChargingPlan();
	}

	static public class Parameters extends ChargingInnovationParameters {
		static public final String SET_NAME = "innovation:random";

		public Parameters() {
			super(SET_NAME);
		}

		@Parameter
		private double activityInclusionProbability = 0.5;

		@Parameter
		private double legInclusionProbability = 0.5;

		@Parameter
		private double reservationProbability = 0.0;

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

		public double getReservationProbability() {
			return reservationProbability;
		}

		public void setReservationProbability(double val) {
			reservationProbability = val;
		}
	}
}
