/**
 * 
 */
package scenarios.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Abstract tool to analyze a MATSim simulation of an arbitrary scenario.
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
 * Note: To use this analyzer you have to implement the abstract methods. I.e.
 * you have to define the number of different routes of your specific scenario
 * and implement a unique route determination via link enter events.
 * 
 * Note: This class calculates travel times via departure and arrival events.
 * I.e. it only gives reliable results if all agents can departure without delay
 * regarding to the flow capacity of the first link. If they are delayed because
 * of storage capacity the results are still fine.
 * 
 * The results may be plotted by gnuplot scripts (see e.g.
 * runs-svn/braess/analysis).
 * 
 * @author tthunig, tschlenther
 */
public abstract class TtAbstractAnalysisTool implements PersonArrivalEventHandler,
PersonDepartureEventHandler, LinkEnterEventHandler, PersonStuckEventHandler{

	private static final Logger log = Logger.getLogger(TtAbstractAnalysisTool.class);
	
	private double totalTT;
	private double[] totalRouteTTs;
	private int[] routeUsers;
	private int numberOfStuckedAgents = 0;

	// collects the departure times per person
	private Map<Id<Person>, Double> personDepartureTimes;
	
	// collects information about the used route per person
	private Map<Id<Person>, Integer> personRouteChoice;
	// counts the number of route starts per second (gets filled when the agent
	// arrives)
	private Map<Double, double[]> routeStartsPerSecond;
	// counts the number of agents on each route per second
	private Map<Double, double[]> onRoutePerSecond;

	private Map<Double, double[]> totalRouteTTsByDepartureTime;
	private Map<Double, int[]> routeUsersByDepartureTime;
	
	private int numberOfRoutes;

	public TtAbstractAnalysisTool() {
		super();
		reset(0);
		defineNumberOfRoutes();
	}
	
	/**
	 * resets all fields
	 */
	@Override
	public void reset(int iteration) {
		this.totalTT = 0.0;
		this.totalRouteTTs = new double[numberOfRoutes];
		this.routeUsers = new int[numberOfRoutes];
		this.numberOfStuckedAgents = 0;
		
		this.personDepartureTimes = new HashMap<>();
		this.personRouteChoice = new HashMap<>();
		this.routeStartsPerSecond = new TreeMap<>();
		this.onRoutePerSecond = new TreeMap<>();

		this.totalRouteTTsByDepartureTime = new TreeMap<>();
		this.routeUsersByDepartureTime = new TreeMap<>();
	}
	
	/**
	 * Defines the field variable numberOfRoutes for the specific scenario by
	 * using the setter method.
	 */
	protected abstract void defineNumberOfRoutes();

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
	 * Determines the agents route choice.
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int route = determineRoute(event);

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
	 * Determines the agents route choice if it is unique.
	 * 
	 * @return the route id (counts from 0 to numberOfRoutes)
	 */
	protected abstract int determineRoute(LinkEnterEvent linkEnterEvent);
	
	
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

		// calculate total travel time
		double personArrivalTime = event.getTime();
		double personDepartureTime = this.personDepartureTimes.get(event
				.getPersonId());
		double personTotalTT = personArrivalTime - personDepartureTime;
		this.totalTT += personTotalTT;

		// store route specific information
		int personRoute = this.personRouteChoice.get(event.getPersonId());
		this.totalRouteTTs[personRoute] += personTotalTT;
		this.routeUsers[personRoute]++;

		// fill maps for calculating avg tt per route
		if (!this.totalRouteTTsByDepartureTime.containsKey(personDepartureTime)) {
			// this is equivalent to
			// !this.routeUsersPerDepartureTime.containsKey(personDepartureTime)
			this.totalRouteTTsByDepartureTime.put(personDepartureTime, new double[3]);
			this.routeUsersByDepartureTime.put(personDepartureTime, new int[3]);
		}
		this.totalRouteTTsByDepartureTime.get(personDepartureTime)[personRoute] += personTotalTT;
		this.routeUsersByDepartureTime.get(personDepartureTime)[personRoute]++;

		// increase the number of persons on route for each second the
		// person is traveling on it
		for (int i = 0; i < personTotalTT; i++) {
			if (!this.onRoutePerSecond.containsKey(personDepartureTime + i)) {
				this.onRoutePerSecond.put(personDepartureTime + i, new double[3]);
			}
			this.onRoutePerSecond.get(personDepartureTime + i)[this.personRouteChoice
					.get(event.getPersonId())]++;
		}

		// add one route start for the specific departure time
		if (!this.routeStartsPerSecond.containsKey(personDepartureTime)) {
			this.routeStartsPerSecond.put(personDepartureTime, new double[3]);
		}
		this.routeStartsPerSecond.get(personDepartureTime)[personRoute]++;

		// remove all trip dependent information of the arrived person
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("Agent " + event.getPersonId() + " stucked on link " + event.getLinkId());
		if (numberOfStuckedAgents == 0){
			log.warn("This handler counts stucked agents but doesn't consider its travel times or route choice.");
		}
		
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
		
		numberOfStuckedAgents++;
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
	 * @return a map containing the number of route starts for each time step.
	 * Thereby a route start is the departure event of an agent using this route.
	 */
	public Map<Double, double[]> getRouteDeparturesPerSecond() {
		// fill missing time steps between first and last departure with zero starts
		long firstStart = Long.MAX_VALUE;
		long lastStart = Long.MIN_VALUE;
		for (Double d : this.routeStartsPerSecond.keySet()) {
			// matsim departure start times are always integer
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
	 * @return the number of agents on route (between departure and arrival
	 * event) per time step.
	 */
	public Map<Double, double[]> getOnRoutePerSecond() {
		// already contains entries for all time steps (seconds)
		// between first departure and last arrival
		return onRoutePerSecond;
	}

	/**
	 * @return the average route travel times by departure time.
	 * 
	 * Thereby the double array in each map entry contains the following average
	 * route travel times: 
	 * 0 - the average route travel time on the upper route.
	 * 1 - the average route travel time on the middle route.
	 * 2 - the average route travel time on the lower route.
	 * (always for agents with the specific departure time)
	 */
	public Map<Double, double[]> calculateAvgRouteTTsByDepartureTime() {
		Map<Double, double[]> avgTTsPerRouteByDepartureTime = new TreeMap<>();

		// calculate average route travel times for existing departure times
		for (Double departureTime : this.totalRouteTTsByDepartureTime.keySet()) {
			double[] totalTTsPerRoute = this.totalRouteTTsByDepartureTime
					.get(departureTime);
			int[] usersPerRoute = this.routeUsersByDepartureTime
					.get(departureTime);
			double[] avgTTsPerRoute = new double[3];
			for (int i = 0; i < 3; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is departing for the specific route at this time
					avgTTsPerRoute[i] = Double.NaN;
				else
					avgTTsPerRoute[i] = totalTTsPerRoute[i] / usersPerRoute[i];
			}
			avgTTsPerRouteByDepartureTime.put(departureTime, avgTTsPerRoute);
		}

		// fill missing time steps between first and last departure
		long firstDeparture = Long.MAX_VALUE;
		long lastDeparture = Long.MIN_VALUE;
		for (Double departureTime : avgTTsPerRouteByDepartureTime.keySet()) {
			// matsim departure times are always integer
			if (departureTime < firstDeparture)
				firstDeparture = departureTime.longValue();
			if (departureTime > lastDeparture)
				lastDeparture = departureTime.longValue();
		}
		for (long l = firstDeparture; l <= lastDeparture; l++) {
			if (!avgTTsPerRouteByDepartureTime.containsKey((double) l)) {
				// add NaN-values as travel times when no agent has a departure
				double[] nanTTsPerRoute = { Double.NaN, Double.NaN, Double.NaN };
				avgTTsPerRouteByDepartureTime.put((double) l, nanTTsPerRoute);
			}
		}

		return avgTTsPerRouteByDepartureTime;
	}

	public int getNumberOfStuckedAgents() {
		return numberOfStuckedAgents;
	}

	public void setNumberOfRoutes(int numberOfRoutes) {
		this.numberOfRoutes = numberOfRoutes;
	}

	public int getNumberOfRoutes() {
		return numberOfRoutes;
	}
}
