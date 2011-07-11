package city2000w;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.VrpImpl;
import freight.VRPTransformation;

public class VrpBuilder {
	
	private Id depotLocationId;
	
	private Constraints constraints;
	
	private VRPTransformation vrpTrafo;
	
	private Costs costs = new CrowFlyDistance();
	
	public VrpBuilder(Id depotId) {
		super();
		this.depotLocationId = depotId;
	}

	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public VRP buildVrp(){
		String depotId = "depot";
		vrpTrafo.addAndCreateCustomer(depotId, depotLocationId, 0, 0.0, 24*3600, 0.0);
		VRP vrp = new VrpImpl(makeId(depotId), vrpTrafo.getCustomers(), costs, constraints);
		return vrp;
	}
	
	private Id makeId(String depotId) {
		return new IdImpl(depotId);
	}

	public void setVrpTransformation(VRPTransformation vrpTrafo) {
		this.vrpTrafo = vrpTrafo;
	}

}
