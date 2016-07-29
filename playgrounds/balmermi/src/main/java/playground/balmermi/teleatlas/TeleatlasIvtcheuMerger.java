/* *********************************************************************** *
 * project: org.matsim.*
 * TeleatlasIvtcheuMerger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.teleatlas;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class TeleatlasIvtcheuMerger {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(TeleatlasIvtcheuMerger.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	private static final void deleteLinks(Network networkIvtcheu, String il2deletefile) {
		log.info("deleteLinks...");
		log.info("  init number of links: "+networkIvtcheu.getLinks().size());
		log.info("  init number of nodes: "+networkIvtcheu.getNodes().size());
		int lineCnt = 0;
		try {
			FileReader fr = new FileReader(il2deletefile);
			BufferedReader br = new BufferedReader(fr);
			// Skip header
			String curr_line = br.readLine(); lineCnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// ivtchLID
				// 0
				Id<Link> lid = Id.create(entries[0].trim(), Link.class);
				Link l = networkIvtcheu.getLinks().get(lid);
				if (l == null) { throw new RuntimeException(lineCnt+": link with id="+lid+" not found."); }
				if (networkIvtcheu.removeLink(l.getId()) != null) { throw new RuntimeException(lineCnt+": could not remove link with id="+lid+"."); }
				lineCnt++;
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			throw new RuntimeException(lineCnt+": read error.");
		}
		log.info("  number of lines processed: "+lineCnt);
		log.info("  final number of links: "+networkIvtcheu.getLinks().size());
		log.info("  final number of nodes: "+networkIvtcheu.getNodes().size());
		log.info("done.");
	}

	private static final void mergeNetworks(Network networkTeleatlas, Network networkIvtcheu, String i2tmappingfile) {
		log.info("adapt mergeNetworks...");
		log.info("  init number of links (networkTeleatlas): "+networkTeleatlas.getLinks().size());
		log.info("  init number of nodes (networkTeleatlas): "+networkTeleatlas.getNodes().size());
		log.info("  init number of links (networkIvtcheu): "+networkIvtcheu.getLinks().size());
		log.info("  init number of nodes (networkIvtcheu): "+networkIvtcheu.getNodes().size());
		int lineCnt = 0;
		Map<Id<Node>, Id<Node>> nodeMapping = new TreeMap<>();
		try {
			FileReader fr = new FileReader(i2tmappingfile);
			BufferedReader br = new BufferedReader(fr);
			// Skip header
			String curr_line = br.readLine(); lineCnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// ivtchNID  teleatlasNID
				// 0         1
				Id<Node> iNid = Id.create(entries[0].trim(), Node.class);
				Id<Node> tNid = Id.create(entries[1].trim(), Node.class);
				if (nodeMapping.put(iNid,tNid) != null) { throw new RuntimeException(lineCnt+": ivtch node id="+iNid+" already exists."); }
				lineCnt++;
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			throw new RuntimeException(lineCnt+": read error.");
		}

		int nodeMapCnt = 0;
		for (Node n : networkIvtcheu.getNodes().values()) {
			if (!nodeMapping.containsKey(n.getId())) {
				Node r = ((Node) n);
				Node n1 = NetworkUtils.createAndAddNode(networkTeleatlas, n.getId(), n.getCoord());
				NetworkUtils.setType(n1,(String) NetworkUtils.getType( r ));
			}
			else { nodeMapCnt++; }
		}
		if (nodeMapCnt != nodeMapping.size()) { throw new RuntimeException("Something is wrong!"); }

		for (Link l : networkIvtcheu.getLinks().values()) {
			Id<Node> fromNodeId = l.getFromNode().getId();
			Id<Node> toNodeId = l.getToNode().getId();
			if (nodeMapping.keySet().contains(fromNodeId) && nodeMapping.keySet().contains(toNodeId)) {
				fromNodeId = nodeMapping.get(fromNodeId);
				toNodeId = nodeMapping.get(toNodeId);
			}
			else if (nodeMapping.keySet().contains(fromNodeId) && !nodeMapping.keySet().contains(toNodeId)) {
				fromNodeId = nodeMapping.get(fromNodeId);
			}
			else if (!nodeMapping.keySet().contains(fromNodeId) && nodeMapping.keySet().contains(toNodeId)) {
				toNodeId = nodeMapping.get(toNodeId);
			}
			else if (!nodeMapping.keySet().contains(fromNodeId) && !nodeMapping.keySet().contains(toNodeId)) {
			}
			else { throw new RuntimeException("HAEH?"); }
			NetworkUtils.createAndAddLink(networkTeleatlas,l.getId(), networkTeleatlas.getNodes().get(fromNodeId), networkTeleatlas.getNodes().get(toNodeId), l.getLength(), l.getFreespeed(), l.getCapacity()/10.0, l.getNumberOfLanes(), (String) NetworkUtils.getOrigId( ((Link) l) ), (String) NetworkUtils.getType(((Link) l)));
		}

		log.info("  number of lines processed: "+lineCnt);
		log.info("  final number of links (networkTeleatlas): "+networkTeleatlas.getLinks().size());
		log.info("  final number of nodes (networkTeleatlas): "+networkTeleatlas.getNodes().size());
		log.info("  final number of links (networkIvtcheu): "+networkIvtcheu.getLinks().size());
		log.info("  final number of nodes (networkIvtcheu): "+networkIvtcheu.getNodes().size());
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// main method
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
		if (args.length != 5) { log.info("wrong number of params"); return; }
		String teleatlasfile = args[0].trim();
		String ivtcheufile = args[1].trim();
		String i2tmappingfile = args[2].trim();
		String il2deletefile = args[3].trim();
		String outNetFile = args[4].trim();

		MutableScenario taScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network networkTeleatlas = (Network) taScenario.getNetwork();
		new MatsimNetworkReader(taScenario.getNetwork()).readFile(teleatlasfile);

		MutableScenario ivtcheuScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network networkIvtcheu = (Network) ivtcheuScenario.getNetwork();
		new MatsimNetworkReader(ivtcheuScenario.getNetwork()).readFile(ivtcheufile);

		deleteLinks(networkIvtcheu,il2deletefile);
		mergeNetworks(networkTeleatlas,networkIvtcheu,i2tmappingfile);

		NetworkWriteAsTable nwat = new NetworkWriteAsTable("../../output/");
		nwat.run(networkTeleatlas);

		new NetworkWriter(networkTeleatlas).write(outNetFile);
	}
}
