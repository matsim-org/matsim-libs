package org.matsim.contrib.msp.capability;

import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.measure.FlexibilityFunction;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.Map;

/**
 * The parameters of one type of discretionary activity, e.g. of shopping. One parameter set per type; the
 * types that have a parameter set are exactly the types the capability analysis reports on.
 *
 * @see CapabilityConfigGroup#getDiscretionaryActivityParams()
 */
public final class DiscretionaryActivityParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "discretionaryActivity";

	private static final String TYPE = "type";
	private static final String MIN_DURATION = "minDuration";
	private static final String MAX_EXTRA_TRAVEL_TIME = "maxExtraTravelTime";
	private static final String BASE_UTILITY = "baseUtility";
	private static final String FLEXIBILITY_UTILITY = "flexibilityUtility";
	private static final String REFERENCE_DURATION = "referenceDuration";
	private static final String FLEXIBILITY_FUNCTION = "flexibilityFunction";

	/**
	 * The type of the discretionary activity, as the facilities call it, e.g. {@code shop}.
	 *
	 * @see DiscretionaryActivityFacility#getType()
	 */
	private String type;

	/**
	 * How long the activity has to last to be worth performing [s]. An insertion that cannot give the
	 * agent at least this much time on site does not count as possible at all.
	 */
	private double minDuration = 1800;

	/**
	 * How much extra travel time the agent may take on to reach the facility [s]. Purely to keep the
	 * search space small: a facility that costs more than this detour is not considered.
	 */
	private double maxExtraTravelTime = 1800;

	/**
	 * The utility of being able to perform the activity for {@link #minDuration}, i.e. {@code u_0}.
	 * Counted once per facility, however many insertion positions are possible - the flexibility of
	 * having several positions is what {@link #flexibilityUtility} is for.
	 */
	private double baseUtility = 3.0;

	/**
	 * The utility of one {@link #referenceDuration} of slack, i.e. {@code k}.
	 * <p>
	 * Deliberately well below MATSim's marginal utility of performing (+6/h): being able to do the
	 * activity at all should matter more than being able to choose freely when.
	 */
	private double flexibilityUtility = 1.0;

	/**
	 * The unit the slack is measured in [s], i.e. {@code t_typ}.
	 * <p>
	 * This is a scale, not a threshold - it is to {@link #minDuration} roughly what MATSim's typical
	 * duration is to its minimal duration. It sets where the diminishing returns of
	 * {@link FlexibilityFunction#LOG} set in: an insertion with exactly this much slack gets
	 * {@code k * ln(2)}.
	 * <p>
	 * One hour by default, which keeps the flexibility bonus of a person who is free all day just
	 * below {@link #baseUtility}. Setting it to {@link #minDuration} is defensible too - the slack is
	 * then counted in "further start slots of the activity" - but then lower {@code k} to about 0.8 to
	 * keep that ordering.
	 */
	private double referenceDuration = 3600;

	private FlexibilityFunction flexibilityFunction = FlexibilityFunction.LOG;

	public DiscretionaryActivityParams() {
		super(SET_NAME);
	}

	public DiscretionaryActivityParams(String type) {
		this();
		this.type = type;
	}

	@StringGetter(TYPE)
	public String getType() {
		return this.type;
	}

	@StringSetter(TYPE)
	public void setType(String type) {
		this.type = type;
	}

	@StringGetter(MIN_DURATION)
	public double getMinDuration() {
		return this.minDuration;
	}

	@StringSetter(MIN_DURATION)
	public void setMinDuration(double minDuration) {
		this.minDuration = minDuration;
	}

	@StringGetter(MAX_EXTRA_TRAVEL_TIME)
	public double getMaxExtraTravelTime() {
		return this.maxExtraTravelTime;
	}

	@StringSetter(MAX_EXTRA_TRAVEL_TIME)
	public void setMaxExtraTravelTime(double maxExtraTravelTime) {
		this.maxExtraTravelTime = maxExtraTravelTime;
	}

	@StringGetter(BASE_UTILITY)
	public double getBaseUtility() {
		return this.baseUtility;
	}

	@StringSetter(BASE_UTILITY)
	public void setBaseUtility(double baseUtility) {
		this.baseUtility = baseUtility;
	}

	@StringGetter(FLEXIBILITY_UTILITY)
	public double getFlexibilityUtility() {
		return this.flexibilityUtility;
	}

	@StringSetter(FLEXIBILITY_UTILITY)
	public void setFlexibilityUtility(double flexibilityUtility) {
		this.flexibilityUtility = flexibilityUtility;
	}

	@StringGetter(REFERENCE_DURATION)
	public double getReferenceDuration() {
		return this.referenceDuration;
	}

	@StringSetter(REFERENCE_DURATION)
	public void setReferenceDuration(double referenceDuration) {
		this.referenceDuration = referenceDuration;
	}

	@StringGetter(FLEXIBILITY_FUNCTION)
	public FlexibilityFunction getFlexibilityFunction() {
		return this.flexibilityFunction;
	}

	@StringSetter(FLEXIBILITY_FUNCTION)
	public void setFlexibilityFunction(FlexibilityFunction flexibilityFunction) {
		this.flexibilityFunction = flexibilityFunction;
	}

	/**
	 * @param slack the width of the start time window of one insertion [s]
	 * @return the utility of being able to perform this activity, i.e. {@code u_0 + k * f(S)}
	 */
	public double utility(double slack) {
		return this.baseUtility + this.flexibilityFunction.apply(
			slack, this.flexibilityUtility, this.referenceDuration);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(TYPE, "Type of the discretionary activity as the facilities call it, e.g. 'shop'.");
		comments.put(MIN_DURATION, "How long [s] the activity has to last to be worth performing. An insertion that "
			+ "cannot give the agent this much time on site does not count as possible.");
		comments.put(MAX_EXTRA_TRAVEL_TIME, "How much extra travel time [s] the agent may take on to reach the "
			+ "facility. Only there to keep the search space small.");
		comments.put(BASE_UTILITY, "Utility of being able to perform the activity for minDuration (u_0). Counted once "
			+ "per facility, no matter how many insertion positions are possible.");
		comments.put(FLEXIBILITY_UTILITY, "Utility of one referenceDuration of slack (k). Should stay well "
			+ "below MATSim's marginal utility of performing, so that being able to perform the activity at all "
			+ "matters more than being free to choose when.");
		comments.put(REFERENCE_DURATION, "Unit [s] the slack is measured in (t_typ). A scale, not a "
			+ "threshold: it sets where the diminishing returns of the LOG function set in.");
		comments.put(FLEXIBILITY_FUNCTION, "How the slack of one insertion becomes a utility bonus. One of "
			+ Arrays.toString(FlexibilityFunction.values()) + ". Only LOG is implemented so far.");
		return comments;
	}
}
