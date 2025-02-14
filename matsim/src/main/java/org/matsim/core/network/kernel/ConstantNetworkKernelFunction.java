package org.matsim.core.network.kernel;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.SearchableNetwork;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A kernel function that assigns a constant weight to all links within a certain distance. Uses a LinkQuadTree to determine the links within the distance.
 */
public class ConstantNetworkKernelFunction implements NetworkKernelFunction {
    LinkQuadTree quadTree;

    @Inject
    public ConstantNetworkKernelFunction(Network network) {
        if (!(network instanceof SearchableNetwork)) {
            throw new IllegalArgumentException("The network must be a SearchableNetwork if you want to calculate parking search times based on occupancy.");
        }

        this.quadTree = ((SearchableNetwork) network).getLinkQuadTree();
    }

    @Override
    public Map<Id<Link>, Double> calculateWeightedKernel(Link link, double distance) {
        return quadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), distance).stream()
                .collect(Collectors.toMap(Identifiable::getId, l -> 1.0));
    }
}
