package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.vrp.basics.Constraints;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VRPImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;

/**
 * This helps building the vehicle routing problem. Basically, it translates carrier-shipments to vrp-shipments (to what is 
 * required by the routing algo) and memories this translation in MatSim2VRP for re-transformation.
 * @author stefan
 *
 */

public class MatSimVRPBuilder {
	
	private Constraints constraints;
	
	private MatSim2VRP matsim2vrpRecorder;
	
	private Costs costs;
	
	private Collection<Vehicle> vehicles;
	
	private Integer shipmentIdCounter = 1;

	public MatSimVRPBuilder(Collection<CarrierVehicle> vehicles, MatSim2VRP matsim2vrpRecorder, Network network) {
		super();
		this.matsim2vrpRecorder = matsim2vrpRecorder;
		makeRoutingVehicles(vehicles);
	}
	
	private void makeRoutingVehicles(Collection<CarrierVehicle> vehicles) {
		this.vehicles = new ArrayList<Vehicle>(); 
		for(CarrierVehicle v : vehicles){
			Vehicle vrpVehicle = VrpUtils.createVehicle(v.getVehicleId().toString(), v.getLocation().toString(), v.getCapacity());
			this.vehicles.add(vrpVehicle);
		}
	}

	/**
	 * By adding a carrierShipment, a vrp-shipment is created (as input for the router). This translation is memorized
	 * for re-translation.
	 * @param carrierShipment
	 */
	public void addShipment(CarrierShipment carrierShipment){
		Shipment vrpShipment = createVrpShipment(carrierShipment);
		matsim2vrpRecorder.addShipmentEntry(carrierShipment, vrpShipment);
	}
		
	private Shipment createVrpShipment(CarrierShipment carrierShipment) {
		String id = makeId();
		Shipment s = VrpUtils.createShipment(id, carrierShipment.getFrom().toString(),carrierShipment.getTo().toString(),
				carrierShipment.getSize(), makeTW(carrierShipment.getPickupTimeWindow()),makeTW(carrierShipment.getDeliveryTimeWindow()));
		s.setPickupServiceTime(carrierShipment.getPickupServiceTime());
		s.setDeliveryServiceTime(carrierShipment.getDeliveryServiceTime());
		return s;
	}

	private String makeId() {
		String id = shipmentIdCounter.toString();
		shipmentIdCounter++;
		return id;
	}

	private org.matsim.contrib.freight.vrp.basics.TimeWindow makeTW(TimeWindow timeWindow) {
		return VrpUtils.createTimeWindow(timeWindow.getStart(), timeWindow.getEnd());
	}

	/**
	 * 
	 * @param constraints
	 */
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	/**
	 * 
	 * @param costs
	 */
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	/**
	 * 
	 * @return
	 */
	public VehicleRoutingProblem buildVRP(){
		verify();
		VRPImpl vrp = new VRPImpl(matsim2vrpRecorder.getVrpShipments(), vehicles, costs, constraints);
		return vrp;
	}

	private void verify() {
		if(constraints == null){
			throw new IllegalStateException("no constraints set");
		}
		if(costs == null){
			throw new IllegalStateException("no costs set");
		}
	}


}
