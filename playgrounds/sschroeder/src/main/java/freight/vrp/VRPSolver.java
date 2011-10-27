package freight.vrp;

import org.matsim.contrib.freight.carrier.Tour;

import java.util.Collection;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
}