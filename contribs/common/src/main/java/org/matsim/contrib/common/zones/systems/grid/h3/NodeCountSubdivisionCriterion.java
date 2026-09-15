package org.matsim.contrib.common.zones.systems.grid.h3;

import org.matsim.api.core.v01.network.Node;

import java.util.List;

/**
 * Subdivides an H3 cell if it contains more network nodes than a given threshold.
 *
 * @author nkuehnel / MOIA
 */
public class NodeCountSubdivisionCriterion implements SubdivisionCriterion {

	private final int maxNodesPerZone;

	public NodeCountSubdivisionCriterion(int maxNodesPerZone) {
		this.maxNodesPerZone = maxNodesPerZone;
	}

	@Override
	public boolean shouldSubdivide(long h3Cell, List<Node> nodesInCell, int currentResolution, int maxResolution) {
		return currentResolution < maxResolution && nodesInCell.size() > maxNodesPerZone;
	}
}