/* *********************************************************************** *
 * project: org.matsim.*
 * TestNodeSelection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.knowledge.utils.GetAllIncludedLinks;

public class TestNodeSelection {
	
	NetworkLayer network;
	Map<Id, Node> selectedNodesMap;
	
	//final String networkFile = "C:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
	final String networkFile = "C:/Master_Thesis_HLI/Workspace/myMATSIM/mysimulations/kt-zurich/input/network.xml";
	final String kmzFile = "kmzFile.kmz";
	final String outputDirectory = "C:/Master_Thesis_HLI/Workspace/TestNetz"; 
		
	protected void init()
	{
		selectedNodesMap = new TreeMap<Id, Node>();
	}
	
	protected void loadNetwork()
	{
		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
	}
	
	protected void testDijkstraSelector()
	{
		// FreespeedTravelTimeCost als Massstab
		SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNodes().get(new IdImpl("545")), network.getNodes().get(new IdImpl("4058")), 1.0);
			
		// Fahrzeit als Massstab
		//SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 10000);

		// Weg als Massstab
		//SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 275000);
		// anderer Startpunkt: 2484		
		snd.addNodesToMap(selectedNodesMap);
		System.out.println("Found ... included Nodes: " + selectedNodesMap.size());
	}
	
	protected void testCircularSelector()
	{
		SelectNodesCircular snc = new SelectNodesCircular(network);
		
		//snc.getNodes(network.getNode("545"), 20000, selectedNodes);
		//snc.getNodes(network.getNode("4058"), 20000, selectedNodes);
		
		snc.getNodes(network.getNodes().get(new IdImpl("9582")), 20000, selectedNodesMap);
		System.out.println("Found ... included Nodes: " + selectedNodesMap.size());
	}
	
	// nicht selektierte Nodes aus dem Netzwerk entfernen
	protected void removeOtherNodes()
	{
		Map<Id, NodeImpl> nodesToRemove = new TreeMap<Id, NodeImpl>();
		
		// iterate over Array or Iteratable 
		for (NodeImpl node : network.getNodes().values())
		{
			if(!selectedNodesMap.containsKey(node.getId())) nodesToRemove.put(node.getId(), node);
		}

		for (NodeImpl node : nodesToRemove.values()) 
		{
			network.removeNode(node);
		}
	}
	
	protected void getIncludedLinks()
	{
		GetAllIncludedLinks gail = new GetAllIncludedLinks();
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.addAll(selectedNodesMap.values());
		gail.getAllLinks(network, nodes);
	}
	
	protected void createKMLFile()
	{
		KMLPersonWriter kmlWriter = new KMLPersonWriter();
		kmlWriter.setKmzFileName(kmzFile);
		kmlWriter.setOutputDirectory(outputDirectory);
		kmlWriter.setNetwork(network);
		
		kmlWriter.writeKnownNodes(false);
		kmlWriter.writeRouteNodes(false);
		kmlWriter.writeActivityLinks(false);
		kmlWriter.writeNetwork(true);
		
		kmlWriter.setCoordinateTransformation(new CH1903LV03toWGS84());
		kmlWriter.writeFile();
	}
	
	public static void main(String[] args)
	{
		Gbl.createConfig(null);
		
		TestNodeSelection tns = new TestNodeSelection();
		tns.init();
		tns.loadNetwork();
		tns.testDijkstraSelector();
		//tns.testCircularSelector();
		tns.getIncludedLinks();
		tns.removeOtherNodes();
		tns.createKMLFile();
		
		System.out.println("Done!");
	}
	
}
