/**
 * 
 */
package scenarios.braess.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Class to analyze a MATSim simulation of Braess' example.
 * 
 * It calculates the total and average travel time on the routes and the number
 * of users on each route. Both once in total and once depending on the
 * departure and arrival times.
 * 
 * Additionally it calculates the number of route starts per second and the
 * number of agents on each route per second.
 * 
 * Last but not least it calculates the total travel time in the network.
 * 
 * Note: This analyzer makes a difference between travel time and route travel
 * time: the travel time starts with the agent departure event, the route travel
 * time with the wait2link event. Both end with the agent departure event.
 * 
 * The results can be plotted by gnuplot scripts.
 * 
 * @author tthunig
 * 
 */
public class TtAnalyzeBraessRouteDistributionAndTT implements PersonArrivalEventHandler,
		PersonDepartureEventHandler, LinkEnterEventHandler,
		Wait2LinkEventHandler, PersonStuckEventHandler {
	
	private static final Logger log = Logger
			.getLogger(TtAnalyzeBraessRouteDistributionAndTT.class);

	private double totalTT;
	private double[] totalRouteTTs;
	private int[] routeUsers;

	private Map<Id<Person>, Double> personDepartureTimes; // departure event
															// time
	private Map<Id<Person>, Double> personRouteStartTime; // wait2link event
															// time

	private Map<Id<Person>, Integer> personRouteChoice;
	private Map<Double, double[]> routeStartsPerSecond;
	private Map<Double, double[]> onRoutePerSecond;

	private Map<Double, double[]> totalRouteTTsPerWait2LinkTime;
	private Map<Double, double[]> totalRouteTTsPerArrivalTime;
	private Map<Double, int[]> routeUsersPerWait2LinkTime;
	private Map<Double, int[]> routeUsersPerArrivalTime;

	public TtAnalyzeBraessRouteDistributionAndTT() {
		super();
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.totalTT = 0.0;
		this.totalRouteTTs = new double[3];
		this.routeUsers = new int[3];

		this.personDepartureTimes = new HashMap<>();
		this.personRouteStartTime = new HashMap<>();
		this.personRouteChoice = new HashMap<>();

		this.routeStartsPerSecond = new TreeMap<>();
		this.onRoutePerSecond = new TreeMap<>();

		this.totalRouteTTsPerWait2LinkTime = new TreeMap<>();
		this.totalRouteTTsPerArrivalTime = new TreeMap<>();
		this.routeUsersPerWait2LinkTime = new TreeMap<>();
		this.routeUsersPerArrivalTime = new TreeMap<>();
	}

	/**
	 * Remembers the persons departure times.
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"A person has departured at least two times without arrival.");
		}

		// remember the persons departure time
		this.personDepartureTimes.put(event.getPersonId(), event.getTime());
	}

	/**
	 * Fills the person route start time map
	 * 
	 * @param event
	 */
	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (this.personRouteStartTime.containsKey(event.getPersonId()))
			throw new IllegalStateException(
					"A person has it's at least second wait2link event without arrival.");
		this.personRouteStartTime.put(event.getPersonId(), event.getTime());
	}

	/**
	 * Determines the agents route choice.
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// collect information about the route choice.
		// the route is unique if you get a link enter event of link 3,4 or 5.
		int route = -1;
		switch (event.getLinkId().toString()) {
		case "2_4":
		case "2_8":
			// the person uses the lower route
			route = 2;
			break;
		case "3_4": // the person uses the middle route
			route = 1;
			break;
		case "3_5": // the person uses the upper route
			route = 0;
			break;
		default:
			break;
		}

		// if a route was determined
		if (route != -1) {
			if (this.personRouteChoice.containsKey(event.getPersonId()))
				throw new IllegalStateException("Person " + event.getPersonId()
						+ " was seen at least twice on a route specific link."
						+ " Did it travel more than once without arrival?");

			// remember the persons route choice
			this.personRouteChoice.put(event.getPersonId(), route);
		}
	}

	/**
	 * Calculates the total travel time and the route travel time of the agent.
	 * 
	 * Fills all fields and maps with the person specific route and travel time
	 * informations.
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " has arrived without departure.");
		}
		if (!this.personRouteChoice.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " arrived, but was not seen on any route.");
		}
		if (!this.personRouteStartTime.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " arrived without route start (i.e. without wait2link event).");
		}

		// calculate total travel time
		double personArrivalTime = event.getTime();
		double personDepartureTime = this.personDepartureTimes.get(event
				.getPersonId());
		double personTotalTT = personArrivalTime - personDepartureTime;
		this.totalTT += personTotalTT;

		// calculate travel time on route
		Double personRouteStart = this.personRouteStartTime.get(event
				.getPersonId());
		Double personTimeOnRoute = personArrivalTime - personRouteStart;
		int personRoute = this.personRouteChoice.get(event.getPersonId());

		this.totalRouteTTs[personRoute] += personTimeOnRoute;
		this.routeUsers[personRoute]++;

		// fill maps for calculating avg tt per route per arrival time
		if (!this.totalRouteTTsPerArrivalTime.containsKey(personArrivalTime)) {
			// this is equivalent to
			// !this.routeUsersPerArrivalTime.containsKey(personArrivalTime)
			this.totalRouteTTsPerArrivalTime.put(personArrivalTime,
					new double[3]);
			this.routeUsersPerArrivalTime.put(personArrivalTime, new int[3]);
		}
		this.totalRouteTTsPerArrivalTime.get(personArrivalTime)[personRoute] += personTimeOnRoute;
		this.routeUsersPerArrivalTime.get(personArrivalTime)[personRoute]++;

		// fill maps for calculating avg tt per route per wait2link time
		if (!this.totalRouteTTsPerWait2LinkTime
				.containsKey(personRouteStart)) {
			// this is equivalent to
			// !this.routeUsersPerDepartureTime.containsKey(personRouteStart)
			this.totalRouteTTsPerWait2LinkTime.put(personRouteStart,
					new double[3]);
			this.routeUsersPerWait2LinkTime
					.put(personRouteStart, new int[3]);
		}
		this.totalRouteTTsPerWait2LinkTime.get(personRouteStart)[personRoute] += personTimeOnRoute;
		this.routeUsersPerWait2LinkTime.get(personRouteStart)[personRoute]++;

		// increase the number of persons on route for each second the
		// person is traveling on it
		for (int i = 0; i < personTimeOnRoute; i++) {
			if (!this.onRoutePerSecond.containsKey(personRouteStart + i)) {
				this.onRoutePerSecond.put(personRouteStart + i, new double[3]);
			}
			this.onRoutePerSecond.get(personRouteStart + i)[this.personRouteChoice
					.get(event.getPersonId())]++;
		}

		// add one route start for the specific route start time
		if (!this.routeStartsPerSecond.containsKey(personRouteStart)) {
			this.routeStartsPerSecond.put(personRouteStart, new double[3]);
		}
		this.routeStartsPerSecond.get(personRouteStart)[personRoute]++;

		// remove all trip dependent information of the arrived person
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
		this.personRouteStartTime.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
//		// TODO check consistency
//		log.warn("Agent " + event.getPersonId() + " stucked on link " + event.getLinkId() + "."
//				+ " This handler doesn't consider stucked agents for the calculation of travel times.");
//		this.personDepartureTimes.remove(event.getPersonId());
//		this.personRouteStartTime.remove(event.getPersonId());
	}

	/**
	 * Calculates and returns the average travel times on the single routes in
	 * Braess' example. The first entry corresponds to the upper route, the
	 * second to the middle route and the third to the lower route.
	 * 
	 * @return average travel times
	 */
	public double[] calculateAvgRouteTTs() {
		double[] avgRouteTTs = new double[3];
		for (int i = 0; i < 3; i++) {
			avgRouteTTs[i] = this.totalRouteTTs[i] / this.routeUsers[i];
		}
		return avgRouteTTs;
	}

	public double getTotalTT() {
		return totalTT;
	}

	public double[] getTotalRouteTTs() {
		return totalRouteTTs;
	}

	public int[] getRouteUsers() {
		return routeUsers;
	}

	/**
	 * Returns a map containing the number of route starts for each time step.
	 * Thereby a route start is regarded as a link entering of link 2 or 3.
	 * 
	 * @return
	 */
	public Map<Double, double[]> getRouteStartsPerSecond() {
		// fill missing time steps between first departure and
		// last arrival with zero starts
		long firstStart = Long.MAX_VALUE;
		long lastStart = Long.MIN_VALUE;
		for (Double d : this.routeStartsPerSecond.keySet()) {
			// because of matsim route start times are always integer
			if (d < firstStart)
				firstStart = d.longValue();
			if (d > lastStart)
				lastStart = d.longValue();
		}
		for (long l = firstStart; l <= lastStart; l++) {
			if (!this.routeStartsPerSecond.containsKey((double) l)) {
				this.routeStartsPerSecond.put((double) l, new double[3]);
			}
		}

		return routeStartsPerSecond;
	}

	/**
	 * Returns the number of agents on route per time step. Thereby an agent is
	 * regarded to be on route if he's traveling on a link that is not the first
	 * or the last one (link 1 and 7).
	 * 
	 * @return
	 */
	public Map<Double, double[]> getOnRoutePerSecond() {
		// already contains entries for all time steps (seconds)
		// between first departure and last arrival
		return onRoutePerSecond;
	}

	/**
	 * Calculates the average route travel times per wait2link time.
	 * 
	 * Thereby the double array in each map entry contains the following average
	 * route travel times: 
	 * 0 - the average route travel time on the upper route.
	 * 1 - the average route travel time on the middle route.
	 * 2 - the average route travel time on the lower route.
	 * (always for agents with the specific wait2link time)
	 * 
	 * @return
	 */
	public Map<Double, double[]> calculateAvgRouteTTsPerWait2LinkTime() {
		Map<Double, double[]> avgTTsPerRoutePerWait2LinkTime = new TreeMap<>();

		// calculate average route travel times for existing departure times
		for (Double wait2linkTime : this.totalRouteTTsPerWait2LinkTime
				.keySet()) {
			double[] totalTTsPerRoute = this.totalRouteTTsPerWait2LinkTime
					.get(wait2linkTime);
			int[] usersPerRoute = this.routeUsersPerWait2LinkTime
					.get(wait2linkTime);
			double[] avgTTsPerRoute = new double[3];
			for (int i = 0; i < 3; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is using the specific route at this wait2link
					// time
					avgTTsPerRoute[i] = Double.NaN;
				else
					avgTTsPerRoute[i] = totalTTsPerRoute[i] / usersPerRoute[i];
			}
			avgTTsPerRoutePerWait2LinkTime.put(wait2linkTime, avgTTsPerRoute);
		}

		// fill missing time steps between first and last wait2link
		long firstWait2Link = Long.MAX_VALUE;
		long lastWait2Link = Long.MIN_VALUE;
		for (Double wait2linkTime : avgTTsPerRoutePerWait2LinkTime.keySet()) {
			// because of matsim wait2link times are always integer
			if (wait2linkTime < firstWait2Link)
				firstWait2Link = wait2linkTime.longValue();
			if (wait2linkTime > lastWait2Link)
				lastWait2Link = wait2linkTime.longValue();
		}
		for (long l = firstWait2Link; l <= lastWait2Link; l++) {
			if (!avgTTsPerRoutePerWait2LinkTime.containsKey((double) l)) {
				// add NaN-values as travel times when no agent has a wait2link
				double[] nanTTsPerRoute = { Double.NaN, Double.NaN, Double.NaN };
				avgTTsPerRoutePerWait2LinkTime.put((double) l, nanTTsPerRoute);
			}
		}

		return avgTTsPerRoutePerWait2LinkTime;
	}

	/**
	 * Calculates the average route travel times per arrival time.
	 * 
	 * Thereby the double array in each map entry contains the following average
	 * route travel times: 
	 * 0 - the average route travel time on the upper route.
	 * 1 - the average route travel time on the middle route.
	 * 2 - the average route travel time on the lower route.
	 * (always for agents with the specific arrival time)
	 * 
	 * @return
	 */
	public Map<Double, double[]> calculateAvgRouteTTsPerArrivalTime() {
		Map<Double, double[]> avgTTsPerRoutePerArrivalTime = new TreeMap<>();

		// calculate average route travel times for existing arrival times
		for (Double arrivalTime : this.totalRouteTTsPerArrivalTime.keySet()) {
			double[] totalTTsPerRoute = this.totalRouteTTsPerArrivalTime
					.get(arrivalTime);
			int[] usersPerRoute = this.routeUsersPerArrivalTime
					.get(arrivalTime);
			double[] avgTTsPerRoute = new double[3];
			for (int i = 0; i < 3; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is using the specific route at this arrival time
					avgTTsPerRoute[i] = Double.NaN;
				else
					avgTTsPerRoute[i] = totalTTsPerRoute[i] / usersPerRoute[i];
			}
			avgTTsPerRoutePerArrivalTime.put(arrivalTime, avgTTsPerRoute);
		}

		// fill missing time steps between first and last arrival
		long firstArrival = Long.MAX_VALUE;
		long lastArrival = Long.MIN_VALUE;
		for (Double arrivalTime : avgTTsPerRoutePerArrivalTime.keySet()) {
			// because of matsim arrival times are always integer
			if (arrivalTime < firstArrival)
				firstArrival = arrivalTime.longValue();
			if (arrivalTime > lastArrival)
				lastArrival = arrivalTime.longValue();
		}
		for (long l = firstArrival; l <= lastArrival; l++) {
			if (!avgTTsPerRoutePerArrivalTime.containsKey((double) l)) {
				// add NaN-values as travel times when no agent arrivals
				double[] nanTTsPerRoute = { Double.NaN, Double.NaN, Double.NaN };
				avgTTsPerRoutePerArrivalTime.put((double) l, nanTTsPerRoute);
			}
		}

		return avgTTsPerRoutePerArrivalTime;
	}

}
