package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.carrier.CarrierPlan;
import vrp.api.Customer;
import vrp.api.SingleDepotVRP;
import vrp.api.VRP;
import vrp.basics.SingleDepotInitialSolutionFactory;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

public class PlanInitialSolutionFactory implements SingleDepotInitialSolutionFactory{

	private VRP vrp;
	
	private MatSim2VRPTransformation vrpTransformation;
	
	private CarrierPlan carrierPlan;
	
	public PlanInitialSolutionFactory(VRP vrp, MatSim2VRPTransformation vrpTransformation, CarrierPlan carrierPlan) {
		super();
		this.vrp = vrp;
		this.vrpTransformation = vrpTransformation;
		this.carrierPlan = carrierPlan;
	}

	@Override
	public Collection<Tour> createInitialSolution(SingleDepotVRP vrp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tour createRoundTour(SingleDepotVRP vrp, Customer from, Customer to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vehicle createVehicle(SingleDepotVRP vrp, Tour tour) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
