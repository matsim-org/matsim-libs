package freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;


import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Shipment;
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
	public void solve(Collection<Contract> contracts, CarrierVehicle carrierVehicle) {
		Id depotId = findDepotId(contracts);
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		vrpBuilder.setConstraints(new ClarkeWrightCapacityConstraint(carrierVehicle.getCapacity()));
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVrpTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVrp();
		ClarkeAndWright clarkAndWright = new ClarkeAndWright(vrp);
		clarkAndWright.run();
		tours.addAll(clarkAndWright.getSolution());
	}
	
	private Id findDepotId(Collection<Contract> contracts) {
		for(Contract c : contracts){
			return c.getShipment().getFrom();
		}
		throw new RuntimeException("no contracts or shipments");
	}

}
