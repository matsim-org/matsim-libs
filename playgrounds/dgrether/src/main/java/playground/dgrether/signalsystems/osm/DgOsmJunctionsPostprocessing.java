/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmJunctionsPostprocessing
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.osm;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;

import playground.dgrether.DgPaths;
import playground.dgrether.osm.OSMEntityCollector;


/**
 * @author dgrether
 *
 */
public class DgOsmJunctionsPostprocessing {

	private static final Logger log = Logger.getLogger(DgOsmJunctionsPostprocessing.class);
	
	
	public void postprocessJunctions(String osmFile, String networkFile, String networkOutFile, String lanesFile, String lanesOutFile) {
		ScenarioImpl scenario = this.loadScenario(networkFile, lanesFile);
		Network network = scenario.getNetwork();
		String signalSystemKey = "btuc_signalsystem_id";

		OSMEntityCollector signalNodeCollector = this.detectAndWriteSignalizedOsmNodes(osmFile, signalSystemKey);
		Map<String, Set<Node>> signalSystemId2NodesMap = this.getSignalSystemId2NodeMap(signalNodeCollector, signalSystemKey, network);
		
		
		Map<Id, Set<Id>> removedLinkIdToLinkIdsMap = this.removeJunctions(network, signalSystemId2NodesMap);
		
		LaneDefinitions lanes = this.handleLanes(scenario.getLaneDefinitions(), removedLinkIdToLinkIdsMap);
		
		new NetworkWriter(network).write(networkOutFile);
		new MatsimLaneDefinitionsWriter(lanes).writeFile(lanesOutFile);
		log.info("done!");
	}
	
	private LaneDefinitions handleLanes(LaneDefinitions laneDefinitions, Map<Id, Set<Id>> removedLinkIdToLinkIdsMap) {
		for (LanesToLinkAssignment l2l : laneDefinitions.getLanesToLinkAssignments().values()){
			if (removedLinkIdToLinkIdsMap.containsKey(l2l.getLinkId())){
				throw new IllegalStateException("Link Id " + l2l.getLinkId() + " was removed but has lanes attached, can't handle this automatically!");
			}
			for (Lane lane : l2l.getLanes().values()){
				Set<Id> toLinkIds2Remove = new HashSet<Id>();
				if (lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty()){
					for (Id toLinkId : lane.getToLinkIds()){
						if (removedLinkIdToLinkIdsMap.containsKey(toLinkId)){
							toLinkIds2Remove.add(toLinkId);
						}
					}
					for (Id id : toLinkIds2Remove){
						lane.getToLinkIds().remove(id);
						Set<Id> toLinkIds = removedLinkIdToLinkIdsMap.get(id);
						for (Id toLinkId : toLinkIds){
							if (!removedLinkIdToLinkIdsMap.containsKey(toLinkId) && !lane.getToLinkIds().contains(toLinkId)){
								lane.getToLinkIds().add(toLinkId);
							}
						}
					}
				}
			}
		}
		return laneDefinitions;
	}

	private Map<Id, Set<Id>> removeJunctions(Network network, Map<String, Set<Node>> signalSystemId2NodesMap){
		Map<Id, Set<Id>> removedLinks = new HashMap<Id, Set<Id>>();
		for (String signalSystem : signalSystemId2NodesMap.keySet()){
			Set<Node> signalizedNodes = signalSystemId2NodesMap.get(signalSystem);
			if (signalizedNodes.size() > 1){
				//some output
				log.error("Signal System : " + signalSystem);
				for (Node n : signalizedNodes){
					log.error("  Node: " + n.getId());
				}
				
				Set<Link> linksBetweenSignalizedNodes = this.getLinksBetweenSignalizedNodes(signalizedNodes);
				//create and add new node
				Coord coord = this.getCoordBetweenSignalizedNodes(signalizedNodes);
				Id id = this.getId4NewNode4SignalizedNodes(signalizedNodes);
				Node newNode = network.getFactory().createNode(id, coord);
				network.addNode(newNode);
				//remove the links between the signalized nodes
				for (Link link : linksBetweenSignalizedNodes){
					Set<Id> set = new HashSet<Id>();
					for (Link toLink  : link.getToNode().getOutLinks().values()){ //persist the outlinks for lane postprocessing
						if (!linksBetweenSignalizedNodes.contains(toLink)){
							set.add(toLink.getId());
						}
					}
					removedLinks.put(link.getId(), set);
					network.removeLink(link.getId());
				}
				//remove the nodes
				for (Node node : signalizedNodes){
					for (Link link : node.getInLinks().values()){
						link.setToNode(newNode);
					}
					for (Link link : node.getOutLinks().values()){
						link.setFromNode(newNode);
					}
					node.getOutLinks().clear();
					node.getInLinks().clear();
					network.removeNode(node.getId());
				}
			}
		}
		return removedLinks;
	}
	
	
	private Id getId4NewNode4SignalizedNodes(Set<Node> signalizedNodes){
		StringBuilder builder = new StringBuilder();
		for (Node node : signalizedNodes){
			builder.append(node.getId().toString() + "_");
		}
		String id = builder.toString();
		return new IdImpl(id.substring(0, id.length() -1));
	}
	
	
	private Coord getCoordBetweenSignalizedNodes(Set<Node> signalizedNodes){
		return signalizedNodes.iterator().next().getCoord();
	}
	
