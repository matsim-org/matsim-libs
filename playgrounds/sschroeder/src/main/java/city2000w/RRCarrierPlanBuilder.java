package city2000w;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.Contract;
import playground.mzilske.freight.carrier.CarrierCapabilities;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierPlan;
import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierVehicle;
import playground.mzilske.freight.carrier.ScheduledTour;
import playground.mzilske.freight.carrier.Tour;
import freight.vrp.VRPSolver;
import freight.vrp.VRPSolverFactory;

public class RRCarrierPlanBuilder {

	private static Logger logger = Logger.getLogger(RRCarrierPlanBuilder.class);
	private CarrierCapabilities caps;
	private Collection<CarrierContract> contracts;
	private Network network;
	private VRPSolverFactory vrpSolverFactory;

	public RRCarrierPlanBuilder(CarrierCapabilities caps,Collection<CarrierContract> contracts,Network network) {
		super();
		this.caps=caps;
		this.contracts=contracts;
		this.network=network;
	}
	
	public void setVrpSolverFactory(VRPSolverFactory vrpSolverFactory) {
		this.vrpSolverFactory = vrpSolverFactory;
	}

	public CarrierPlan buildPlan(){
		if(contracts == null){
			return null;
		}
		if(contracts.isEmpty()){
			return null;
		}
		VRPSolver vrpSolver = vrpSolverFactory.createSolver(getShipments(contracts), getVehicles(caps), network);
		Collection<Tour> tours = vrpSolver.solve();
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
	
	private Collection<CarrierShipment> getShipments(Collection<CarrierContract> contracts) {
		Collection<CarrierShipment> shipments = new ArrayList<CarrierShipment>();
		for(Contract c : contracts){
			shipments.add((CarrierShipment)c.getShipment());
		}
		return shipments;
	}
}