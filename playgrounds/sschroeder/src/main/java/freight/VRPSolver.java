package freight;

import java.util.Collection;

import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;

public interface VRPSolver {

	public abstract void solve(Collection<Contract> contracts,
			CarrierVehicle carrierVehicle);

}