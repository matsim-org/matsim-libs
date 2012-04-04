package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Contract;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class VRPCarrierPlanBuilder {
	
	private static Logger logger = Logger.getLogger(VRPCarrierPlanBuilder.class);
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
		Collection<ScheduledTour> scheduledTours = vrpSolver.solve();
//		route(scheduledTours);
		return new CarrierPlan(scheduledTours);
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