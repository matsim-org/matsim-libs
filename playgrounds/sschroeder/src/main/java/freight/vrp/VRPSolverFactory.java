package freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.Contract;

public interface VRPSolverFactory {
	
	public abstract VRPSolver createSolver(Collection<Contract> contracts, CarrierCapabilities capabilities, Network network);

}
