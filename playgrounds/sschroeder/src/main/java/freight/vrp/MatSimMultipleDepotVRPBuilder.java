package freight.vrp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.CarrierShipment;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.MultipleDepotsVRP;
import vrp.api.VRP;
import vrp.basics.CrowFlyCosts;
import vrp.basics.Tour;
import vrp.basics.VRPWithMultipleDepotsImpl;
import vrp.basics.Vehicle;
import vrp.basics.VehicleType;

public class MatSimMultipleDepotVRPBuilder {
	
	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour, Vehicle vehicle) {
			return true;
		}
		
	};
	
	private MatSim2VRPTransformation vrpTrafo;
	
	private Costs costs = new CrowFlyCosts();
	
	private List<Id> depots = new ArrayList<Id>();
	
	private List<Id> depotLocations = new ArrayList<Id>();
	
	private Map<Id,VehicleType> types = new HashMap<Id, VehicleType>();
	
	public MatSimMultipleDepotVRPBuilder(MatSim2VRPTransformation vrpTrafo) {
		super();
		this.vrpTrafo = vrpTrafo;
	}
	
	/**
	 * Adds a depot to the vehicle routing problem and with that a certain vehicle type (currently differing in capacity only). Note, that each depot requires exactly
	 * one vehicle type.
	 * @param depotId
	 * @param depotLocationId
	 * @param type
	 */
	public void addDepotAndVehicleType(Id depotId, Id depotLocationId, VehicleType type){
		depots.add(depotId);
		depotLocations.add(depotLocationId);
		types.put(depotId, type);
	}
	
	/**
	 * Use this method to register a transport relation from a customer to the depot, i.e. pickup smth from customer and deliver it to the depot.
	 * @param carrierShipment
	 */
	public void addPickupForDepotRelation(CarrierShipment carrierShipment){
		vrpTrafo.addPickupForDepotShipment(carrierShipment);
	}
	
	/**
	 * Use this method to register a transport relation from the depot to a customer, i.e. pickup smth in the depot to deliver it to a customer. 
	 * @param carrierShipment
	 */
	public void addDeliveryFromDepot(CarrierShipment carrierShipment){
		vrpTrafo.addDeliveryFromDepotShipment(carrierShipment);
	}
	
	/**
	 * Use this method to register a transport relation occurring during the route, i.e. the pickup activity occurs after the vehicle has started from depot and 
	 * the delivery activity occurs before the vehicle heads back to the depot. In principle, even relation from the depot to a customer (and the other way around) can
	 * be formulated as enRoutePickupAndDelivery problem. This however is by far more complex than simple depot customer relations.  
	 * @param carrierShipment
	 */
	public void addEnRoutePickupAndDeliveryRelation(CarrierShipment carrierShipment){
		vrpTrafo.addEnRoutePickupAndDeliveryShipment(carrierShipment);
	}
	

	public MatSim2VRPTransformation getVrpTransformation() {
		return vrpTrafo;
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
	public VRP buildVRP(){
		verify();
		for(int i=0;i<depots.size();i++){
			vrpTrafo.addAndCreateCustomer(depots.get(i), depotLocations.get(i), 0, 0.0, Double.MAX_VALUE, 0.0);
		}
		List<String> depots_ = makeDepotList();
		VRPWithMultipleDepotsImpl vrp = new VRPWithMultipleDepotsImpl(depots_, vrpTrafo.getCustomers(), costs, constraints);
		for(Id id : types.keySet()){
			vrp.assignVehicleTypeToDepot(id.toString(), types.get(id));
		}
		assertEachDepotHasVehicleType(vrp);
		return vrp;
	}
	
	private List<String> makeDepotList() {
		List<String> depots_ = new ArrayList<String>();
		for(Id id : depots){
			depots_.add(id.toString());
		}
		return depots_;
	}

	private void assertEachDepotHasVehicleType(MultipleDepotsVRP vrp) {
		for(Id id : depots){
			VehicleType type = vrp.getVehicleType(id.toString());
			if(type == null){
				throw new IllegalStateException("each depot must have one vehicleType. Depot " + id + " does not have!");
			}
		}
		
	}
	
	private void verify() {
		if(depots.isEmpty()){
			throw new IllegalStateException("at least one depot must be set");
		}
		if(vrpTrafo == null){
			throw new IllegalStateException("vrpTrafo not set");
		}
		
	}

	public void setVRPTransformation(MatSim2VRPTransformation vrpTrafo) {
		this.vrpTrafo = vrpTrafo;
	}

}
