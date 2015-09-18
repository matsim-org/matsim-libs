/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.networks.shortestpaths;

import java.util.Collection;

import floetteroed.utilities.networks.basic.BasicLink;
import floetteroed.utilities.networks.basic.BasicNode;



/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class LinkCostExcludingNodes implements LinkCost {

	// -------------------- CONSTANTS --------------------

	private final LinkCost linkCost;

	private final Collection<BasicNode> excludedNodes;

	// -------------------- CONSTRUCTION --------------------

	public LinkCostExcludingNodes(final LinkCost linkCost,
			final Collection<BasicNode> excludedNodes) {
		if (linkCost == null) {
			throw new IllegalArgumentException("linkCost is null");
		}
		if (excludedNodes == null) {
			throw new IllegalArgumentException("excludedNodes is null");
		}
		this.linkCost = linkCost;
		this.excludedNodes = excludedNodes;
	}

	// -------------------- IMPLEMENTATION OF LinkCost --------------------

	@Override
	public double getCost(final BasicLink link) {
		if (this.excludedNodes.contains(link.getFromNode())
				|| this.excludedNodes.contains(link.getToNode())) {
			return Double.POSITIVE_INFINITY;
		} else {
			return this.linkCost.getCost(link);
		}
	}
}
