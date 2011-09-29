package freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import vrp.algorithms.clarkeAndWright.ClarkeAndWright;
import vrp.algorithms.clarkeAndWright.ClarkeWrightCapacityConstraint;
import vrp.api.VRP;
import vrp.basics.Tour;

public class ClarkeAndWrightSolver implements VRPSolver {

	private Collection<Tour> tours;
	private VRPTransformation vrpTransformation;
		
	public ClarkeAndWrightSolver(Collection<Tour> tours,
			VRPTransformation vrpTransformation) {
		super();
		this.tours = tours;
		this.vrpTransformation = vrpTransformation;
	}

	@Override
	public void solve(Collection<CarrierContract> contracts, CarrierVehicle carrierVehicle) {
		Id depotId = findDepotId(contracts);
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		vrpBuilder.setConstraints(new ClarkeWrightCapacityConstraint(carrierVehicle.getCapacity()));
		for(CarrierContract c : contracts){
			CarrierShipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVRPTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVRP();
		ClarkeAndWright clarkAndWright = new ClarkeAndWright(vrp);
		clarkAndWright.run();
		tours.addAll(clarkAndWright.getSolution());
	}
	
	private Id findDepotId(Collection<CarrierContract> contracts) {
		for(CarrierContract c : contracts){
			return c.getShipment().getFrom();
		}
		throw new RuntimeException("no contracts or shipments");
	}

	@Override
	public Collection<playground.mzilske.freight.Tour> solve() {
		// TODO Auto-generated method stub
		return null;
	}

}
