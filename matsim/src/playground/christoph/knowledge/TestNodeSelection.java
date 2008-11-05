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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.knowledge.utils.GetAllIncludedLinks;
import playground.christoph.knowledge.utils.GetAllNodes;

public class TestNodeSelection {
	
	NetworkLayer network;
	Map<Id, Node> selectedNodesMap;
	
	final String networkFile = "D:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
	final String kmzFile = "kmzFile.kmz";
	final String outputDirectory = "D:/Master_Thesis_HLI/Workspace/TestNetz"; 
	
	protected void init()
	{
		selectedNodesMap = new TreeMap<Id, Node>();
	}
	
	protected void loadNetwork()
	{
		network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(networkFile);
	}
	
	protected void testDijkstraSelector()
	{
		// FreespeedTravelTimeCost als Massstab
		SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 1.1);
		
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
		
		snc.getNodes(network.getNode("9582"), 20000, selectedNodesMap);
		System.out.println("Found ... included Nodes: " + selectedNodesMap.size());
	}
	
	// nicht selektierte Nodes aus dem Netzwerk entfernen
	protected void removeOtherNodes()
	{
		Map<Id, Node> nodesToRemove = new TreeMap<Id, Node>();
		
		// iterate over Array or Iteratable 
		for (Node node : network.getNodes().values())
		{
			if(!selectedNodesMap.containsKey(node.getId())) nodesToRemove.put(node.getId(), node);
		}

		for (Node node : nodesToRemove.values()) 
		{
			network.removeNode(node);
		}
	}
	
	protected void getIncludedLinks()
	{
		GetAllIncludedLinks gail = new GetAllIncludedLinks();
		gail.getAllLinks(network, selectedNodesMap);
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
