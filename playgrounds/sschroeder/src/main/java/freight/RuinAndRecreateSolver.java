package freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Shipment;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.VrpSolution;
import vrp.basics.VrpUtils;
import city2000w.VrpBuilder;

public class RuinAndRecreateSolver implements VRPSolver {
	
	private Collection<Tour> tours;
	private VRPTransformation vrpTransformation;
	private VrpSolution vrpSolution = null;
	private Collection<Tour> initialSolution = null;
	
	public void setInitialSolution(Collection<Tour> initialSolution) {
		this.initialSolution = initialSolution;
	}

	public RuinAndRecreateSolver(Collection<Tour> tours, VRPTransformation vrpTransformation) {
		super();
		this.tours = tours;
		this.vrpTransformation = vrpTransformation;
	}

	/* (non-Javadoc)
	 * @see freight.VRPSolver#solve(java.util.Collection, playground.mzilske.freight.CarrierVehicle)
	 */
	@Override
	public void solve(Collection<Contract> contracts, CarrierVehicle carrierVehicle) {
		vrpTransformation.clear();
		vrpSolution = null;
		Id depotId = carrierVehicle.getLocation();
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		vrpBuilder.setConstraints(new CapacityConstraint(carrierVehicle.getCapacity()));
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVrpTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVrp();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		if(initialSolution == null){
			initialSolution = VrpUtils.createTrivialSolution(vrp);
		}
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.run();
		tours.addAll(ruinAndRecreateAlgo.getSolution());
		vrpSolution = ruinAndRecreateAlgo.getVrpSolution();
	}

	public VrpSolution getVrpSolution() {
		return vrpSolution;
	}
}
