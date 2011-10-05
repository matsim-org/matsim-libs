package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierVehicle;
import playground.mzilske.freight.carrier.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
}