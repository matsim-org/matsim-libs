package besttimeresponse;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface InterpolatedTravelTimes {

	public TimeDiscretization getTimeDiscretization();

	public double getTravelTime_s(final Object origin, final Object destination, final double dptTime_s,
			final Object mode);

	public double getTravelTime_s(Trip trip);

	public double get_dTravelTime_dDptTime(Trip trip);

	public double getTravelTimeOffset_s(Trip trip);

}
