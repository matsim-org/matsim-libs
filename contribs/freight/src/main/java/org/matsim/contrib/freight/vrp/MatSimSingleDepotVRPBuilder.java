package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
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

public class MatSimSingleDepotVRPBuilder {
	
	private Constraints constraints;
	
	private MatSim2VRP matsim2vrpRecorder;
	
	private Costs costs;
	
	private String depotLocationId;
	
	private Collection<Vehicle> vehicles;
	
	private Integer shipmentIdCounter = 1;

	public MatSimSingleDepotVRPBuilder(Id depotLocationId, Collection<CarrierVehicle> vehicles, MatSim2VRP matsim2vrpRecorder, 
			Network network) {
		super();
		this.matsim2vrpRecorder = matsim2vrpRecorder;
		this.depotLocationId = depotLocationId.toString();
		makeVehicles(vehicles);
	}
	
	private void makeVehicles(Collection<CarrierVehicle> vehicles) {
		for(CarrierVehicle v : vehicles){
			VrpUtils.createVehicle(v.getVehicleId().toString(), v.getLocation().toString(), v.getCapacity());
			vehicles.add(v);
		}
	}

	public void addShipment(CarrierShipment carrierShipment, double pickupServiceTime, double deliveryServiceTime){
		Shipment vrpShipment = buildVrpShipment(carrierShipment,pickupServiceTime,deliveryServiceTime);
		matsim2vrpRecorder.addShipmentEntry(carrierShipment, vrpShipment);
	}
		
	private Shipment buildVrpShipment(CarrierShipment carrierShipment, double pickupServiceTime, double deliveryServiceTime) {
		String id = makeId();
		Shipment s = VrpUtils.createShipment(id, carrierShipment.getFrom().toString(),carrierShipment.getTo().toString(),
				carrierShipment.getSize(), makeTW(carrierShipment.getPickupTimeWindow()),makeTW(carrierShipment.getDeliveryTimeWindow()));
		s.setPickupServiceTime(pickupServiceTime);
		s.setDeliveryServiceTime(deliveryServiceTime);
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
		VRPImpl vrp = new VRPImpl(matsim2vrpRecorder.getVrpShipments(), vehicles, 
				costs, constraints);
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
