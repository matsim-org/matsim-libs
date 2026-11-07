package org.matsim.application.analysis.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.utils.objectattributes.attributable.Attributable;

/** Typed access to the results attached to an analyzed experienced plan and its original person. */
public final class VttsAttributes {

	/** Keep the KN attribute name so that its existing consumers continue to work. */
	public static final String MUSE = "marginal_utility_of_starting_earlier_h";
	/** Score per money unit, resolved for the original person. */
	public static final String MONEY_UTILITY = "vtts_marginal_utility_of_money";
	/** Monetary activity effect in money/hour, excluding the direct travel-time effect. */
	public static final String VSE = "vtts_value_of_starting_earlier_per_hour";
	/** Activity plus direct travel-time saving, in money/hour. */
	public static final String VTTS = "vtts_money_per_hour";
	/** Number of destinations with finite MUSE; the denominator of the person's MUSE mean. */
	public static final String DESTINATIONS = "vtts_valid_destinations";
	/** Number with finite VTTS; can be smaller than DESTINATIONS when conversion is unsupported. */
	public static final String VTTS_DESTINATIONS = "vtts_valid_vtts_destinations";
	/** Coverage outcome such as ok, excluded, incomplete_plan, or unsupported_mode. */
	public static final String STATUS = "vtts_status";
	/** Realized trip's analysis main mode, whose traveling time receives the saving. */
	public static final String MODE = "vtts_saving_mode";
	/** Direct traveling coefficient in score/hour, normally negative. */
	public static final String TRAVEL_UTILITY = "vtts_travel_utility_per_hour";

	private VttsAttributes() { }

	public static void setMuse(Attributable object, double value) {
		// Activities hold individual destination values; plans and persons hold destination means.
		object.getAttributes().putAttribute(MUSE, value);
	}

	public static Double getMuse(Activity activity) {
		Double value = getDouble(activity, MUSE);
		// Preserve the older KN activity-key fallback, including its existing sign convention.
		return value != null ? value : getDouble(activity, "marginal_utility_of_starting_later_h");
	}

	public static Double getMuse(Plan plan) {
		return getDouble(plan, MUSE);
	}

	public static Double getDouble(Attributable object, String key) {
		Object value = object.getAttributes().getAttribute(key);
		return value instanceof Number number ? number.doubleValue() : null;
	}

	static void resetSummary(Attributable object, String status) {
		// Undefined is NaN, not zero: exports omit nonfinite values while retaining genuine zeros.
		for (String key : new String[]{MUSE, MONEY_UTILITY, VSE, VTTS})
			object.getAttributes().putAttribute(key, Double.NaN);
		object.getAttributes().putAttribute(DESTINATIONS, 0);
		object.getAttributes().putAttribute(VTTS_DESTINATIONS, 0);
		object.getAttributes().putAttribute(STATUS, status);
	}
}
