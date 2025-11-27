package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

public interface Wait2Link {

	boolean accept(DistributedMobsimVehicle vehicle, SimLink link, double now);

	void moveWaiting(double now);
}
