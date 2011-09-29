package freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityPickupsDeliveriesSequenceConstraint;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;
import vrp.api.VRP;
import vrp.basics.ManhattanDistance;
import vrp.basics.Tour;
import vrp.basics.VrpSolution;
import vrp.basics.VrpUtils;

public class RuinAndRecreateSolver implements VRPSolver {
	
	private Collection<Tour> tours;
	private VRPTransformation vrpTransformation;
	private VrpSolution vrpSolution = null;
	private Collection<Tour> initialSolution = null;
	private Collection<RecreationListener> recreationListeners = new ArrayList<RecreationListener>();
	private int nOfWarmUpIterations = 4;
	private int nOfIterations = 25;
 	
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
	public void solve(Collection<CarrierContract> contracts, CarrierVehicle carrierVehicle) {
		vrpTransformation.clear();
		vrpSolution = null;
		Id depotId = carrierVehicle.getLocation();
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		ManhattanDistance costs = new ManhattanDistance();
		costs.speed = 25;
		vrpBuilder.setCosts(costs);
		vrpBuilder.setConstraints(new CapacityPickupsDeliveriesSequenceConstraint(carrierVehicle.getCapacity()));
		for(CarrierContract c : contracts){
			CarrierShipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVRPTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVRP();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		rrFactory.setWarmUp(nOfWarmUpIterations);
		rrFactory.setIterations(nOfIterations);
		addListener(rrFactory);
		initialSolution = VrpUtils.createTrivialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		ruinAndRecreateAlgo.setWarmUpIterations(nOfWarmUpIterations);
		
		ruinAndRecreateAlgo.run();
		finishListener();
		tours.addAll(ruinAndRecreateAlgo.getSolution());
		vrpSolution = ruinAndRecreateAlgo.getVrpSolution();
	}

	public void setNofIterations(int nOfIterations) {
		this.nOfIterations = nOfIterations;
	}

	private void finishListener() {
		for(RecreationListener rl : recreationListeners){
			rl.finish();
		}
	}

	private void addListener(RuinAndRecreateFactory rrFactory) {
		for(RecreationListener rl : recreationListeners){
			rrFactory.addRecreationListener(rl);
		}
		
	}

	public VrpSolution getVrpSolution() {
		return vrpSolution;
	}

	public void addRecreationListener(RecreationListener recreationListener) {
		recreationListeners.add(recreationListener);
	}

	public void setNumberOfWarmUpIterations(int i) {
		this.nOfWarmUpIterations = i;
		
	}

	@Override
	public Collection<playground.mzilske.freight.Tour> solve() {
		// TODO Auto-generated method stub
		return null;
	}
}
