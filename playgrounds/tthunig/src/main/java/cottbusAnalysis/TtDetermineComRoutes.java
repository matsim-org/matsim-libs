package cottbusAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author tthunig
 * @deprecated
 */
public class TtDetermineComRoutes implements PersonEntersVehicleEventHandler, Wait2LinkEventHandler, LinkEnterEventHandler{

	private Map<Id<Vehicle>, List<Id<Link>>> vehRoutes = new HashMap<>();
	private Map<Id<Person>, Id<Vehicle>> passengerId2VehId = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		vehRoutes = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		passengerId2VehId.put(event.getPersonId(), event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		// every vehicle departures only once in this scenario
		List<Id<Link>> route = new ArrayList<>();
		route.add(event.getLinkId());
		vehRoutes.put(event.getVehicleId(), route);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add entered link to the route of the vehicle
		vehRoutes.get(event.getVehicleId()).add(event.getLinkId());
	}

	public List<Id<Link>> getAgentRoute(Id<Person> agentId){
		return vehRoutes.get(passengerId2VehId.get(agentId));
	}

	public String getComIdOfAgent(Id<Person> agentId){
		// the first five digits of the personId are the commodityId
		return agentId.toString().substring(0, 5);
	}

}
