package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.CarrierPlan;
import vrp.api.VRP;
import vrp.basics.Tour;

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
	public Collection<Tour> createInitialSolution() {
		
		return null;
	}

}
