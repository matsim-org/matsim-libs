package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.AfterMobsim;

public interface Wait2Link extends AfterMobsim {

	boolean accept(DistributedMobsimVehicle vehicle, SimLink link, double now);

	void moveWaiting(double now);
}
