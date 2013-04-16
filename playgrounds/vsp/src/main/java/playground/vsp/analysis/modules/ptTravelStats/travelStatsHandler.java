package playground.vsp.analysis.modules.ptTravelStats;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.scenario.ScenarioImpl;

public class travelStatsHandler implements LinkLeaveEventHandler {
	
	private Map<Id, Integer> vehicleCountPerLink;
	private Map<Id, Integer> capacityPerLink;
	private Map<Id, Double> capacityKmPerLink;
	private Map<Id, Double> paxPerLink;
	private Map<Id, Double> paxKmPerLink;

	private ScenarioImpl scenario;
	
	public travelStatsHandler(ScenarioImpl scenario) {
		this.scenario = scenario;
		this.vehicleCountPerLink = new HashMap<Id, Integer>();
		this.capacityPerLink = new HashMap<Id, Integer>();
		this.capacityKmPerLink = new HashMap<Id, Double>();
		this.paxPerLink = new HashMap<Id, Double>();
		this.paxKmPerLink = new HashMap<Id, Double>();
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {

		Id linkId = event.getLinkId();
		Coord fromCoord = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord();
		Coord toCoord = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
	
//		double bla = event.getTime() % 3600;
		
		calcVehicleCount(event);
		calcCapacityPerLink(event);
		calcPaxPerLink(event);
		
		
		
	}
	
	private void calcVehicleCount(LinkLeaveEvent event) {
		if (this.vehicleCountPerLink.get(event.getLinkId()) == null){
			this.vehicleCountPerLink.put(event.getLinkId(), 1);
		} else {
			int vehiclesSoFar = this.vehicleCountPerLink.get(event.getLinkId());
			int vehiclesAfterEvent = vehiclesSoFar++;
			this.vehicleCountPerLink.put(event.getLinkId(), vehiclesAfterEvent);
		}
	}

	private void calcCapacityPerLink(LinkLeaveEvent event) {
		int vehSeats = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getCapacity().getSeats();
		int vehStand = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getCapacity().getStandingRoom();
		int vehCap = vehSeats + vehStand;
		double linkLength_Km = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength() / 1000.;
		double vehCapKm = vehCap * linkLength_Km;
		if (this.capacityPerLink.get(event.getLinkId()) == null) {
			this.capacityPerLink.put(event.getLinkId(), vehCap);
		}
		else {
			int capSoFar = this.capacityPerLink.get(event.getLinkId());
			int capAfterEvent = capSoFar + vehCap;
			this.capacityPerLink.put(event.getLinkId(), capAfterEvent);
		}
		if (this.capacityKmPerLink.get(event.getLinkId()) == null) {
			this.capacityKmPerLink.put(event.getLinkId(), vehCapKm);
		}
		else {
			double capKmSoFar = this.capacityKmPerLink.get(event.getLinkId());
			double capKmAfterEvent = capKmSoFar + vehCapKm;
			this.capacityKmPerLink.put(event.getLinkId(), capKmAfterEvent);
		}
	}
	
	private void calcPaxPerLink(LinkLeaveEvent event) {
		
	}
	
}
