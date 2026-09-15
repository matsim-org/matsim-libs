package org.matsim.core.network.kernel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class ConstantKernelDistance implements KernelDistance {
	private final double distance;

	public ConstantKernelDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public double calculateDistance(QVehicle vehicle, Link link) {
		return distance;
	}
}
