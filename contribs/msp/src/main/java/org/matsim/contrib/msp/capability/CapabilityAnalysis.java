package org.matsim.contrib.msp.capability;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilities;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.measure.CapabilityLogsums;
import org.matsim.contrib.msp.capability.measure.CapabilityTravelTimes;
import org.matsim.contrib.msp.capability.measure.InsertionCalculator;
import org.matsim.contrib.msp.capability.measure.InsertionCost;
import org.matsim.contrib.msp.capability.prepare.CapabilityPersonFilter;
import org.matsim.contrib.msp.capability.prepare.DefaultCapabilityPersonFilter;
import org.matsim.contrib.msp.capability.result.CapabilityWriter;
import org.matsim.contrib.msp.capability.result.FacilityRecord;
import org.matsim.contrib.msp.capability.result.FundamentalPlanRecord;
import org.matsim.contrib.msp.capability.result.InsertionRecord;
import org.matsim.contrib.msp.capability.result.PersonRecord;
import org.matsim.contrib.msp.capability.structure.CapabilityTimeline;
import org.matsim.contrib.msp.capability.structure.FreePeriod;
import org.matsim.contrib.msp.capability.structure.FundamentalPlanExtractor;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Measures the capability of each person by the discretionary activities that can be inserted into their day.
 * <p>
 * The idea: strip a person's plan down to the activities they cannot choose not to perform (the
 * <i>fundamental plan</i>), then ask, for each place a discretionary activity could be performed, whether it
 * would fit into the day that is left over, and how much freedom the person would have in when to go.
 * A person who can reach a supermarket in the morning, the afternoon or the evening is better off than
 * one who can only make it there after work, and both are better off than one who cannot make it at all.
 * <p>
 * <b>The measure, in three levels.</b> Each level is the one below it, aggregated:
 * <pre>
 *   level 1, one insertion   u_potential     = u_0 + k * ln(1 + slack / t_typ) - c_insertion
 *   level 2, one facility    u_potential,max = the best insertion of that facility
 *   level 3, one person      u_potential,max = the best facility
 *                            u*              = nested logsum over the facilities
 * </pre>
 * {@code slack} is the width of the window the activity could be started in, so the logarithm grades
 * flexibility while {@code u_0} carries the main signal: being able to perform the activity at all.
 * {@code c_insertion} is what the detour costs, by default {@code VoT * t_extra}.
 * <p>
 * The two aggregation steps are deliberately different. From insertions to a facility it is a
 * <b>maximum</b>, because the insertions of one facility are the same activity at different times of
 * day - the person performs it once, and the best opportunity is the one they would take. Summing the
 * flexibility of all of them and pairing that with the cheapest detour would describe a trip nobody can
 * make. From facilities to a person it is a <b>logsum</b>, because having a choice of places genuinely
 * is worth something; see {@link CapabilityLogsums} for what its two scales mean.
 * <p>
 * <b>What is analysed.</b> Only persons whose fundamental plan starts and ends at home (people who stay
 * home all day included) and whose fixed activities all happen within 00:00-24:00, so that no activity
 * wraps past midnight. {@link CapabilityPersonFilter} narrows it down further;
 * {@link DefaultCapabilityPersonFilter} is what the run command uses. Every person that <i>is</i>
 * analysed has a row on level 3, including those who cannot reach anything - that is a result rather
 * than a missing value.
 * <p>
 * <b>Output.</b> See {@link CapabilityWriter}.
 * <p>
 * <b>Known limitations</b>, to be addressed later:
 * <ul>
 *     <li><b>No vehicle continuity.</b> Each mode is analysed on its own, so a walk row means "if this
 *     person walked the whole detour", not "if they walked while keeping their car chain intact". For a
 *     commuter who drives, the realistic pattern - drive home, park, walk to the shop round the corner -
 *     is not covered, so <b>walk understates capability for commuters and should be read as a lower
 *     bound</b>. Car is unaffected, since the car stays with the agent for the whole tour.</li>
 *     <li><b>No home-anchored round trips.</b> Insertion is en route only. That is provably no loss as
 *     long as one mode is used for the whole detour (see {@link FreePeriod}), and becomes a real gap only
 *     together with the point above.</li>
 *     <li><b>No car availability or reduced mobility.</b> Every analysed person is assumed able to use
 *     every configured mode. {@link DefaultCapabilityPersonFilter} does filter on age, but only to decide
 *     who is analysed at all - it does not restrict which modes they may use.</li>
 *     <li><b>Car at free speed only.</b> Congestion is not accounted for.</li>
 *     <li><b>Several occasions do not add up.</b> Taking the maximum over the insertions of a facility
 *     means that a person who could go in the morning <i>or</i> in the evening scores the same as one
 *     who can only go in the evening; {@code sum_slack} on level 2 is the only trace of the difference.
 *     Letting occasions count would mean a third logsum, over the insertions of a facility, with a
 *     scale larger than both others - the maximum is its limiting case.</li>
 * </ul>
 */
