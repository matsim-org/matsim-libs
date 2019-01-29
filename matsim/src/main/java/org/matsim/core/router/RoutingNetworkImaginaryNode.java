/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import java.util.Collection;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.PreProcessDijkstra.DeadEndData;

/**
 * A subclass of ImaginaryNode for fast routers (FastMultiNodeDijkstra and BackwardFastMultiNodeDijkstra).
 * 
 * @author michalm
 */
public class RoutingNetworkImaginaryNode extends ImaginaryNode implements RoutingNetworkNode {

	public RoutingNetworkImaginaryNode(Collection<? extends InitialNode> initialNodes) {
		super(initialNodes);
	}

	@Override
	public Node getNode() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setOutLinksArray(RoutingNetworkLink[] outLinks) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public RoutingNetworkLink[] getOutLinksArray() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setDeadEndData(DeadEndData deadEndData) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public DeadEndData getDeadEndData() {
		return null;
	}
}
