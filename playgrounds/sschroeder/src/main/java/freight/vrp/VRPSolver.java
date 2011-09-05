package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
	public void solve(Collection<Contract> contracts, CarrierVehicle carrierVehicle);
}