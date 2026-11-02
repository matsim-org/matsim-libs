package org.matsim.contrib.msp.capability.measure;

/**
 * What one insertion costs the agent, in utility: {@code c_insertion}.
 * <p>
 * Only the detour matters today, so the default is {@code VoT * t_extra} - see {@link #ofTime}. The
 * interface exists so that further terms can be added later (the distance of the detour, a parking fee,
 * a mode constant) without touching the insertion arithmetic: whatever is added here is subtracted from
 * the base utility in exactly one place.
 * <p>
 * <b>Why not MATSim's {@link org.matsim.core.router.util.TravelDisutility}?</b> That interface is a
 * <i>per link</i> function, meant to be minimised by a router, and it carries the value of time only
 * implicitly, as the {@code marginalCostOfTime_s} and {@code marginalCostOfDistance_m} its
 * implementations are built with. Using it here would mean summing it over the links of a route, and the
 * capability analysis deliberately has no routes: {@link CapabilityTravelTimes} returns a travel time,
 * and for a teleported mode there are no links at all. On top of that, the quantity wanted here is a
 * <i>difference</i> - the detour {@code t_in + t_out - t_direct} - rather than the cost of a path, so
 * the baseline trip the agent was making anyway has to cancel out. Should distance ever be needed, the
 * step is to have {@link CapabilityTravelTimes} report it next to the travel time and to widen this
 * interface, which can then be parameterised from the same scoring parameters a
 * {@code TravelDisutility} uses.
 */
@FunctionalInterface
public interface InsertionCost {

	/**
	 * @param extraTravelTime the detour of the insertion [s], i.e. how much longer the agent is under
	 * way than it would have been without the discretionary activity. Never negative.
	 * @return what that costs in utility, to be subtracted from the base utility. Positive numbers are
	 * costs.
	 */
	double cost(double extraTravelTime);

	/**
	 * The default: travel time only.
	 * <p>
	 * The value of time is given <b>per hour</b>, as every utility rate in this analysis is, while the
	 * detour arrives in seconds, as every duration does. This is the one place where the two meet, so it
	 * is the one place that divides by 3600.
	 *
	 * @param valueOfTimePerHour what one hour of travel is worth in utility, e.g.
	 * {@link org.matsim.contrib.msp.capability.CapabilityConfigGroup#getValueOfTimePerHour()}
	 */
	static InsertionCost ofTime(double valueOfTimePerHour) {
		double perSecond = valueOfTimePerHour / 3600;
		return extraTravelTime -> perSecond * extraTravelTime;
	}
}
