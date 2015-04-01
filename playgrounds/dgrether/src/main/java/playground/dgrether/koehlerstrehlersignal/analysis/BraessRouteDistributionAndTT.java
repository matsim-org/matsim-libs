/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Class to analyze a MATSim simulation of Braess' example.
 * 
 * It calculates the total travel time in the network, the total and average 
 * travel times on the single routes and the number of users on each route.
 * 
 * @author tthunig
 *
 */
public class BraessRouteDistributionAndTT implements PersonArrivalEventHandler,
		PersonDepartureEventHandler, LinkEnterEventHandler {

	private double totalTT;
	private double[] totalRouteTTs;
	private int[] routeUsers;
	
	private Map<Id<Person>, Double> personDepartureTimes;
	private Map<Id<Person>, Integer> personRouteChoice;
	
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
		this.personRouteChoice = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		switch (event.getLinkId().toString()){
		case "3": // the person uses the lower route
			this.personRouteChoice.put(event.getPersonId(), 2);
			break;
		case "4": // the person uses the middle route
			this.personRouteChoice.put(event.getPersonId(), 1);
			break;
		case "5": // the person uses the upper route
			this.personRouteChoice.put(event.getPersonId(), 0);
			break;
		default: break;	
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personDepartureTimes.containsKey(event.getPersonId())){
			throw new IllegalStateException("A person has departured two times without arrival.");
		}
		this.personDepartureTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!this.personDepartureTimes.containsKey(event.getPersonId())){
			throw new IllegalStateException("A person has arrived without departure.");
		}
		if (!this.personRouteChoice.containsKey(event.getPersonId())){
			throw new IllegalStateException("An arrived person was not seen on any route.");
		}
		
		double personTT = event.getTime() - this.personDepartureTimes.get(event.getPersonId());
		int personRoute = this.personRouteChoice.get(event.getPersonId());
		this.totalTT += personTT;
		this.totalRouteTTs[personRoute] += personTT;
		this.routeUsers[personRoute]++;
		
		this.personDepartureTimes.remove(event.getPersonId());
		this.personDepartureTimes.remove(event.getPersonId());
	}
	
	/**
	 * Calculates and returns the average travel times on the single routes in Braess' example.
	 * The first entry corresponds to the upper route, the second to the middle route and the 
	 * third to the lower route.
	 * 
	 * @return average travel times
	 */
	public double[] getAvgRouteTTs(){
		double[] avgRouteTTs = new double[3];
		for (int i=0; i<3; i++){
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

}
