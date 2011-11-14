package org.matsim.contrib.freight.replanning;

import java.util.Collection;

import org.matsim.contrib.freight.carrier.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
}