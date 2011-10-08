package freight.vrp;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.CarrierShipment;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.SingleDepotVRP;
import vrp.basics.SingleDepotVRPImpl;
import vrp.basics.VehicleType;

public class MatSimSingleDepotVRPBuilder {
	
	private Constraints constraints;
	
	private MatSim2VRPTransformation matsim2vrp;
	
	private Costs costs;
	
	private Id depot;
	
	private Id depotLocation;
	
	private VehicleType vehicleType;
	
	public MatSimSingleDepotVRPBuilder(Id depotId, Id depotLocationId, VehicleType vehicleType, MatSim2VRPTransformation matsim2vrp) {
		super();
		this.matsim2vrp = matsim2vrp;
		this.depot = depotId;
		this.depotLocation = depotLocationId;
		this.vehicleType = vehicleType;
	}
	
	/**
	 * Use this method to register a transport relation from a customer to the depot, i.e. pickup smth from customer and deliver it to the depot.
	 * @param carrierShipment
	 */
	public void addPickupForDepotShipment(CarrierShipment carrierShipment){
		if(!carrierShipment.getTo().equals(depotLocation)){
			throw new IllegalStateException("a pickupForDepot-shipment must have the depotLocation as destination (i.e. shipment.getTo() must return depotLocation)");
		}
		matsim2vrp.addPickupForDepotShipment(carrierShipment);
	}
	
	/**
	 * Use this method to register a transport relation from the depot to a customer, i.e. pickup smth in the depot to deliver it to a customer. 
	 * @param carrierShipment
	 */
	public void addDeliveryFromDepotShipment(CarrierShipment carrierShipment){
		if(!carrierShipment.getFrom().equals(depotLocation)){
			throw new IllegalStateException("a deliveryFromDepotShipment-shipment must have the depotLocation as origint (i.e. shipment.getFrom() must return depotLocation)");
		}
		matsim2vrp.addDeliveryFromDepotShipment(carrierShipment);
	}
	
	/**
	 * Use this method to register a transport relation occurring during the route, i.e. the pickup activity occurs after the vehicle has started from depot and 
	 * the delivery activity occurs before the vehicle heads back to the depot. In principle, even relation from the depot to a customer (and the other way around) can
	 * be formulated as enRoutePickupAndDelivery problem. This however is by far more complex than simple depot customer relations.  
	 * @param carrierShipment
	 */
	public void addEnRoutePickupAndDeliveryShipment(CarrierShipment carrierShipment){
		matsim2vrp.addEnRoutePickupAndDeliveryShipment(carrierShipment);
	}
	

	public MatSim2VRPTransformation getVrpTransformation() {
		return matsim2vrp;
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
	public SingleDepotVRP buildVRP(){
		verify();
		matsim2vrp.addAndCreateCustomer(depot, depotLocation, 0, 0.0, Double.MAX_VALUE, 0.0);
		SingleDepotVRPImpl vrp = new SingleDepotVRPImpl(depot.toString(), vehicleType, matsim2vrp.getCustomers(), costs, constraints);
		return vrp;
	}
	
	private void verify() {
		if(depot == null){
			throw new IllegalStateException("no depot set");
		}
		if(constraints == null){
			throw new IllegalStateException("no constraints set");
		}
		if(costs == null){
			throw new IllegalStateException("no costs set");
		}
		if(vehicleType == null){
			throw new IllegalStateException("no vehicleType set");
		}
	}

}
