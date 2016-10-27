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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.tagfilter.v0_6.TagFilter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.FastXmlReader;

import playground.dgrether.DgPaths;
import playground.dgrether.osm.OSMEntityCollector;


/**
 * @author dgrether
 *
 */
public class DgOsmJunctionsPostprocessing {

	private static final Logger log = Logger.getLogger(DgOsmJunctionsPostprocessing.class);
	
	
	public void postprocessJunctions(String osmFile, String networkFile, String networkOutFile, String lanesFile, String lanesOutFile) {
		MutableScenario scenario = this.loadScenario(networkFile, lanesFile);
		Network network = scenario.getNetwork();
		String signalSystemKey = "btuc_signalsystem_id";

		OSMEntityCollector signalNodeCollector = this.detectAndWriteSignalizedOsmNodes(osmFile, signalSystemKey);
		Map<String, Set<Node>> signalSystemId2NodesMap = this.getSignalSystemId2NodeMap(signalNodeCollector, signalSystemKey, network);
		
		
		Map<Id<Link>, Set<Id<Link>>> removedLinkIdToLinkIdsMap = this.removeJunctions(network, signalSystemId2NodesMap);
		
		Lanes lanes = this.handleLanes((Lanes)scenario.getScenarioElement(Lanes.ELEMENT_NAME), removedLinkIdToLinkIdsMap);
		
		new NetworkWriter(network).write(networkOutFile);
		LanesWriter writerDelegate = new LanesWriter(lanes);
		writerDelegate.write(lanesOutFile);
		log.info("done!");
	}
	
	private Lanes handleLanes(Lanes laneDefinitions20, Map<Id<Link>, Set<Id<Link>>> removedLinkIdToLinkIdsMap) {
		for (LanesToLinkAssignment l2l : laneDefinitions20.getLanesToLinkAssignments().values()){
			if (removedLinkIdToLinkIdsMap.containsKey(l2l.getLinkId())){
				throw new IllegalStateException("Link Id " + l2l.getLinkId() + " was removed but has lanes attached, can't handle this automatically!");
			}
			for (Lane lane : l2l.getLanes().values()){
				Set<Id<Link>> toLinkIds2Remove = new HashSet<>();
				if (lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty()){
					for (Id<Link> toLinkId : lane.getToLinkIds()){
						if (removedLinkIdToLinkIdsMap.containsKey(toLinkId)){
							toLinkIds2Remove.add(toLinkId);
						}
					}
					for (Id<Link> id : toLinkIds2Remove){
						lane.getToLinkIds().remove(id);
						Set<Id<Link>> toLinkIds = removedLinkIdToLinkIdsMap.get(id);
						for (Id<Link> toLinkId : toLinkIds){
							if (!removedLinkIdToLinkIdsMap.containsKey(toLinkId) && !lane.getToLinkIds().contains(toLinkId)){
								lane.getToLinkIds().add(toLinkId);
							}
						}
					}
				}
			}
		}
		return laneDefinitions20;
	}

	private Map<Id<Link>, Set<Id<Link>>> removeJunctions(Network network, Map<String, Set<Node>> signalSystemId2NodesMap){
		Map<Id<Link>, Set<Id<Link>>> removedLinks = new HashMap<>();
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
				Id<Node> id = this.getId4NewNode4SignalizedNodes(signalizedNodes);
				Node newNode = network.getFactory().createNode(id, coord);
				network.addNode(newNode);
				//remove the links between the signalized nodes
				for (Link link : linksBetweenSignalizedNodes){
					Set<Id<Link>> set = new HashSet<>();
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
	
	
	private Id<Node> getId4NewNode4SignalizedNodes(Set<Node> signalizedNodes){
		StringBuilder builder = new StringBuilder();
		for (Node node : signalizedNodes){
			builder.append(node.getId().toString() + "_");
		}
		String id = builder.toString();
		return Id.create(id.substring(0, id.length() -1), Node.class);
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
					Node matsimNode = network.getNodes().get(Id.create(node.getId(), Node.class));
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

	
	
	private MutableScenario loadScenario(String net, String lanesInputFile){
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
    sc.getConfig().network().setInputFile(net);
    if (lanesInputFile != null){
    	sc.getConfig().qsim().setUseLanes(true);
    	sc.getConfig().network().setLaneDefinitionsFile(lanesInputFile);
    }
    ScenarioUtils.loadScenario(sc);
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