public final class CapabilityAnalysis {

	/**
	 * How often the progress through the population is reported, in percent.
	 */
	private static final int PROGRESS_STEP = 10;

	private static final Logger log = LogManager.getLogger(CapabilityAnalysis.class);

	private final CapabilityConfigGroup config;
	private final FundamentalPlanExtractor extractor;
	private final CapabilityPersonFilter filter;
	private final InsertionCost cost;

	private boolean writeInsertions = true;

	public CapabilityAnalysis(CapabilityConfigGroup config) {
		this(config, CapabilityPersonFilter.acceptAll());
	}

	public CapabilityAnalysis(CapabilityConfigGroup config, CapabilityPersonFilter filter) {
		this(config, filter, InsertionCost.ofTime(config.getValueOfTimePerHour()));
	}

	/**
	 * @param cost what an insertion costs the person. {@link InsertionCost#ofTime} with the value of
	 * time of the config by default.
	 */
	public CapabilityAnalysis(CapabilityConfigGroup config, CapabilityPersonFilter filter, InsertionCost cost) {
		this.config = config;
		this.extractor = new FundamentalPlanExtractor(config);
		this.filter = filter;
		this.cost = cost;
	}

	/**
	 * @param writeInsertions whether the insertion level is written. On by default; it is the level that
	 * grows with persons x modes x types x facilities x periods.
	 */
	public CapabilityAnalysis setWriteInsertions(boolean writeInsertions) {
		this.writeInsertions = writeInsertions;
		return this;
	}

	/**
	 * Runs the analysis and writes its three levels.
	 *
	 * @param scenario the scenario to analyse; needs a population, a network for the modes that are routed
	 * on it, and the vehicle types those modes take their speed from
	 * @param places where the discretionary activities can be performed
	 * @param directory where the tables are written, see {@link CapabilityWriter}
	 */
	public void run(Scenario scenario, DiscretionaryActivityFacilities places, Path directory) {

		Map<String, DiscretionaryActivityParams> paramsByType = this.config.getDiscretionaryActivityParams();
		if (paramsByType.isEmpty()) {
			// an analysis with no activity type to analyse would write a header and nothing else, which
			// looks like a result; it is a configuration mistake and has to say so. There is deliberately
			// no default type, so the message carries the parameter set a config needs instead.
			throw new IllegalStateException("No '" + DiscretionaryActivityParams.SET_NAME + "' parameter set "
				+ "is configured in the '" + CapabilityConfigGroup.GROUP_NAME + "' module, so there is no "
				+ "activity type to analyse. Declare one per type, its 'type' matching the type column of "
				+ "the places csv:\n"
				+ "\n"
				+ "  <module name=\"" + CapabilityConfigGroup.GROUP_NAME + "\">\n"
				+ "    <parameterset type=\"" + DiscretionaryActivityParams.SET_NAME + "\">\n"
				+ "      <param name=\"type\" value=\"<the type in the places csv>\" />\n"
				+ "    </parameterset>\n"
				+ "  </module>\n"
				+ "\nEvery other parameter of the set has a default; see DiscretionaryActivityParams.");
		}

		Map<String, CapabilityTravelTimes> travelTimesByMode = CapabilityTravelTimes.of(scenario, this.config);

		log.info("Analysing the capability for modes {} and activity types {}.",
			this.config.getModes(), paramsByType.keySet());

		for (String type : paramsByType.keySet()) {
			if (places.getByType(type).isEmpty()) {
				log.warn("No facility offers '{}', so no person will have any capability for it.", type);
			}
		}

		Counts counts = new Counts();
		int population = scenario.getPopulation().getPersons().size();

		log.info("Start analyzing capability. There are {} persons to be analyzed.", population);

		try (CapabilityWriter writer = new CapabilityWriter(directory, this.writeInsertions)) {

			int processed = 0;
			int nextStep = PROGRESS_STEP;

			for (Person person : scenario.getPopulation().getPersons().values()) {
				analyse(person, places, paramsByType, travelTimesByMode, counts, writer);
				nextStep = logProgress(++processed, population, nextStep);
			}
		}

		this.filter.logSummary();
		counts.log(population, directory);
	}

