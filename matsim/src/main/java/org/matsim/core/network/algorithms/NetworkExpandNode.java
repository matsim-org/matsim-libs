/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkExpandNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;

/**
 * A Procedure to expand a node of the {@link Network network}.
 *
 * @author balmermi
 * @author mrieser / senozon
 */
public class NetworkExpandNode {

	private final Network network;
	private double expRadius = 1.0;
	private double offset = 0.0;
	private double dist = 1.0; // cache a value that is often used in the method expandNode
	
	public NetworkExpandNode(final Network network, final double expRadius, final double offset) {
		if (network == null) {
			throw new IllegalArgumentException("network must not be null.");
		}
		this.network = network;
		this.setExpRadius(expRadius);
		this.setOffset(offset);
	}
	
	/**
	 * @param expRadius the expansion radius. If zero, all new nodes have the same coordinate
	 * and the new links with have length equals zero
	 */
    void setExpRadius(final double expRadius) {
		if (Double.isNaN(expRadius)) {
			throw new IllegalArgumentException("expansion radius must not be NaN.");
		}
		this.expRadius = expRadius;
		this.dist = Math.sqrt(this.expRadius * this.expRadius - this.offset * this.offset);
	}

	/**
	 * @param offset the offset between a link pair with the same incident nodes. If zero, the two new
	 * nodes created for that link pair will have the same coordinates
	 */
    void setOffset(final double offset) {
		if (Double.isNaN(offset)) {
			throw new IllegalArgumentException("expansion offset must not be NaN.");
		}
		this.offset = offset;
		this.dist = Math.sqrt(this.expRadius * this.expRadius - this.offset * this.offset);
	}

