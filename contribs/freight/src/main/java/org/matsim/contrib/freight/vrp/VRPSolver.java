package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.contrib.freight.carrier.Tour;

public interface VRPSolver {

	public abstract Collection<Tour> solve();
	
}