	/**
	 * Reports how far the analysis has got, in steps of {@link #PROGRESS_STEP} percent. A population
	 * smaller than ten persons crosses several steps with one person, so a step may be skipped - but
	 * 100% is always reached, because the last person makes the percentage exact.
	 *
	 * @param processed how many persons have been through the analysis
	 * @param population how many there are in total
	 * @param nextStep the percentage that is still to be reported
	 * @return the percentage to be reported next, to be passed back in on the following person
	 */
	private static int logProgress(int processed, int population, int nextStep) {

		int percent = (int) (100L * processed / population);

		while (percent >= nextStep && nextStep <= 100) {
			log.info("{}% complete", nextStep);
			nextStep += PROGRESS_STEP;
		}

		return nextStep;
	}

	/**
	 * Analyses one person and writes their rows on all three levels. A person that is not analysed -
	 * filtered out, or with a plan the method cannot handle - gets no row at all.
	 */
	private void analyse(Person person, DiscretionaryActivityFacilities places,
						 Map<String, DiscretionaryActivityParams> paramsByType,
						 Map<String, CapabilityTravelTimes> travelTimesByMode, Counts counts,
						 CapabilityWriter writer) {

		if (!this.filter.accept(person)) {
			counts.filtered++;
			return;
		}

		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			counts.withoutPlan++;
			return;
		}

		// step 1: shrink the plan down to what the person cannot choose not to do
		Plan fundamentalPlan = this.extractor.extract(plan);
		List<Activity> mandatory = activitiesOf(fundamentalPlan);