	/**
	 * Expands the specified {@link Node node} that is part of the {@link Network network} and
	 * adds turning restrictions/maneuvers to it. 
	 *
	 * <p>It is done in the following way:
	 * <ol>
	 * <li>creates for each in- and out-link a new node with
	 * <ul>
	 * <li><code>new_nodeId = nodeId+"-"+index; index=[0..#incidentLinks]</code></li>
	 * <li><code>new_coord</code> with distance <code>r</code> to the given node
	 * in direction of the corresponding incident Link of the given node with
	 * offset <code>e</code>.</li>
	 * </ul>
	 * <pre>
	 * <-----12------         <----21-------
	 *
	 *            x-0 o     o 1-5
	 *                   O nodeId = x
	 *            x-1 o     o x-4
	 *             x-2 o   o x-3
	 * ------11----->         -----22------>
	 *               |       ^
	 *               |       |
	 *              32       31
	 *               |       |
	 *               |       |
	 *               v       |
	 * </pre>
	 * </li>
	 * <li>connects each incident link of the given node with the corresponding <code>new_node</code>
	 * <pre>
	 * <-----12------ o     o <----21-------
	 *                   O
	 * ------11-----> o     o -----22------>
	 *                 o   o
	 *                 |   ^
	 *                 |   |
	 *                32   31
	 *                 |   |
	 *                 |   |
	 *                 v   |
	 * </pre>
	 * </li>
	 * <li>removes the given node from the network
	 * <pre>
	 * <-----12------ o     o <----21-------
	 *
	 * ------11-----> o     o -----22------>
	 *                 o   o
	 *                 |   ^
	 *                 |   |
	 *                32   31
	 *                 |   |
	 *                 |   |
	 *                 v   |
	 * </pre>
	 * </li>
	 * <li>inter-connects the <code>new_node</code>s with new links as defined in the
	 * <code>turns</code> list, with:<br>
	 * <ul>
	 * <li><code>new_linkId = fromLinkId+"-"+index; index=[0..#turn-tuples]</code></li>
	 * <li>length equals the Euclidean distance</li>
	 * <li>freespeed, capacity, permlanes, origId and type are equals to the attributes of the fromLink.</li>
	 * </ul>
	 * <pre>
	 * <-----12------ o <--21-0-- o <----21-------
	 *
	 * ------11-----> o --11-1--> o -----22------>
	 *                 \         ^
	 *              11-2\       /31-3
	 *                   \     /
	 *                    v   /
	 *                    o   o
	 *                    |   ^
	 *                    |   |
	 *                   32   31
	 *                    |   |
	 *                    |   |
	 *                    v   |
	 * </pre>
	 * </li>
	 * </ol>
	 * </p>
	 *
	 * @param nodeId the {@link Id} of the {@link Node} to expand
	 * @param turns The {@link List} of allowed turns at the given {@link Node node}.
	 * @return The {@link Tuple} of {@link List lists} containing the newly created
	 * {@link Node nodes} and {@link Link links}.
	 */
	public final Tuple<List<Node>,List<Link>> expandNode(final Id<Node> nodeId, final List<TurnInfo> turns) {
		double e = this.offset;
		Node node = network.getNodes().get(nodeId);
		if (node == null) {
			throw new IllegalArgumentException("nodeid="+nodeId+": not found in the network.");
		}
		if (turns == null) {
			throw new IllegalArgumentException("nodeid="+nodeId+": turn list not defined!");
		}

        for (TurnInfo turn1 : turns) {
            Id<Link> first = turn1.getFromLinkId();
            if (first == null) {
                throw new IllegalArgumentException("given list contains 'null' values.");
            }
            if (!node.getInLinks().containsKey(first)) {
                throw new IllegalArgumentException("nodeid=" + nodeId + ", linkid=" + first + ": link not an inlink of given node.");
            }
            Id<Link> second = turn1.getToLinkId();
            if (second == null) {
                throw new IllegalArgumentException("given list contains 'null' values.");
            }
            if (!node.getOutLinks().containsKey(second)) {
                throw new IllegalArgumentException("nodeid=" + nodeId + ", linkid=" + second + ": link not an outlink of given node.");
            }
        }

		// remove the node
		Map<Id<Link>,Link> inlinks = new TreeMap<>(node.getInLinks());
		Map<Id<Link>,Link> outlinks = new TreeMap<>(node.getOutLinks());
		if (network.removeNode(node.getId()) == null) { throw new RuntimeException("nodeid="+nodeId+": Failed to remove node from the network."); }

		ArrayList<Node> newNodes = new ArrayList<>(inlinks.size()+outlinks.size());
		ArrayList<Link> newLinks = new ArrayList<>(turns.size());
		// add new nodes and connect them with the in and out links
		int nodeIdCnt = 0;
		double d = this.dist;
		for (Link inlink : inlinks.values()) {
			Coord c = node.getCoord();
			Coord p = inlink.getFromNode().getCoord();
			Coord cp = new Coord(p.getX() - c.getX(), p.getY() - c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			if (Math.abs(lcp) < 1e-8) {
				// c and p seem to lay on top of each other, leading to Double.NaN in some calculations
				lcp = d;
			}
			double dx = cp.getX() / lcp;
			double dy = cp.getY() / lcp;
			double x = c.getX() + d * dx - e * dy;
			double y = c.getY() + d * dy + e * dx;

			Node n = network.getFactory().createNode(Id.create(node.getId()+"-"+nodeIdCnt, Node.class), new Coord(x, y));
			network.addNode(n);
			newNodes.add(n);
			nodeIdCnt++;
			Link l = network.getFactory().createLink(inlink.getId(), inlink.getFromNode(), n);
			l.setLength(inlink.getLength());
			l.setFreespeed(inlink.getFreespeed());
			l.setCapacity(inlink.getCapacity());
			l.setNumberOfLanes(inlink.getNumberOfLanes());
			l.setAllowedModes(inlink.getAllowedModes());
			if (inlink instanceof LinkImpl) {
				((LinkImpl) l).setOrigId(((LinkImpl) inlink).getOrigId());
				((LinkImpl) l).setType(((LinkImpl) inlink).getType());
			}
			network.addLink(l);
		}
		for (Link outlink : outlinks.values()) {
			Coord c = node.getCoord();
			Coord p = outlink.getToNode().getCoord();
			Coord cp = new Coord(p.getX() - c.getX(), p.getY() - c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			if (Math.abs(lcp) < 1e-8) {
				// c and p seem to lay on top of each other, leading to Double.NaN in some calculations
				lcp = d;
			}
			double dx = cp.getX() / lcp;
			double dy = cp.getY() / lcp;
			double x = c.getX() + d * dx + e * dy;
			double y = c.getY() + d * dy - e * dx;
			Node n = network.getFactory().createNode(Id.create(node.getId()+"-"+nodeIdCnt, Node.class), new Coord(x, y));
			network.addNode(n);
			newNodes.add(n);
			nodeIdCnt++;
			Link l = network.getFactory().createLink(outlink.getId(), n, outlink.getToNode());
			l.setLength(outlink.getLength());
			l.setFreespeed(outlink.getFreespeed());
			l.setCapacity(outlink.getCapacity());
			l.setNumberOfLanes(outlink.getNumberOfLanes());
			l.setAllowedModes(outlink.getAllowedModes());
			if (outlink instanceof LinkImpl) {
				((LinkImpl) l).setOrigId(((LinkImpl) outlink).getOrigId());
				((LinkImpl) l).setType(((LinkImpl) outlink).getType());
			}
			network.addLink(l);
		}

		// add virtual links for the turn restrictions
		for (int i=0; i<turns.size(); i++) {
			TurnInfo turn = turns.get(i);
			Link fromLink = network.getLinks().get(turn.getFromLinkId());
			Link toLink = network.getLinks().get(turn.getToLinkId());
			Link l = network.getFactory().createLink(Id.create(fromLink.getId()+"-"+i, Link.class), fromLink.getToNode(), toLink.getFromNode());
			double dist = CoordUtils.calcEuclideanDistance(toLink.getFromNode().getCoord(), fromLink.getToNode().getCoord());
			if (dist < 0.1 * this.expRadius) {
				// mostly the case when nodes are on top of each other
				// use a small length, but not really hard-coded because of different coordinate systems ("1" is not always 1 meter)
				dist = 0.1 * this.expRadius;
			}
			l.setLength(dist);
			l.setFreespeed(fromLink.getFreespeed());
			l.setCapacity(fromLink.getCapacity());
			l.setNumberOfLanes(fromLink.getNumberOfLanes());
			if (turn.getModes() == null) {
				l.setAllowedModes(fromLink.getAllowedModes());
			} else {
				l.setAllowedModes(turn.getModes());
			}
			if (fromLink instanceof LinkImpl) {
				((LinkImpl) l).setOrigId(((LinkImpl) fromLink).getOrigId());
				((LinkImpl) l).setType(((LinkImpl) fromLink).getType());
			}
			network.addLink(l);
			newLinks.add(l);
		}
		return new Tuple<List<Node>, List<Link>>(newNodes,newLinks);
	}
	
	/**
	 * Checks whether it makes sense to expand the node with turning links or not. For example, it might 
	 * not make sense to expand the node if no real restrictions are given, but just all possible options
	 * are listed -- the exact same turning options could be available without expanding the node.
	 * Thus, it usually only makes sense to expand a node if {@link #turnsAreSameAsSingleNode(Id, List)}
	 * returns <code>false</code>.
	 * The algorithm can optionally ignore u-turns, that means that it is not checked if all u-turns are
	 * allowed according to the given turning options. 
	 * 
	 * 
	 * @param nodeId
	 * @param turns the allowed turning options
	 * @param ignoreUTurns set this to <code>true</code> if u-turns are excluded from the check
	 * @return <code>true</code> if the list of explicit turns given results in the same turning options available as when the node is not expanded, <code>false</code> otherwise. 
	 */
	public boolean turnsAreSameAsSingleNode(final Id<Node> nodeId, final List<TurnInfo> turns, final boolean ignoreUTurns) {
		Node node = this.network.getNodes().get(nodeId);

		// first create list of all possible turning options
		Map<Id<Link>, Map<Id<Link>, Set<String>>> allTurns = new HashMap<>();
		
		for (Link inLink : node.getInLinks().values()) {
			Map<Id<Link>, Set<String>> t2 = new HashMap<>();
			for (Link outLink : node.getOutLinks().values()) {
				if (inLink.getFromNode() != outLink.getToNode() || !ignoreUTurns) {
					HashSet<String> modes = new HashSet<>();
					modes.addAll(inLink.getAllowedModes());
					modes.retainAll(outLink.getAllowedModes()); // modes contains now all useful modes
					t2.put(outLink.getId(), modes);
				}
			}
			allTurns.put(inLink.getId(), t2);
		}
		
		/* now compare the given turning options and remove them from all possible options.
		 * if there remain some options in the "all possibles", we know that the given turns
		 * act indeed as restrictions. */
		for (TurnInfo ti : turns) {
			Id<Link> from = ti.fromLinkId;
			Id<Link> to = ti.toLinkId;
			Map<Id<Link>, Set<String>> t2 = allTurns.get(from);
			if (t2 != null) {
				Set<String> modes = t2.get(to);
				if (modes != null) {
					if (ti.getModes() == null) {
						// no specific modes are set, so remove all modes from the inLink
						// because the modes are the intersection of modes from inLink and outLink, we can just clear it
						modes.clear();
					} else {
						modes.removeAll(ti.getModes());
					}
				}
			}
		}
		
		// now do the check
		for (Map<Id<Link>, Set<String>> m : allTurns.values()) {
			for (Set<String> modes : m.values()) {
				if (modes.size() > 0) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static class TurnInfo {
		
		private final Id<Link> fromLinkId;
		private final Id<Link> toLinkId;
		private final Set<String> modes;
		
		public TurnInfo(final Id<Link> fromLinkId, final Id<Link> toLinkId) {
			this.fromLinkId = fromLinkId;
			this.toLinkId = toLinkId;
			this.modes = null;
		}
		
		public TurnInfo(final Id<Link> fromLinkId, final Id<Link> toLinkId, final Set<String> modes) {
			this.fromLinkId = fromLinkId;
			this.toLinkId = toLinkId;
			this.modes = modes;
		}
		
		public Id<Link> getFromLinkId() {
			return this.fromLinkId;
		}
		
		public Id<Link> getToLinkId() {
			return this.toLinkId;
		}
		
		public Set<String> getModes() {
			return this.modes;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof TurnInfo)) {
				return false;
			}
			TurnInfo ti = (TurnInfo) obj;
			return (ti.fromLinkId.equals(this.fromLinkId))
					&& (ti.toLinkId.equals(this.toLinkId))
					&& ((ti.modes == null && this.modes == null)
							|| (ti.modes != null && ti.modes.equals(this.modes))
							);
		}
		
		@Override
		public int hashCode() {
			return this.fromLinkId.hashCode() & this.toLinkId.hashCode() & this.modes.hashCode();
		}
		
	}
	
}
