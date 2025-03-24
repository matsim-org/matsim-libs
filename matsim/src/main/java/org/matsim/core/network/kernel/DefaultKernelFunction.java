package org.matsim.core.network.kernel;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A kernel function that assigns a constant weight to all links within a certain distance. Uses a LinkQuadTree to determine the links within the distance.
 */
public class DefaultKernelFunction implements NetworkKernelFunction {
	LinkQuadTree quadTree;
	KernelDistance kernelDistance;

	private final Map<Tuple<Id<Vehicle>, Id<Link>>, Map<Id<Link>, Double>> kernelCache = new HashMap<>();

	@Inject
	public DefaultKernelFunction(Network network, KernelDistance kernelDistance) {
		if (!(network instanceof SearchableNetwork)) {
			throw new IllegalArgumentException("The network must be a SearchableNetwork if you want to calculate parking search times based on occupancy.");
		}

		this.quadTree = ((SearchableNetwork) network).getLinkQuadTree();
		this.kernelDistance = kernelDistance;
	}

	@Override
	synchronized public Map<Id<Link>, Double> calculateWeightedKernel(QVehicle vehicle, Link link) {
		kernelCache.computeIfAbsent(new Tuple<>(vehicle.getVehicle().getId(), link.getId()),
			k -> {
				double distance = this.kernelDistance.calculateDistance(vehicle, link);
				return quadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), distance).stream()
					.collect(Collectors.toMap(Identifiable::getId, l -> 1.0));
			});
		return kernelCache.get(new Tuple<>(vehicle.getVehicle().getId(), link.getId()));
	}
}