		// recorded before the plan is checked, so that the table also covers the persons the checks
		// below drop - that is what makes it readable against the person level
		writer.write(new FundamentalPlanRecord(person.getId(), mandatory,
			TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities), this.config));
		counts.fundamentalPlanRows++;

		if (!startsAndEndsAtHome(mandatory)) {
			counts.notHomeToHome++;
			return;
		}
		if (!FreePeriod.isWithinDay(fundamentalPlan, this.config)) {
			counts.pastMidnight++;
			return;
		}
		if (!FreePeriod.hasUsableTimes(fundamentalPlan, this.config)) {
			// the plan does not say when its fixed activities happen and the times could not be
			// reconstructed either, so any capability computed from it would be made up
			counts.withoutUsableTimes++;
			return;
		}

		// step 2: split the remaining day into the stretches a discretionary activity could go into
		List<FreePeriod> periods = FreePeriod.of(fundamentalPlan, this.config);
		if (periods.isEmpty()) {
			// back to back fixed activities that fill the whole day: an analysable plan with nowhere to
			// put anything, which is a different statement from "the plan could not be analysed"
			counts.withoutFreePeriod++;
			return;
		}

		counts.analysed++;

		// how constrained the day is, so that comparable persons can be told apart later: the fixed
		// activities (everything but home) are what pins the day down, and there is one free period more
		// than there are of them
		int fixed = (int) mandatory.stream().filter(activity -> !this.config.isHome(activity.getType())).count();

		// the blocks of this person's day, for the time axis of the drawing
		writer.writeActivities(person, CapabilityTimeline.of(fundamentalPlan, this.config));

		// step 3: score every facility, for every mode and type
		boolean anyCapability = false;

		for (Map.Entry<String, CapabilityTravelTimes> mode : travelTimesByMode.entrySet()) {
			for (DiscretionaryActivityParams params : paramsByType.values()) {

				InsertionCalculator calculator = new InsertionCalculator(mode.getValue(), params, this.cost);
				List<FacilityRecord> reachable = new ArrayList<>();

				for (DiscretionaryActivityFacility facility : places.getByType(params.getType())) {

					InsertionCalculator.Totals totals = calculator.evaluate(periods, facility);
					if (!totals.possible()) {
						// nothing is written for an activity the person cannot perform here
						continue;
					}

					// level 2
					FacilityRecord facilityRecord = new FacilityRecord(person.getId(), mode.getKey(),
						params.getType(), facility, mandatory.size(), fixed, totals);
					reachable.add(facilityRecord);
					writer.write(facilityRecord);
					counts.facilityRows++;

					// level 1: the same insertions one by one. The calculator kept them, so nothing is
					// evaluated twice.
					if (writer.writesInsertions()) {
						for (int i = 0; i < totals.byPeriod().size(); i++) {
							InsertionCalculator.Insertion insertion = totals.byPeriod().get(i);
							if (insertion.possible()) {
								writer.write(new InsertionRecord(person.getId(), mode.getKey(), params.getType(),
									facility, i, mandatory.size(), fixed, periods.get(i), insertion, params));
								counts.insertionRows++;
							}
						}
					}
				}

				// level 3: a row for every analysed person, mode and type, even an empty one
				writer.write(new PersonRecord(person.getId(), mode.getKey(), params.getType(),
					mandatory.size(), fixed, reachable, this.config));
				counts.personRows++;
				anyCapability |= !reachable.isEmpty();
			}
		}

		if (!anyCapability) {
			counts.withoutCapability++;
		}
	}

	/**
	 * @return true if the fundamental plan starts and ends at home, which includes the plan of someone
	 * who stays home all day
	 */
	private boolean startsAndEndsAtHome(List<Activity> mandatory) {
		return !mandatory.isEmpty()
			&& this.config.isHome(mandatory.get(0).getType())
			&& this.config.isHome(mandatory.get(mandatory.size() - 1).getType());
	}

	private static List<Activity> activitiesOf(Plan plan) {
		return plan.getPlanElements().stream()
			.filter(Activity.class::isInstance)
			.map(Activity.class::cast)
			.toList();
	}

	/**
	 * Why persons were left out, and how much was written, so that a surprising row count can be
	 * explained.
	 */
	private static final class Counts {
		private int analysed;
		private int filtered;
		private int withoutPlan;
		private int notHomeToHome;
		private int pastMidnight;
		private int withoutUsableTimes;
		private int withoutFreePeriod;
		private int withoutCapability;
		private long insertionRows;
		private long facilityRows;
		private long personRows;
		private long fundamentalPlanRows;

		private void log(int population, Path directory) {
			log.info("Analysed {} of {} persons.", this.analysed, population);
			if (this.filtered > 0) {
				log.info("  {} skipped by the person filter.", this.filtered);
			}
			if (this.withoutPlan > 0) {
				log.info("  {} skipped: no selected plan.", this.withoutPlan);
			}
			if (this.notHomeToHome > 0) {
				log.info("  {} skipped: the fundamental plan does not start and end at home.", this.notHomeToHome);
			}
			if (this.pastMidnight > 0) {
				log.info("  {} skipped: a fixed activity reaches past the end of the day.", this.pastMidnight);
			}
			if (this.withoutUsableTimes > 0) {
				log.warn("  {} skipped: a fixed activity does not say when it happens and the time could not be "
					+ "reconstructed. Does the plans file carry departure and travel times on its legs?",
					this.withoutUsableTimes);
			}
			if (this.withoutFreePeriod > 0) {
				log.info("  {} skipped: the fixed activities leave no free stretch of the day at all.",
					this.withoutFreePeriod);
			}
			if (this.withoutCapability > 0) {
				log.info("  {} analysed but cannot reach any facility in time, so they only have a row on the "
					+ "person level.", this.withoutCapability);
			}
			log.info("Written to {}: {} insertion rows, {} facility rows, {} person rows, {} fundamental "
				+ "plan rows.", directory, this.insertionRows, this.facilityRows, this.personRows,
				this.fundamentalPlanRows);
		}
	}
}
