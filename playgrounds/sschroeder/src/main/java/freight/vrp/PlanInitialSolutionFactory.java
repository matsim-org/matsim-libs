package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.carrier.CarrierPlan;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.InitialSolutionFactory;
import vrp.basics.Tour;
import vrp.basics.Vehicle;

public class PlanInitialSolutionFactory implements InitialSolutionFactory{

	private VRP vrp;
	
	private VRPTransformation vrpTransformation;
	
	private CarrierPlan carrierPlan;
	
	public PlanInitialSolutionFactory(VRP vrp, VRPTransformation vrpTransformation, CarrierPlan carrierPlan) {
		super();
		this.vrp = vrp;
		this.vrpTransformation = vrpTransformation;
		this.carrierPlan = carrierPlan;
	}

	@Override
	public Collection<Tour> createInitialSolution(VRP vrp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tour createRoundTour(VRP vrp, Customer from, Customer to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vehicle createVehicle(VRP vrp, Tour tour) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
