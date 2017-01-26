package playground.sergioo.mixedtraffic2016;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import playground.sergioo.mixedtraffic2016.gui.Road;

public class RoadVehiclesAnalyzer  implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler{

	public Map<Id<Vehicle>, Road.Vehicle> vehicles =  new HashMap<>();
	private Id<Link> linkId;
	private Map<Id<Vehicle>, String> modes = new HashMap<>();
	
	public RoadVehiclesAnalyzer(Id<Link> linkId) {
		this.linkId = linkId;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		modes.put(event.getVehicleId(), event.getNetworkMode());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(linkId))
			vehicles.get(event.getVehicleId()).setOutTime(event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getLinkId().equals(linkId))
			vehicles.put(event.getVehicleId(), new Road.Vehicle(modes.get(event.getVehicleId()), event.getTime()));
	}

}
