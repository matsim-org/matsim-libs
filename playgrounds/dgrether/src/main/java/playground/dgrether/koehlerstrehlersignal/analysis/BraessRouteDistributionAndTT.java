/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Class to analyze a MATSim simulation of Braess' example.
 * 
 * It calculates the total travel time in the network, the total and average
 * travel times on the single routes and the number of users on each route.
 * Additionally it calculates the number of route starts per second and the
 * number of agents on each route per second.
 * 
 * @author tthunig
 * 
 */
public class BraessRouteDistributionAndTT implements PersonArrivalEventHandler,
		PersonDepartureEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler {

	private double totalTT;
	private double[] totalRouteTTs;
	private int[] routeUsers;

	private Map<Id<Person>, Double> personDepartureTimes;
	private Map<Id<Person>, Double> personRouteStartTime; // link enter of link
															// 2 or 3
	private Map<Id<Person>, Integer> personRouteChoice;
	private Map<Double, double[]> routeStartsPerSecond;
	private Map<Double, double[]> onRoutePerSecond;

	public BraessRouteDistributionAndTT() {
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
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = event.getPersonId();

		int route = -1;
		switch (event.getLinkId().toString()) {
		case "2":
			this.personRouteStartTime.put(personId, event.getTime());
			break;
		case "3": // the person uses the lower route
			route = 2;
			this.personRouteStartTime.put(personId, event.getTime());
			break;
		case "4": // the person uses the middle route
			route = 1;
			break;
		case "5": // the person uses the upper route
			route = 0;
			break;
		default:
			break;
		}

		if (route != -1) {
			// save route choice per person
			this.personRouteChoice.put(personId, route);

			// save starts per route per second
			Double personRouteStart = this.personRouteStartTime.get(personId);
			if (!this.routeStartsPerSecond.containsKey(personRouteStart)) {
				this.routeStartsPerSecond.put(personRouteStart, new double[3]);
			}
			this.routeStartsPerSecond.get(personRouteStart)[route]++;
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double personRouteEnd = Double.NaN;
		switch (event.getLinkId().toString()) {
		case "5":
		case "6":
			personRouteEnd = event.getTime();
			break;
		default:
			break;
		}

		if (!personRouteEnd.isNaN()) {
			Double personRouteStart = this.personRouteStartTime.get(event
					.getPersonId());
			Double timeOnRoute = personRouteEnd - personRouteStart;

			// increase the number of persons on route for each second the
			// person is traveling on it
			for (int i = 0; i < timeOnRoute; i++) {
				if (!this.onRoutePerSecond.containsKey(personRouteStart + i)) {
					this.onRoutePerSecond.put(personRouteStart + i,
							new double[3]);
				}
				this.onRoutePerSecond.get(personRouteStart + i)[this.personRouteChoice
						.get(event.getPersonId())]++;
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"A person has departured two times without arrival.");
		}
		this.personDepartureTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"A person has arrived without departure.");
		}
		if (!this.personRouteChoice.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"An arrived person was not seen on any route.");
		}

		double personStartTime = this.personDepartureTimes.get(event
				.getPersonId());
		double personTT = event.getTime() - personStartTime;
		int personRoute = this.personRouteChoice.get(event.getPersonId());
		this.totalTT += personTT;
		this.totalRouteTTs[personRoute] += personTT;
		this.routeUsers[personRoute]++;

		this.personDepartureTimes.remove(event.getPersonId());
		this.personDepartureTimes.remove(event.getPersonId());
	}

	/**
	 * Calculates and returns the average travel times on the single routes in
	 * Braess' example. The first entry corresponds to the upper route, the
	 * second to the middle route and the third to the lower route.
	 * 
	 * @return average travel times
	 */
	public double[] getAvgRouteTTs() {
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

	public Map<Double, double[]> getRouteStartsPerSecond() {
		// fill missing time steps between first departure and
		// last arrival with zero starts
		int firstStart = Integer.MAX_VALUE;
		int lastStart = Integer.MIN_VALUE;
		for (Double d : this.routeStartsPerSecond.keySet()) {
			if (d < firstStart)
				firstStart = d.intValue();
			if (d > lastStart)
				lastStart = d.intValue();
		}
		for (int i = firstStart; i <= lastStart; i++) {
			if (!this.routeStartsPerSecond.containsKey((double) i)) {
				this.routeStartsPerSecond.put((double) i, new double[3]);
			}
		}

		return routeStartsPerSecond;
	}

	public Map<Double, double[]> getOnRoutePerSecond() {
		// already contains entries for all time steps (seconds)
		// between first departure and last arrival
		return onRoutePerSecond;
	}

}
