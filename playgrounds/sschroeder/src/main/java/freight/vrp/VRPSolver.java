package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.Tour;

public interface VRPSolver {

	public abstract void solve(Collection<Contract> contracts,
			CarrierVehicle carrierVehicle);

	public abstract Collection<Tour> solve();
}