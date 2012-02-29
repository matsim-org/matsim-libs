package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.contrib.freight.carrier.ScheduledTour;

public interface VRPSolver {

	public abstract Collection<ScheduledTour> solve();
	
}