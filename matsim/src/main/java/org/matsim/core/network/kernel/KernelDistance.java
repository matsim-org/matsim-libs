package org.matsim.core.network.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public interface KernelDistance {
	double calculateDistance(QVehicle vehicle, Link link);
}
