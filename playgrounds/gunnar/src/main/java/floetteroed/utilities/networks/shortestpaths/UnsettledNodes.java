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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import floetteroed.utilities.networks.basic.BasicNode;



/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class UnsettledNodes implements Comparator<BasicNode> {

	// -------------------- MEMBERS --------------------

	private final Map<BasicNode, Double> treeCost;

	private final SortedSet<BasicNode> nodes;

	// -------------------- CONSTRUCTIONS --------------------

	UnsettledNodes() {
		this.treeCost = new HashMap<BasicNode, Double>();
		this.nodes = new TreeSet<BasicNode>(this);
	}

	// -------------------- IMPLEMENTATION --------------------

	boolean isEmpty() {
		return this.nodes.isEmpty();
	}

	int size() {
		return this.nodes.size();
	}

	BasicNode first() {
		return this.nodes.first();
	}

	void remove(final BasicNode node) {
		this.nodes.remove(node);
	}

	void update(final BasicNode node, final double cost) {
		this.nodes.remove(node);
		if (!Double.isInfinite(cost)) {
			this.treeCost.put(node, cost);
		}
		this.nodes.add(node);
	}

	Double cost(final BasicNode node) {
		return Router.treeCost(node, this.treeCost);
	}

	Map<BasicNode, Double> cost() {
		return this.treeCost;
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compare(final BasicNode n1, final BasicNode n2) {
		final int costResult = Double.compare(this.cost(n1), this.cost(n2));
		if (costResult != 0) {
			return costResult;
		} else {
			// TODO this requires unique string representations of the ids
			return n1.getId().toString().compareTo(n2.getId().toString());
		}
	}
}
