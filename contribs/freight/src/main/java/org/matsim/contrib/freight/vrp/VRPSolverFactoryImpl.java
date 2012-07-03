package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VrpType;

public class VRPSolverFactoryImpl implements VRPSolverFactory{
	
	private RuinAndRecreateFactory rrFactory;
	
	private VrpType vrpType;

	public VRPSolverFactoryImpl(RuinAndRecreateFactory rrFactory, VrpType vrpType) {
		super();
		this.rrFactory = rrFactory;
		this.vrpType = vrpType;
	}

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles, Network network, TourCost tourCost, VehicleRoutingCosts costs) {
		verifyVehicleRouteProblem(shipments,carrierVehicles);
		MatsimVrpSolver rrSolver = new MatsimVrpSolver(shipments, carrierVehicles, costs);
		rrSolver.setRuinAndRecreateFactory(rrFactory);
		return rrSolver;
	}

	protected void verifyVehicleRouteProblem(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles) {
		if(vrpType.equals(VrpType.SINGLE_DEPOT_DISTRIBUTION)){
			Id location = null;
			for(CarrierVehicle v : carrierVehicles){
				if(location == null){
					location = v.getLocation();
				}
				else if(!location.toString().equals(v.getLocation().toString())){
					throw new IllegalStateException("if you use this solver " + this.getClass().toString() + "), all vehicles must have the same depot-location. vehicle " + v.getVehicleId() + " has not.");
				}
			}
			for(CarrierShipment s : shipments){
				if(location == null){
					return;
				}
				if(!s.getFrom().toString().equals(location.toString())){
					throw new IllegalStateException("if you use this solver, all shipments must have the same from-location. errorShipment " + s);
				}
			}
		}
		else{
			throw new IllegalStateException("this problem type is not yet supported");
		}
		
	}
	
	

}
