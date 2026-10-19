package org.matsim.contrib.common.zones.systems.grid.h3;

import org.matsim.api.core.v01.network.Node;

import java.util.List;

/**
 * Determines whether a given H3 cell should be subdivided into finer-resolution child cells
 * in a {@link HierarchicalH3ZoneSystem}.
 *
 * @author nkuehnel / MOIA
 */
@FunctionalInterface
public interface SubdivisionCriterion {

	/**
	 * @param h3Cell            the H3 cell index at the current resolution
	 * @param nodesInCell       the network nodes that fall within this cell
	 * @param currentResolution the H3 resolution level of this cell
	 * @param maxResolution     the maximum resolution allowed
	 * @return true if this cell should be subdivided into children at currentResolution + 1
	 */
	boolean shouldSubdivide(long h3Cell, List<Node> nodesInCell, int currentResolution, int maxResolution);
}