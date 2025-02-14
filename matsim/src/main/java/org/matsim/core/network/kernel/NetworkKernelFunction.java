package org.matsim.core.network.kernel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface NetworkKernelFunction {
	Map<Id<Link>, Double> calculateWeightedKernel(Link link, double distance);
}
