package org.matsim.contrib.msp.capability;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacilityExtractor;
import org.matsim.contrib.msp.capability.structure.FreePeriod;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Config group of the capability analysis.
 *
 * @see CapabilityAnalysis
 */
public final class CapabilityConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "capability";

	private static final String MANDATORY_ACTIVITIES = "mandatoryActivities";
	private static final String FACILITY_TYPE_ATTRIBUTE = "facilityTypeAttribute";
	private static final String FACILITY_SUB_TYPE_ATTRIBUTE = "facilitySubTypeAttribute";
	private static final String HOME_ACTIVITY = "homeActivity";
	private static final String MIN_AGE = "minAge";
	private static final String MODES = "modes";
	private static final String VALUE_OF_TIME = "valueOfTimePerHour";
	private static final String COST_LOGSUM_SCALE = "costLogsumScale";
	private static final String BETA_WITHIN_SUB_TYPE = "betaWithinSubType";
	private static final String BETA_ACROSS_SUB_TYPE = "betaAcrossSubType";

	/**
	 * The activity types that are mandatory unless configured otherwise. Derived from the activity
	 * analysis of the Dresden scenario: next to <code>home</code>, these are the types an agent
	 * performs because it has to, not because it chooses to.
	 * <p>
	 * <code>business</code> is mandatory in that analysis as well, but it is left out here: a
	 * business trip is part of working rather than an activity of its own, and it only shows up for
	 * agents that already have a <code>work</code> activity.
	 */
	public static final List<String> DEFAULT_MANDATORY_ACTIVITIES = List.of("home", "work", "educ", "errands", "accomp");

	/**
	 * The modes the capability analysis reports on by default: car at free speed and teleported walk.
	 * Bike is supported and only left out of the default because it needs the scenario to route bike on
	 * the network - add it to {@code modes} where that is the case. Public transport and car under
	 * congested conditions are meant to follow.
	 */
	public static final List<String> DEFAULT_MODES = List.of(TransportMode.car, TransportMode.walk);

	/**
	 * MATSim appends the typical duration to the activity type, e.g. <code>work_7200</code>.
	 */
	private static final Pattern TYPICAL_DURATION_SUFFIX = Pattern.compile("_\\d+$");

	/**
	 * Activity types an agent cannot choose not to perform. Everything else is considered discretionary
	 * and is therefore removed when the fundamental plan is derived.
	 */
	private Set<String> mandatoryActivities = new LinkedHashSet<>(DEFAULT_MANDATORY_ACTIVITIES);

	/**
	 * Facility attribute the type of a discretionary activity facility is read from.
	 *
	 * @see DiscretionaryActivityFacilityExtractor
	 */
	private String facilityTypeAttribute = "capabilityType";

	/**
	 * Facility attribute the sub-type of a discretionary activity facility is read from.
	 *
	 * @see DiscretionaryActivityFacilityExtractor
	 */
	private String facilitySubTypeAttribute = "capabilitySubType";

	/**
	 * The mandatory activity that is <i>elastic</i>: the agent may come home later, leave earlier or
	 * skip a home stay altogether, so home activities never constrain when a discretionary activity can be
	 * inserted. Only the other mandatory activities pin the schedule down.
	 *
	 * @see FreePeriod
	 */
	private String homeActivity = "home";

	/**
	 * The youngest age the analysis speaks for, in years. The measure asks which discretionary
	 * activities a person could choose to perform, and a child's day is not theirs to rearrange.
	 * 0 switches the criterion off, which a population without an {@code age} attribute needs.
	 */
	private int minAge = 18;

	/**
	 * The modes the capability is computed for. One row of the output per mode.
	 */
	private Set<String> modes = new LinkedHashSet<>(DEFAULT_MODES);

	/**
	 * What an <b>hour</b> of travel is worth in utility, i.e. the {@code VoT} of
	 * {@code c_insertion = VoT * t_extra}. Per hour, like MATSim's own
	 * {@code marginalUtilityOfTraveling_utils_hr}, and the one utility rate of this analysis - every
	 * other parameter is either a plain utility ({@code u_0}, {@code k}) or a duration in seconds
	 * ({@code minDuration}, {@code referenceDuration}). The single conversion to seconds happens in
	 * {@link org.matsim.contrib.msp.capability.measure.InsertionCost#ofTime}.
	 * <p>
	 * The default of 6 matches MATSim's marginal utility of performing, so the trade off between
	 * travelling and being somewhere stays consistent with the scoring of the run.
	 */
	private double valueOfTimePerHour = 6.0;

	/**
	 * The scale of the cost logsum, i.e. theta in {@code (1/theta) * ln sum_j exp(-theta * VoT * cost_j)}.
	 * The accessibility measure of {@code contribs/accessibility} has the same shape.
	 */
	private double costLogsumScale = 1.0;

	/**
	 * {@code b_1}, the scale of the logsum over the facilities <i>within</i> one sub-type, i.e. over the
	 * branches of one supermarket chain. Larger than {@link #getBetaAcrossSubType()} because two shops
	 * of the same brand are close substitutes: the larger this is, the more the nest collapses onto its
	 * best member, and the less a second branch of the same brand adds.
	 * <p>
	 * {@code N} identical alternatives are worth {@code ln(N)/b_1} more than one, so this default makes
	 * a second branch of a chain the person can already reach worth {@code ln(2)/8 = 0.09} utils, i.e.
	 * about 3% of the default {@code u_0}. Note that the number only means that at the default utility
	 * scale; re-derive it from {@code ln(N)/b} if {@code u_0}, {@code k} or the value of time change.
	 */
	private double betaWithinSubType = 8.0;

	/**
	 * {@code b_2}, the scale of the logsum <i>across</i> sub-types, i.e. over the brands. Smaller than
	 * {@link #getBetaWithinSubType()}, which is what makes a further brand worth more than a further
	 * branch of a brand already available - and is also exactly the condition for the nested logit to be
	 * consistent with random utility maximisation: the dissimilarity parameter {@code b_2 / b_1} has to
	 * lie in {@code (0, 1]}.
	 * <p>
	 * This default makes a second, equally good brand worth {@code ln(2)/2 = 0.35} utils and four brands
	 * {@code ln(4)/2 = 0.69}, i.e. roughly a quarter of the default {@code u_0} for a realistic urban
	 * choice set.
	 */
	private double betaAcrossSubType = 2.0;

	public CapabilityConfigGroup() {
		super(GROUP_NAME);
		// deliberately no default parameter set: the types that have one are exactly the types the
		// analysis reports on, so a default would be a type nobody asked for, and a config that declares
		// none would quietly analyse that instead of saying so
	}

	@StringGetter(MANDATORY_ACTIVITIES)
	public String getMandatoryActivitiesAsString() {
		return CollectionUtils.setToString(this.mandatoryActivities);
	}

	@StringSetter(MANDATORY_ACTIVITIES)
	public void setMandatoryActivitiesAsString(String mandatoryActivities) {
		// keep the order in which the types were given, it makes the config easier to read back
		this.mandatoryActivities = new LinkedHashSet<>(CollectionUtils.stringToSet(mandatoryActivities));
	}

	/**
	 * @return the configured mandatory activity types, never null, possibly empty
	 */
	public Set<String> getMandatoryActivities() {
		return Collections.unmodifiableSet(this.mandatoryActivities);
	}

	public void setMandatoryActivities(Set<String> mandatoryActivities) {
		this.mandatoryActivities = new LinkedHashSet<>(mandatoryActivities);
	}

	@StringGetter(FACILITY_TYPE_ATTRIBUTE)
	public String getFacilityTypeAttribute() {
		return this.facilityTypeAttribute;
	}

	@StringSetter(FACILITY_TYPE_ATTRIBUTE)
	public void setFacilityTypeAttribute(String facilityTypeAttribute) {
		this.facilityTypeAttribute = facilityTypeAttribute;
	}

	@StringGetter(FACILITY_SUB_TYPE_ATTRIBUTE)
	public String getFacilitySubTypeAttribute() {
		return this.facilitySubTypeAttribute;
	}

	@StringSetter(FACILITY_SUB_TYPE_ATTRIBUTE)
	public void setFacilitySubTypeAttribute(String facilitySubTypeAttribute) {
		this.facilitySubTypeAttribute = facilitySubTypeAttribute;
	}

	@StringGetter(MIN_AGE)
	public int getMinAge() {
		return this.minAge;
	}

	@StringSetter(MIN_AGE)
	public void setMinAge(int minAge) {
		this.minAge = minAge;
	}

	@StringGetter(HOME_ACTIVITY)
	public String getHomeActivity() {
		return this.homeActivity;
	}

	@StringSetter(HOME_ACTIVITY)
	public void setHomeActivity(String homeActivity) {
		this.homeActivity = homeActivity;
	}

	@StringGetter(MODES)
	public String getModesAsString() {
		return CollectionUtils.setToString(this.modes);
	}

	@StringSetter(MODES)
	public void setModesAsString(String modes) {
		this.modes = new LinkedHashSet<>(CollectionUtils.stringToSet(modes));
	}

	public Set<String> getModes() {
		return Collections.unmodifiableSet(this.modes);
	}

	public void setModes(Set<String> modes) {
		this.modes = new LinkedHashSet<>(modes);
	}

	@StringGetter(VALUE_OF_TIME)
	public double getValueOfTimePerHour() {
		return this.valueOfTimePerHour;
	}

	@StringSetter(VALUE_OF_TIME)
	public void setValueOfTimePerHour(double valueOfTimePerHour) {
		this.valueOfTimePerHour = valueOfTimePerHour;
	}

	@StringGetter(COST_LOGSUM_SCALE)
	public double getCostLogsumScale() {
		return this.costLogsumScale;
	}

	@StringSetter(COST_LOGSUM_SCALE)
	public void setCostLogsumScale(double costLogsumScale) {
		this.costLogsumScale = costLogsumScale;
	}

	@StringGetter(BETA_WITHIN_SUB_TYPE)
	public double getBetaWithinSubType() {
		return this.betaWithinSubType;
	}

	@StringSetter(BETA_WITHIN_SUB_TYPE)
	public void setBetaWithinSubType(double betaWithinSubType) {
		this.betaWithinSubType = betaWithinSubType;
	}

	@StringGetter(BETA_ACROSS_SUB_TYPE)
	public double getBetaAcrossSubType() {
		return this.betaAcrossSubType;
	}

	@StringSetter(BETA_ACROSS_SUB_TYPE)
	public void setBetaAcrossSubType(double betaAcrossSubType) {
		this.betaAcrossSubType = betaAcrossSubType;
	}

	/**
	 * @return true if the given activity type is the elastic home activity
	 * @see #getHomeActivity()
	 */
	public boolean isHome(String activityType) {
		String type = stripTypicalDuration(activityType);
		return type.equals(this.homeActivity) || type.startsWith(this.homeActivity + "_");
	}

	/**
	 * The parameters of the discretionary activity types, by type. The types that have a parameter set are
	 * exactly the types the analysis reports on, so there is no second list of types to keep in sync.
	 *
	 * @return the parameter sets, by {@link DiscretionaryActivityParams#getType()}
	 */
	public Map<String, DiscretionaryActivityParams> getDiscretionaryActivityParams() {
		Map<String, DiscretionaryActivityParams> params = new LinkedHashMap<>();
		for (ConfigGroup set : getParameterSets(DiscretionaryActivityParams.SET_NAME)) {
			DiscretionaryActivityParams typeParams = (DiscretionaryActivityParams) set;
			if (typeParams.getType() == null) {
				throw new IllegalStateException("An '" + DiscretionaryActivityParams.SET_NAME
					+ "' parameter set has no 'type'.");
			}
			params.put(typeParams.getType(), typeParams);
		}
		return params;
	}

	/**
	 * @return the parameters of the given discretionary activity type, creating them if the config does not
	 * have them yet
	 */
	public DiscretionaryActivityParams getOrCreateDiscretionaryActivityParams(String type) {
		DiscretionaryActivityParams params = getDiscretionaryActivityParams().get(type);
		if (params == null) {
			params = new DiscretionaryActivityParams(type);
			addParameterSet(params);
		}
		return params;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Rejects any other type rather than letting {@link ConfigGroup} turn it into a nameless parameter
	 * set: the capability module has exactly one kind of parameter set, so an unknown type is a
	 * misspelling, and MATSim would otherwise read it without complaint and the analysis would silently
	 * leave out the activity type it was meant to declare.
	 *
	 * @throws IllegalArgumentException if the type is not {@link DiscretionaryActivityParams#SET_NAME}
	 */
	@Override
	public ConfigGroup createParameterSet(String type) {
		if (DiscretionaryActivityParams.SET_NAME.equals(type)) {
			return new DiscretionaryActivityParams();
		}
		throw new IllegalArgumentException("The '" + GROUP_NAME + "' config module has no parameter set of "
			+ "type '" + type + "'. The only one it takes is '" + DiscretionaryActivityParams.SET_NAME
			+ "', one per discretionary activity type to analyse.");
	}

	/**
	 * @return true if the given activity type is mandatory
	 * @see #mandatoryTypeOf(String)
	 */
	public boolean isMandatory(String activityType) {
		return mandatoryTypeOf(activityType).isPresent();
	}

	/**
	 * Matches an activity type of a scenario against the configured mandatory types. A type matches
	 * if it equals a configured type once the typical duration is stripped off, or if it is a
	 * sub-type of one, i.e. the configured type followed by an underscore. So configuring
	 * <code>educ</code> covers <code>educ_primary</code> and <code>educ_higher_28800</code>, and
	 * configuring <code>home</code> covers <code>home_28200</code> and <code>home_morning</code>.
	 * The longest configured type wins, so the order of the configuration does not matter.
	 *
	 * @return the configured type the given activity type belongs to, empty if it belongs to none and
	 * the activity is therefore discretionary
	 */
	public Optional<String> mandatoryTypeOf(String activityType) {

		String type = stripTypicalDuration(activityType);
		String match = null;

		for (String mandatory : this.mandatoryActivities) {
			if (!type.equals(mandatory) && !type.startsWith(mandatory + "_")) {
				continue;
			}
			if (match == null || mandatory.length() > match.length()) {
				match = mandatory;
			}
		}

		return Optional.ofNullable(match);
	}

	/**
	 * @return the given activity type without the typical duration MATSim appends to it, e.g.
	 * {@code work} for {@code work_7200}. Types without such a suffix are returned unchanged.
	 */
	public static String stripTypicalDuration(String activityType) {
		return TYPICAL_DURATION_SUFFIX.matcher(activityType).replaceFirst("");
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MANDATORY_ACTIVITIES, "Comma-separated list of activity types that are considered mandatory. All other "
			+ "activity types are considered discretionary and are removed when the fundamental plan of an agent is derived. "
			+ "Give the basic types: the typical duration MATSim appends is ignored (so 'work' covers 'work_7200') and "
			+ "sub-types are covered by their basic type (so 'educ' covers 'educ_primary'). Default is "
			+ String.join(",", DEFAULT_MANDATORY_ACTIVITIES) + ".");
		comments.put(FACILITY_TYPE_ATTRIBUTE, "Name of the facility attribute that says what can be done at a facility, "
			+ "e.g. 'shop' or 'restaurant'. Several types can be listed separated by commas. This vocabulary is defined by "
			+ "whoever builds the facilities and does not have to match the MATSim activity types. Facilities without this "
			+ "attribute fall back to the types of their activity options.");
		comments.put(FACILITY_SUB_TYPE_ATTRIBUTE, "Name of the facility attribute that says which kind of the type a "
			+ "facility is, e.g. the brand of the supermarket or the kind of restaurant. Used for the variety analysis, "
			+ "and optional.");
		comments.put(HOME_ACTIVITY, "The mandatory activity that is treated as elastic: the agent may come home "
			+ "later, leave earlier or skip a home stay, so home never constrains when a discretionary activity can be "
			+ "inserted. Only the other mandatory activities pin the schedule down.");
		comments.put(MIN_AGE, "Youngest age the analysis speaks for, in years, read from the 'age' person "
			+ "attribute. Persons without a readable age are dropped, because there is no telling whether they "
			+ "qualify. Set to 0 for a population that carries no ages at all.");
		comments.put(MODES, "Comma-separated list of the modes the capability is computed for, one output row per "
			+ "mode. Default is " + String.join(",", DEFAULT_MODES) + ".");
		comments.put(VALUE_OF_TIME, "What one HOUR of travel is worth in utility (VoT), i.e. the cost of an "
			+ "insertion is VoT * t_extra. Per hour like MATSim's marginalUtilityOfTraveling_utils_hr, and the only "
			+ "utility rate here - u_0 and k are plain utilities, and every duration is in seconds. The default of "
			+ "6 matches MATSim's marginal utility of performing.");
		comments.put(COST_LOGSUM_SCALE, "Scale (theta) of the cost logsum: (1/theta) * ln sum_j exp(-theta * c_j), "
			+ "the same shape as the accessibility measure of contribs/accessibility.");
		comments.put(BETA_WITHIN_SUB_TYPE, "Scale (b_1) of the logsum over the facilities within one sub-type, e.g. "
			+ "over the branches of one supermarket chain. N identical alternatives are worth ln(N)/b_1 more than "
			+ "one, which is how to pick it: at 8, a second branch of the same chain adds 0.09 utils. Has to be "
			+ "LARGER than betaAcrossSubType, which is the condition for the nested logit to be consistent with "
			+ "random utility maximisation.");
		comments.put(BETA_ACROSS_SUB_TYPE, "Scale (b_2) of the logsum across sub-types, e.g. over the brands. "
			+ "Smaller than betaWithinSubType, so that a further brand is worth more than a further branch of a brand "
			+ "the agent can already reach: at 2, a second brand adds ln(2)/2 = 0.35 utils.");
		return comments;
	}
}
