package playground.sergioo.eventAnalysisTools2013.missedServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;

public class MissedServices implements VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, AgentArrivalEventHandler {

	private class PersonInfo {
	
		private final Id id;
		private final Collection<Id> lines = new ArrayList<Id>();
		private final Id stopId;
		private Integer numMissed;
	
		public PersonInfo(Id id, Id stopId, Integer numMissed) {
			super();
			this.id = id;
			this.stopId = stopId;
			this.numMissed = numMissed;
		}
		public void addLine(Id lineId) {
			lines.add(lineId);
		}
	
	}

	private final Map<Id, Tuple<Id, Id>> linesRoutesNumVehicle = new HashMap<Id, Tuple<Id, Id>>();
	private Map<Id, PersonInfo> timeOfStopLineRoute = new HashMap<Id, PersonInfo>();
	private Map<Id, Id> stopOfVehicle = new HashMap<Id, Id>();

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
