package freight.vrp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.Tour;
import vrp.basics.VRPWithMultipleDepotsAndVehiclesImpl;
import vrp.basics.VRPWithMultipleDepotsImpl;
import vrp.basics.Vehicle;
import vrp.basics.VehicleType;

public class VRPWithMultipleDepotsBuilder {
	
	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour) {
			return true;
		}

		@Override
		public boolean judge(Tour tour, Vehicle vehicle) {
			return true;
		}
		
	};
	
	private VRPTransformation vrpTrafo;
	
	private Costs costs = new CrowFlyDistance();
	
	private List<Id> depots = new ArrayList<Id>();
	
	private List<Id> depotLocations = new ArrayList<Id>();
	
	private Map<Id,VehicleType> types = new HashMap<Id, VehicleType>();
	
	public void addDepot(Id depotId, Id depotLocationId){
		depots.add(depotId);
		depotLocations.add(depotLocationId);
	}
	
	public void assignVehicleType(Id depotId, VehicleType type){
		types.put(depotId, type);
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public VRP buildVRP(){
		verify();
		for(int i=0;i<depots.size();i++){
			vrpTrafo.addAndCreateCustomer(depots.get(i), depotLocations.get(i), 0, 0.0, Double.MAX_VALUE, 0.0);
		}
		List<String> depots_ = makeDepotList();
		VRPWithMultipleDepotsAndVehiclesImpl vrp = new VRPWithMultipleDepotsAndVehiclesImpl(depots_, vrpTrafo.getCustomers(), costs, constraints);
		for(Id id : types.keySet()){
			vrp.assignVehicleType(id.toString(), types.get(id));
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

	private void assertEachDepotHasVehicleType(VRP vrp) {
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

	public void setVRPTransformation(VRPTransformation vrpTrafo) {
		this.vrpTrafo = vrpTrafo;
	}

}
