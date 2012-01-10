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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

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
	public void setExpRadius(final double expRadius) {
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
	public void setOffset(final double offset) {
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
	public final Tuple<List<Node>,List<Link>> expandNode(final Id nodeId, final List<TurnInfo> turns) {
		double e = this.offset;
		Node node = network.getNodes().get(nodeId);
		if (node == null) {
			throw new IllegalArgumentException("nodeid="+nodeId+": not found in the network.");
		}
		if (turns == null) {
			throw new IllegalArgumentException("nodeid="+nodeId+": turn list not defined!");
		}
		
		for (int i=0; i<turns.size(); i++) {
			Id first = turns.get(i).getFromLinkId();
			if (first == null) {
				throw new IllegalArgumentException("given list contains 'null' values.");
			}
			if (!node.getInLinks().containsKey(first)) {
				throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+first+": link not an inlink of given node.");
			}
			Id second = turns.get(i).getToLinkId();
			if (second == null) {
				throw new IllegalArgumentException("given list contains 'null' values.");
			}
			if (!node.getOutLinks().containsKey(second)) {
				throw new IllegalArgumentException("nodeid="+nodeId+", linkid="+second+": link not an outlink of given node.");
			}
		}

		// remove the node
		Map<Id,Link> inlinks = new TreeMap<Id, Link>(node.getInLinks());
		Map<Id,Link> outlinks = new TreeMap<Id, Link>(node.getOutLinks());
		if (network.removeNode(node.getId()) == null) { throw new RuntimeException("nodeid="+nodeId+": Failed to remove node from the network."); }

		ArrayList<Node> newNodes = new ArrayList<Node>(inlinks.size()+outlinks.size());
		ArrayList<Link> newLinks = new ArrayList<Link>(turns.size());
		// add new nodes and connect them with the in and out links
		int nodeIdCnt = 0;
		double d = this.dist;
		for (Link inlink : inlinks.values()) {
			Coord c = node.getCoord();
			Coord p = inlink.getFromNode().getCoord();
			Coord cp = new CoordImpl(p.getX()-c.getX(),p.getY()-c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			if (Math.abs(lcp) < 1e-8) {
				// c and p seem to lay on top of each other, leading to Double.NaN in some calculations
				lcp = d;
			}
			double dx = cp.getX() / lcp;
			double dy = cp.getY() / lcp;
			double x = c.getX() + d * dx - e * dy;
			double y = c.getY() + d * dy + e * dx;
			
			
			
//			Coord c = node.getCoord();
//			Coord p = inlink.getFromNode().getCoord();
//			Coord pc = new CoordImpl(c.getX()-p.getX(),c.getY()-p.getY());
//			double lpc = Math.sqrt(pc.getX()*pc.getX()+pc.getY()*pc.getY());
//			if (Math.abs(lpc) < 1e-8) {
//				// c and p seem to lay on top of each other, leading to Double.NaN in some calculations
//				lpc = d;
//			}
//			double x = p.getX()+(1-d/lpc)*pc.getX()+e/lpc*pc.getY();
//			double y = p.getY()+(1-d/lpc)*pc.getY()-e/lpc*pc.getX();
			
			
			
			Node n = network.getFactory().createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y));
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
			Coord cp = new CoordImpl(p.getX()-c.getX(),p.getY()-c.getY());
			double lcp = Math.sqrt(cp.getX()*cp.getX()+cp.getY()*cp.getY());
			if (Math.abs(lcp) < 1e-8) {
				// c and p seem to lay on top of each other, leading to Double.NaN in some calculations
				lcp = d;
			}
			double dx = cp.getX() / lcp;
			double dy = cp.getY() / lcp;
			double x = c.getX() + d * dx + e * dy;
			double y = c.getY() + d * dy - e * dx;
			Node n = network.getFactory().createNode(new IdImpl(node.getId()+"-"+nodeIdCnt),new CoordImpl(x,y));
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
			Link l = network.getFactory().createLink(new IdImpl(fromLink.getId()+"-"+i), fromLink.getToNode(), toLink.getFromNode());
			double dist = CoordUtils.calcDistance(toLink.getFromNode().getCoord(), fromLink.getToNode().getCoord());
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
	
	public static class TurnInfo {
		
		private final Id fromLinkId;
		private final Id toLinkId;
		private final Set<String> modes;
		
		public TurnInfo(final Id fromLinkId, final Id toLinkId) {
			this.fromLinkId = fromLinkId;
			this.toLinkId = toLinkId;
			this.modes = null;
		}
		
		public TurnInfo(final Id fromLinkId, final Id toLinkId, final Set<String> modes) {
			this.fromLinkId = fromLinkId;
			this.toLinkId = toLinkId;
			this.modes = modes;
		}
		
		public Id getFromLinkId() {
			return this.fromLinkId;
		}
		
		public Id getToLinkId() {
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
							|| (ti.modes.equals(this.modes))
							);
		}
		
	}
	
}
