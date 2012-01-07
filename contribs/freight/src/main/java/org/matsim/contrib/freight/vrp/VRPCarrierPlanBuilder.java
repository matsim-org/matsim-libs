package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Contract;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.vrp.basics.Costs;

public class VRPCarrierPlanBuilder {
	
	private CarrierCapabilities caps;
	private Collection<CarrierContract> contracts;
	private Network network;
	private VRPSolverFactory vrpSolverFactory;
	private Costs costs;

	public VRPCarrierPlanBuilder(CarrierCapabilities caps, Collection<CarrierContract> contracts, Network network, Costs costs) {
		super();
		this.costs = costs;
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
		VRPSolver vrpSolver = vrpSolverFactory.createSolver(getShipments(contracts), getVehicles(caps), network, costs);
		Collection<Tour> tours = vrpSolver.solve();
		Collection<ScheduledTour> scheduledTours = makeScheduledTours(tours);
		return new CarrierPlan(scheduledTours);
	}
	
	private Collection<ScheduledTour> makeScheduledTours(Collection<Tour> tours) {
		Collection<ScheduledTour> sTours = new ArrayList<ScheduledTour>();
		for(Tour t : tours){
			sTours.add(new ScheduledTour(t, getVehicle(t,caps), t.getEarliestDeparture()));
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