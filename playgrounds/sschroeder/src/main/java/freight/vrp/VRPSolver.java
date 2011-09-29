package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
	public void solve(Collection<CarrierContract> contracts, CarrierVehicle carrierVehicle);
}