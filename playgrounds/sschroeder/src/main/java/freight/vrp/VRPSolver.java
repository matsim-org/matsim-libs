package freight.vrp;

import java.util.Collection;

import playground.mzilske.freight.carrier.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
}