package org.matsim.contrib.msp.capability.prepare;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.capability.CapabilityAnalysis;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.structure.FreePeriod;
import org.matsim.contrib.msp.capability.structure.FundamentalPlanExtractor;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The person filter the capability analysis uses unless told otherwise: the residents of the study area
 * whose day is a home based day within the analysed day.
 * <p>
 * A person is kept if
 * <ol>
 *     <li><b>they are a person.</b> A population that carries subpopulations usually also carries the
 *     freight and commercial agents of the scenario, and those have no capability in the sense of this
 *     analysis. Only the subpopulations given to the constructor are kept, {@code person} by default.
 *     A population without subpopulations passes this criterion as a whole.</li>
 *     <li><b>they are an adult</b>, i.e. at least {@link CapabilityConfigGroup#getMinAge()} years old, read from the
 *     {@code age} person attribute. The measure asks which discretionary activities a person could choose
 *     to perform, and a child's day is not theirs to rearrange. A person whose age is unknown is dropped
 *     for the same reason the home location works that way: there is no telling whether they qualify.
 *     A scenario that carries no ages at all can switch the criterion off with a minimum age of 0.</li>
 *     <li><b>they live in the study area</b>, given as a shape file. Home is the {@code home_x} /
 *     {@code home_y} person attribute if the population carries it (see {@code ExtractHomeCoordinates}
 *     of {@code contribs/application}), otherwise the coordinate of the first home activity of the
 *     selected plan. A person whose home cannot be located either way is dropped, since there is no
 *     telling whether they belong to the study area. Without a shape file the criterion is skipped.</li>
 *     <li><b>their day starts and ends at home</b> and stays within the analysed day, i.e. the last
 *     non-home activity is left no later than 24:00:00. Both are requirements of the method rather than
 *     choices about the population - a day that wraps past midnight cannot be analysed without wrap
 *     around, and one that does not start at home has no anchor - and {@link CapabilityAnalysis} checks
 *     them on the fundamental plan again. Checking them here, on the full plan, keeps them out of the
 *     capability numbers <i>and</i> out of the denominator: a person whose home based day only comes
 *     about because a discretionary activity was stripped off is not a resident the analysis can speak for.</li>
 * </ol>
 * The filter counts why persons were dropped and reports it through {@link #logSummary()}, which
 * {@link CapabilityAnalysis} calls at the end of its run.
 *
 * @see CapabilityPersonFilter
 */
public final class DefaultCapabilityPersonFilter implements CapabilityPersonFilter {

	/**
	 * Person attributes holding the home coordinate, written by {@code ExtractHomeCoordinates} of
	 * {@code contribs/application}.
	 */
	public static final String HOME_X = "home_x";
	public static final String HOME_Y = "home_y";

	/**
	 * The subpopulation of the persons the analysis is about. Freight and commercial agents live in
	 * subpopulations of their own and are therefore dropped.
	 */
	public static final String DEFAULT_SUBPOPULATION = "person";

	/**
	 * Person attribute holding the age in years, as {@code PersonUtils} writes it.
	 */
	public static final String AGE = "age";


	private static final Logger log = LogManager.getLogger(DefaultCapabilityPersonFilter.class);

	private final CapabilityConfigGroup config;

	/**
	 * The study area, or null if the home location is not to be checked.
	 */
	@Nullable
	private final ShpOptions.Index studyArea;

	private final Set<String> subpopulations;

	/**
	 * The youngest age that is kept, or 0 and below to skip the criterion.
	 */
	private final int minAge;

	private final Counts counts = new Counts();

	/**
	 * @param config the capability settings, used to tell a home activity from any other
	 * @param studyArea the area the persons have to live in, built with the crs of the population as
	 * the query crs, e.g. {@code shp.createIndex(config.global().getCoordinateSystem(), "_")}. Null
	 * skips the home location criterion.
	 */
	public DefaultCapabilityPersonFilter(CapabilityConfigGroup config, @Nullable ShpOptions.Index studyArea) {
		this(config, studyArea, Set.of(DEFAULT_SUBPOPULATION));
	}

	/**
	 * @param subpopulations the subpopulations to keep. An empty set keeps every subpopulation.
	 * @see #DefaultCapabilityPersonFilter(CapabilityConfigGroup, ShpOptions.Index)
	 */
	public DefaultCapabilityPersonFilter(CapabilityConfigGroup config, @Nullable ShpOptions.Index studyArea,
										 Set<String> subpopulations) {
		this(config, studyArea, subpopulations, config.getMinAge());
	}

	/**
	 * @param minAge the youngest age to keep, overriding {@link CapabilityConfigGroup#getMinAge()}, which
	 * is what the other constructors use. 0 or less skips the age criterion, which is what a population
	 * without an {@code age} attribute needs.
	 * @see #DefaultCapabilityPersonFilter(CapabilityConfigGroup, ShpOptions.Index)
	 */
	public DefaultCapabilityPersonFilter(CapabilityConfigGroup config, @Nullable ShpOptions.Index studyArea,
										 Set<String> subpopulations, int minAge) {
		this.config = config;
		this.studyArea = studyArea;
		this.subpopulations = new LinkedHashSet<>(subpopulations);
		this.minAge = minAge;

		if (studyArea == null) {
			log.info("No study area given, so the home location of the persons is not checked.");
		}
		if (minAge <= 0) {
			log.info("No minimum age given, so the age of the persons is not checked.");
		}
	}

	@Override
	public boolean accept(Person person) {

		this.counts.seen++;

		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			this.counts.withoutPlan++;
			return false;
		}

		String subpopulation = PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && !this.subpopulations.isEmpty() && !this.subpopulations.contains(subpopulation)) {
			this.counts.otherSubpopulation++;
			return false;
		}

		if (this.minAge > 0) {
			Integer age = age(person);
			if (age == null) {
				this.counts.withoutAge++;
				return false;
			}
			if (age < this.minAge) {
				this.counts.tooYoung++;
				return false;
			}
		}

		if (this.studyArea != null) {
			Coord home = homeCoordinate(person);
			if (home == null) {
				this.counts.withoutHomeCoordinate++;
				return false;
			}
			if (!this.studyArea.contains(home)) {
				this.counts.outsideStudyArea++;
				return false;
			}
		}

		if (!startsAndEndsAtHome(plan)) {
			this.counts.notHomeToHome++;
			return false;
		}
		if (!lastNonHomeActivityEndsWithinDay(plan)) {
			this.counts.pastMidnight++;
			return false;
		}

		this.counts.accepted++;
		return true;
	}

	/**
	 * Where the person lives: the {@link #HOME_X} / {@link #HOME_Y} attributes if the person carries
	 * them, otherwise the coordinate of the first home activity of the selected plan.
	 *
	 * @return the home coordinate, null if neither the attributes nor the plan give one. The latter
	 * happens for a plan whose activities only reference links; run such a population through
	 * {@code ExtractHomeCoordinates} or a routing step first.
	 */
	@Nullable
	public Coord homeCoordinate(Person person) {

		Double x = asDouble(person.getAttributes().getAttribute(HOME_X));
		Double y = asDouble(person.getAttributes().getAttribute(HOME_Y));
		if (x != null && y != null) {
			return new Coord(x, y);
		}

		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			return null;
		}

		return activitiesOf(plan).stream()
			.filter(activity -> this.config.isHome(activity.getType()))
			.map(Activity::getCoord)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * The age of the person in years. Read off the attribute directly rather than through
	 * {@code PersonUtils.getAge}, which casts to {@code Integer} and therefore fails on a plans file
	 * that wrote the age as a string.
	 *
	 * @return the age, null if the person does not carry one that can be read as a number
	 */
	@Nullable
	public static Integer age(Person person) {
		Double age = asDouble(person.getAttributes().getAttribute(AGE));
		if (age==null){
			age = asDouble(person.getAttributes().getAttribute("SNZ_age"));
		}
		return age == null ? null : (int) Math.floor(age);
	}

	@Override
	public void logSummary() {
		this.counts.log();
	}

	/**
	 * @return true if the first and the last activity of the plan are home activities, which includes
	 * the plan of someone who stays home all day
	 */
	private boolean startsAndEndsAtHome(Plan plan) {
		List<Activity> activities = activitiesOf(plan);
		return !activities.isEmpty()
			&& this.config.isHome(activities.get(0).getType())
			&& this.config.isHome(activities.get(activities.size() - 1).getType());
	}

	/**
	 * The last activity that is not home is the one the person comes home from, so it has to be over
	 * before the analysed day is. Activity start times are reconstructed from the trips of the plan,
	 * because a plans file rarely stores them.
	 *
	 * @return true if the person leaves their last non-home activity at 24:00:00 at the latest, or if
	 * there is no non-home activity at all
	 */
	private boolean lastNonHomeActivityEndsWithinDay(Plan plan) {

		List<Activity> activities = activitiesOf(plan);
		List<OptionalTime> startTimes = FundamentalPlanExtractor.startTimes(plan);

		for (int i = activities.size() - 1; i >= 0; i--) {
			Activity activity = activities.get(i);
			if (this.config.isHome(activity.getType())) {
				continue;
			}
			// an activity that says neither when it ends nor how long it takes runs until the end of the
			// day and beyond, so an undefined departure time is not within the day either
			OptionalTime departure = FundamentalPlanExtractor.departureTime(activity, startTimes.get(i));
			return departure.isDefined() && departure.seconds() <= FreePeriod.END_OF_DAY;
		}

		// the person is home all day
		return true;
	}

	private static List<Activity> activitiesOf(Plan plan) {
		return TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities);
	}

	/**
	 * Person attributes come out of a plans file as the type they were written with, so the home
	 * coordinate may arrive as a double or as a string.
	 *
	 * @return the value as a double, null if it is none
	 */
	@Nullable
	private static Double asDouble(@Nullable Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String string) {
			try {
				return Double.valueOf(string.trim());
			} catch (NumberFormatException e) {
				log.warn("'{}' is not a coordinate, ignoring it.", string);
			}
		}
		return null;
	}

	/**
	 * Why persons were dropped, so that the population the analysis speaks for can be explained.
	 */
	private final class Counts {
		private int seen;
		private int accepted;
		private int withoutPlan;
		private int otherSubpopulation;
		private int withoutAge;
		private int tooYoung;
		private int withoutHomeCoordinate;
		private int outsideStudyArea;
		private int notHomeToHome;
		private int pastMidnight;

		private void log() {

			DefaultCapabilityPersonFilter filter = DefaultCapabilityPersonFilter.this;

			// which criteria actually ran, so that a row count can be read without going back to the
			// command line that produced it
			log.info("Person filter criteria:");
			log.info("  subpopulation   : {}", filter.subpopulations.isEmpty()
				? "any" : filter.subpopulations);
			log.info("  minimum age     : {}", filter.minAge > 0
				? filter.minAge + " years, from the '" + AGE + "' attribute" : "not checked");
			log.info("  study area      : {}", filter.studyArea != null
				? "home location has to be inside the given shape" : "not checked");
			log.info("  activity chain  : has to start and end at home, and the last non-home activity "
				+ "has to be over by {}", Time.writeTime(FreePeriod.END_OF_DAY));

			log.info("Person filter: {} of {} persons remain ({}).", this.accepted, this.seen,
				share(this.accepted));

			logDropped(this.withoutPlan, "no selected plan");
			logDropped(this.otherSubpopulation, "not in subpopulation " + filter.subpopulations);
			logDropped(this.tooYoung, "younger than " + filter.minAge);
			logDropped(this.outsideStudyArea, "living outside the study area");
			logDropped(this.notHomeToHome, "the day does not start and end at home");
			logDropped(this.pastMidnight, "the last non-home activity is not over by "
				+ Time.writeTime(FreePeriod.END_OF_DAY));

			// these two are not statements about the population but about the input being incomplete,
			// so they are warnings: they silently shrink the population the analysis speaks for
			if (this.withoutAge > 0) {
				log.warn("  {} dropped ({}): age unknown. These persons carry no readable '{}' attribute. "
					+ "Set the minimum age to 0 if the population has no ages at all.",
					this.withoutAge, share(this.withoutAge), AGE);
			}
			if (this.withoutHomeCoordinate > 0) {
				log.warn("  {} dropped ({}): home location unknown. Neither the '{}' / '{}' attributes nor a "
					+ "home activity with a coordinate are there to place these persons.",
					this.withoutHomeCoordinate, share(this.withoutHomeCoordinate), HOME_X, HOME_Y);
			}
		}

		private void logDropped(int dropped, String why) {
			if (dropped > 0) {
				log.info("  {} dropped ({}): {}.", dropped, share(dropped), why);
			}
		}

		/**
		 * @return the given count as a share of the persons seen, e.g. {@code 12.3%}
		 */
		private String share(int count) {
			return this.seen == 0 ? "-" : String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * count / this.seen);
		}
	}
}
