package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import freight.vrp.RRSolver;
import freight.vrp.VRPSolver;

class RRCarrierPlanBuilder {

	private static Logger logger = Logger.getLogger(RRCarrierPlanBuilder.class);
	private CarrierCapabilities caps;
	private Collection<Contract> contracts;
	private Network network;

	public RRCarrierPlanBuilder(CarrierCapabilities caps,Collection<Contract> contracts,Network network) {
		super();
		this.caps=caps;
		this.contracts=contracts;
		this.network=network;
	}
	
	public CarrierPlan buildPlan(){
		if(contracts == null){
			return null;
		}
		if(contracts.isEmpty()){
			return null;
		}
		VRPSolver vrpSolver = new RRSolver(getShipments(contracts), getVehicles(caps), network);
		Collection<Tour> tours = vrpSolver.solve();
		logger.info(tours.size());

		Collection<ScheduledTour> scheduledTours = makeScheduledTours(tours);
		return new CarrierPlan(scheduledTours);
	}
	
	private Collection<ScheduledTour> makeScheduledTours(Collection<Tour> tours) {
		Collection<ScheduledTour> sTours = new ArrayList<ScheduledTour>();
		for(Tour t : tours){
			sTours.add(new ScheduledTour(t, getVehicle(t,caps), 0.0));
		}
		return sTours;
	}
	
	private CarrierVehicle getVehicle(Tour t, CarrierCapabilities caps) {
		Id locationId = t.getStartLinkId();
		CarrierVehicle cV = null;
		for(CarrierVehicle vehicle : caps.getCarrierVehicles()){
			if(vehicle.getLocation().equals(locationId)){
				cV = vehicle;
			}
		}
		return cV;
	}
	
	private Collection<CarrierVehicle> getVehicles(CarrierCapabilities caps) {
		return new ArrayList<CarrierVehicle>(caps.getCarrierVehicles());
	}
	
	private Collection<Shipment> getShipments(Collection<Contract> contracts) {
		Collection<Shipment> shipments = new ArrayList<Shipment>();
		for(Contract c : contracts){
			shipments.add(c.getShipment());
		}
		return shipments;
	}
}