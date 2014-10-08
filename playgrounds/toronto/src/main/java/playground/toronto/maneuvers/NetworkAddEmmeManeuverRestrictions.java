/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTeleatlasAddManeuverRestrictions.java
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

package playground.toronto.maneuvers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkExpandNode;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.utils.collections.Tuple;

/**
 * Keyword(s): emme/2
 * 
 * @author balmer
 */
public class NetworkAddEmmeManeuverRestrictions {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(NetworkAddEmmeManeuverRestrictions.class);

	private final String maneuversTextFileName;

	public boolean removeUTurns = false;
	public double expansionRadius = 0.000030; // WGS84
	public double linkSeparation = 0.000005; // WGS84

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkAddEmmeManeuverRestrictions(final String maneuversTextFileName) {
		log.info("init " + this.getClass().getName() + " module...");
		this.maneuversTextFileName = maneuversTextFileName;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final Map<Id,ArrayList<TurnInfo>> parseManeuvers(final Network network) {
		log.info("  parsing maneuvers...");
		Map<Id,ArrayList<TurnInfo>> illegalManeuvers = new HashMap<Id, ArrayList<TurnInfo>>();
		try {
			FileReader fr = new FileReader(maneuversTextFileName);
			BufferedReader br = new BufferedReader(fr);
			int lineCnt = 0;
			int mnCnt = 0;
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				lineCnt++;
				String[] entries = curr_line.split("\t", -1);
				// "a"  nodeid  fromnodeid  tonodeid  "0"
				// 0    1       2           3         4
				Id<Node> nid = Id.create(entries[1], Node.class);
				Id<Node> fnid = Id.create(entries[2], Node.class);
				Id<Node> tnid = Id.create(entries[3], Node.class);
				Node n = network.getNodes().get(nid);
				Node fn = network.getNodes().get(fnid);
				Node tn = network.getNodes().get(tnid);
				if ((n != null) && (fn != null) && (tn != null)) {
					if (((NodeImpl) n).getInNodes().containsKey(fn.getId()) && (((NodeImpl) n).getOutNodes().containsKey(tn.getId()))) {
						ArrayList<TurnInfo> mns = illegalManeuvers.get(n.getId());
						if (mns == null) {
							mns = new ArrayList<TurnInfo>();
							illegalManeuvers.put(n.getId(),mns);
						}
						Link inlink = null;
						for (Link l : n.getInLinks().values()) {
							if (l.getFromNode().getId().equals(fn.getId())) {
								inlink = l;
							}
						}
						Link outlink = null;
						for (Link l : n.getOutLinks().values()) {
							if (l.getToNode().getId().equals(tn.getId())) {
								outlink = l;
							}
						}
						mns.add(new TurnInfo(inlink.getId(),outlink.getId()));
						mnCnt++;
					}
					else { log.warn("line="+lineCnt+", nid="+nid+", fnid="+fnid+", tnid="+tnid+": either from_node or to_node are not neighbours of node."); }
				}
				else { log.warn("line="+lineCnt+", nid="+nid+", fnid="+fnid+", tnid="+tnid+": at least one node not found."); }
			}
			log.info("    "+illegalManeuvers.size()+" nodes contains maneuver restrictions.");
			log.info("    "+mnCnt+" maneuver restrictions in total.");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("  done.");
		return illegalManeuvers;
	}

	private final boolean contains(TurnInfo tuple, ArrayList<TurnInfo> tuples) {
		for (TurnInfo t : tuples) {
			if (t.equals(tuple)) { return true; }
		}
		return false;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Network network) throws Exception {
		log.info("running " + this.getClass().getName() + " module...");
		
		NetworkExpandNode neModule = new NetworkExpandNode(network, this.expansionRadius, this.linkSeparation);

		int nodesAssignedCnt = 0;
		int virtualNodesCnt = 0;
		int virtualLinksCnt = 0;

		Map<Id,ArrayList<TurnInfo>> illegalManeuvers = this.parseManeuvers(network);
		Map<Id,ArrayList<TurnInfo>> maneuvers = new HashMap<Id, ArrayList<TurnInfo>>();

		for (Id nodeId : illegalManeuvers.keySet()) {
			ArrayList<TurnInfo> mns = illegalManeuvers.get(nodeId);
			ArrayList<TurnInfo> turns = new ArrayList<TurnInfo>();

			Node n = network.getNodes().get(nodeId);
			for (Link inLink : n.getInLinks().values()) {
				for (Link outLink : n.getOutLinks().values()) {
					TurnInfo tuple = new TurnInfo(inLink.getId(), outLink.getId());
					if (!this.contains(tuple,mns)) {
						if (removeUTurns) {
							if (!inLink.getFromNode().getId().equals(outLink.getToNode().getId())) {
								turns.add(tuple);
							}
						}
						else {
							turns.add(tuple);
						}
					}
				}
			}
			maneuvers.put(n.getId(),turns);
		}
		for (Id nodeId : maneuvers.keySet()) {
			ArrayList<TurnInfo> turns = maneuvers.get(nodeId);
			Tuple<List<Node>, List<Link>> t = neModule.expandNode(nodeId, turns);
			virtualNodesCnt += t.getFirst().size();
			virtualLinksCnt += t.getSecond().size();
			nodesAssignedCnt++;
		}
		log.info("  "+nodesAssignedCnt+" nodes expanded.");
		log.info("  "+virtualNodesCnt+" new nodes created.");
		log.info("  "+virtualLinksCnt+" new links created.");
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void printInfo(final String prefix) {
		System.out.println(prefix+"configuration of "+this.getClass().getName()+":");
		System.out.println(prefix+"  options:");
		System.out.println(prefix+"    removeUTurns:          "+removeUTurns);
		System.out.println(prefix+"    expansionRadius:       "+expansionRadius);
		System.out.println(prefix+"    linkSeparation:        "+linkSeparation);
		System.out.println(prefix+"    maneuversTextFileName: "+maneuversTextFileName);
		System.out.println(prefix+"done.");
	}
}
