/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Class to read selected MATSim routes per agent from the events file 
 * to convert them into KS format.
 * Requires an event file with a single trip per agent (dummy-dummy trip).
 * 
 * @author tthunig
 */
public class ReadRoutesFromEvents implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler{

	private static final Logger log = Logger.getLogger(ReadRoutesFromEvents.class);
	
	private Map<Id<Person>, List<Id<Link>>> matsimRoutes;

	private Map<Id<Vehicle>, Set<Id<Person>>> vehicleId2PassengerIds = new HashMap<>();
	
	public ReadRoutesFromEvents() {
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.matsimRoutes = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		if (!this.matsimRoutes.containsKey(personId)){
			this.matsimRoutes.put(personId, new ArrayList<Id<Link>>());
		}
		else{
			log.error("Person was already seen.");
		}
		// add the link to the route
		this.matsimRoutes.get(personId).add(event.getLinkId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!vehicleId2PassengerIds.containsKey(event.getVehicleId())){
			vehicleId2PassengerIds.put(event.getVehicleId(), new HashSet<Id<Person>>());
		}
		vehicleId2PassengerIds.get(event.getVehicleId()).add(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (Id<Person> passengerId : vehicleId2PassengerIds.get(event.getVehicleId())){
			if (!this.matsimRoutes.containsKey(passengerId)){
				log.error("Person wasn't seen before.");
			}
			// add the link to the route
			this.matsimRoutes.get(passengerId).add(event.getLinkId());
		}		
	}

	public Map<Id<Person>, List<Id<Link>>> getMatsimRoutes() {
		return this.matsimRoutes;
	}
	
}