	private Set<Link> getLinksBetweenSignalizedNodes(Set<Node> signalizedNodes) {
		Set<Link> linksBetweenSignalizedNodes = new HashSet<Link>();
		for (Node node : signalizedNodes){
			for (Link outLink : node.getOutLinks().values()){
				if (signalizedNodes.contains(outLink.getToNode())){
					linksBetweenSignalizedNodes.add(outLink);
				}
			}
		}
		return linksBetweenSignalizedNodes;
	}

	private Map<String, Set<org.matsim.api.core.v01.network.Node>> getSignalSystemId2NodeMap(OSMEntityCollector signalNodeCollector, String signalSystemKey, Network network){
		Map<Long, org.openstreetmap.osmosis.core.domain.v0_6.Node> osmNodes = signalNodeCollector.getAllNodes();
		Map<String, Set<org.matsim.api.core.v01.network.Node>> signalSystemIdString2OsmNodeMap = new HashMap<String, Set<org.matsim.api.core.v01.network.Node>>();
		for (org.openstreetmap.osmosis.core.domain.v0_6.Node node : osmNodes.values()){
//			log.error(node.getId());
			for (Tag tag : node.getTags()){
//				log.error("  tags: " + tag);
				if (tag.getKey().equalsIgnoreCase(signalSystemKey)){
					String signalSystemId = tag.getValue();
					Node matsimNode = network.getNodes().get(new IdImpl(node.getId()));
					if (matsimNode == null){
						log.error("Matsim network has no node with id " + node.getId() + " of signal system id " + signalSystemId);
						log.error("Removing node from postprocessing.");
						continue;
					}
					if (! signalSystemIdString2OsmNodeMap.containsKey(signalSystemId)){
						signalSystemIdString2OsmNodeMap.put(signalSystemId, new HashSet<Node>());
					}
					signalSystemIdString2OsmNodeMap.get(signalSystemId).add(matsimNode);
				}
			}
		}
		
		return signalSystemIdString2OsmNodeMap;
	}
	

	private OSMEntityCollector detectAndWriteSignalizedOsmNodes(String osmFile, String signalSystemKey){
		Set<String> emptyKeys = Collections.emptySet();
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put(signalSystemKey, new HashSet<String>());
		Set<String> btucKeys = new HashSet<String>();
		btucKeys.add(signalSystemKey);
		
		TagFilter tagFilterWays = new TagFilter("reject-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("reject-relations", emptyKeys, emptyKVs);
		TagFilter tagFilterNodes = new TagFilter("accept-node", btucKeys, tagKeyValues);
		
		FastXmlReader reader = new FastXmlReader(new File(osmFile), true, CompressionMethod.None);
		reader.setSink(tagFilterWays);
		tagFilterWays.setSink(tagFilterRelations);
		tagFilterRelations.setSink(tagFilterNodes);

		OSMEntityCollector signalizedOsmNodes = new OSMEntityCollector();
		tagFilterNodes.setSink(signalizedOsmNodes);
		reader.run();
		return signalizedOsmNodes;
	}

	
	
	private ScenarioImpl loadScenario(String net, String lanesInputFile){
		ScenarioImpl sc = new ScenarioImpl();
    sc.getConfig().network().setInputFile(net);
    if (lanesInputFile != null){
    	sc.getConfig().scenario().setUseLanes(true);
    	sc.getConfig().network().setLaneDefinitionsFile(lanesInputFile);
    }
    ScenarioLoader loader = new ScenarioLoaderImpl(sc);
    loader.loadScenario();
    return sc;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String osmFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/brandenburg_tagged.osm";
		String networkFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/network.xml";
		String networkOutFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/network_wo_junctions.xml";
		String lanesFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/lanes_cottbus_v20_jbol_c.xml";
		String lanesOutFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/lanes_cottbus_v20_jbol_c_wo_junctions.xml";
		
		new DgOsmJunctionsPostprocessing().postprocessJunctions(osmFile, networkFile, networkOutFile, lanesFile, lanesOutFile);
		

		
	}


}
