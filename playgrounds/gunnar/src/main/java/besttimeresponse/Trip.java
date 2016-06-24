package besttimeresponse;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Trip {

	// -------------------- CONSTANTS --------------------

	final Object origin;

	final Object destination;

	// modifiable in the course of the optimization
	double departureTime_s;

	final Object mode;

	// ratio of desired to realized activity duration at origin
	final double originTimePressure;

	// ratio of desired to realized activity duration at destination
	final double destinationTimePressure;

	final boolean originClosesBeforeDeparture;

	final boolean destinationOpensAfterArrival;

	final boolean earlyDepartureFromOrigin;

	final boolean lateArrivalToDestination;

	// -------------------- CONSTRUCTION --------------------

	public Trip(final Object origin, final Object destination, final double departureTime_s, final Object mode,
			final double originTimePressure, final double destinationTimePressure, final int originClosingTime_s,
			final int destinationOpeningTime_s, final int earliestDepartureTime_s, final int latestArrivalTime_s,
			final InterpolatedTravelTimes travelTimes) {

		this.origin = origin;
		this.destination = destination;
		this.departureTime_s = departureTime_s;
		this.mode = mode;
		this.originTimePressure = originTimePressure;
		this.destinationTimePressure = destinationTimePressure;

		/*---------------------------------------------------------------------
		 * DEPARTURE AND ARRIVAL TIME STEPS.
		 * 
		 * Departure and arrival _time_ are mapped on a discrete grid by
		 * rounding down on the nearest grid point, this yields the 
		 * corresponding _step_. The _interval_ in which departure and arrival
		 * _time_ are located then has the same integer index as the respective
		 * _step_.
		 * 
		 *                    departure time . . . . . . . . arrival time
		 *                           |                            |
		 * ---+--------------+--------------+--------------+--------------+----
		 *                   |                             |
		 * 	         departure time step           arrival time step
		 *                   \______________/              \______________/
		 *                  departure interval              arrival interval
		 * 
		 *---------------------------------------------------------------------
		 */
		final int departureTimeStep = travelTimes.getTimeDiscretization().getBin(departureTime_s);
		final double arrivalTime_s = departureTime_s
				+ travelTimes.getTravelTime_s(origin, destination, departureTime_s, mode);
		final int arrivalTimeStep = travelTimes.getTimeDiscretization().getBin(arrivalTime_s);

		/*---------------------------------------------------------------------
		 * DEPARTURE AFTER CLOSING TIME?
		 * 
		 * The closing time is treated as if it was located exactly on a time
		 * grid point. 
		 * 
		 *                        departure after closing time
		 *                    /-------------------------------------> 
		 *                   /                            
		 * ---+--------------+--------------+--------------+--------------+----
		 *                   |              |              |              |
		 *           closing time (step)    |              |              |
		 *                   |______________|______________|______________|___
		 *                       departure time steps after closing time
		 *
		 *--------------------------------------------------------------------- 
		 */
		final int originClosingTimeStep = travelTimes.getTimeDiscretization().getBin(originClosingTime_s);
		this.originClosesBeforeDeparture = (originClosingTimeStep <= departureTimeStep);

		/*---------------------------------------------------------------------
		 * EARLY DEPARTURE? 
		 * 
		 * The earliest departure time is treated as if it was located exactly 
		 * on a time grid point. 
		 * 
		 *            early departure times 
		 * ----------------------------------------------\ 
		 *                                                \
		 * ---+--------------+--------------+--------------+--------------+----
		 *    |              |              |              |
		 * ___|______________|______________|   earliest departure time (step)
		 *     early departure time steps                                 
		 *                                      
		 *--------------------------------------------------------------------- 
		 */
		final int earliestDepartureTimeStep = travelTimes.getTimeDiscretization().getBin(earliestDepartureTime_s);
		this.earlyDepartureFromOrigin = (departureTimeStep < earliestDepartureTimeStep);

		/*---------------------------------------------------------------------
		 * DESTINATION OPENS AFTER ARRIVAL?
		 * 
		 * The destination opening time is treated as if it was located exactly 
		 * on a time grid point. 
		 * 
		 *            arrival times before opening
		 * ----------------------------------------------\ 
		 *                                                \
		 * ---+--------------+--------------+--------------+--------------+----
		 *    |              |              |              |
		 * ___|______________|______________|  destination opening time (step)
		 * arrival time steps before opening                                 
		 *                                      
		 *--------------------------------------------------------------------- 
		 */
		final int destinationOpeningTimeStep = travelTimes.getTimeDiscretization().getBin(destinationOpeningTime_s);
		this.destinationOpensAfterArrival = (arrivalTimeStep < destinationOpeningTimeStep);

		/*---------------------------------------------------------------------
		 * LATE ARRIVAL?
		 * 
		 * The latest arrival time is treated as if it was located exactly on a 
		 * time grid point. 
		 * 
		 *                        late arrival time
		 *                    /-------------------------------------> 
		 *                   /                            
		 * ---+--------------+--------------+--------------+--------------+----
		 *                   |              |              |              |
		 *      latest arrival time (step)  |              |              |
		 *                   |______________|______________|______________|___
		 *                       late arrival time steps
		 *
		 *--------------------------------------------------------------------- 
		 */
		final int latestArrivalTimeStep = travelTimes.getTimeDiscretization().getBin(latestArrivalTime_s);
		this.lateArrivalToDestination = (latestArrivalTimeStep <= arrivalTimeStep);
	}
}
