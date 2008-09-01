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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.knowledge.utils.GetAllIncludedLinks;
import playground.christoph.knowledge.utils.GetAllNodes;

public class TestNodeSelection {
	
	NetworkLayer network;
	ArrayList<Node> selectedNodes;
	
	final String networkFile = "D:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
	final String kmzFile = "D:/Master_Thesis_HLI/Workspace/TestNetz/kmzFile.kmz";
	
	protected void init()
	{
		selectedNodes = new ArrayList<Node>();
	}
	
	protected void loadNetwork()
	{
		network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(networkFile);
	}
	
	protected void testDijkstraSelector()
	{
		// FreespeedTravelTimeCost als Massstab
		SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 17.0);
		
		// Fahrzeit als Massstab
		//SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 10000);

		// Weg als Massstab
		//SelectNodesDijkstra snd = new SelectNodesDijkstra(network, network.getNode("545"), network.getNode("4058"), 275000);
		// anderer Startpunkt: 2484		
		snd.getNodes(selectedNodes);
		System.out.println("Found ... included Nodes: " + selectedNodes.size());
	}
	
	protected void testCircularSelector()
	{
		SelectNodesCircular snc = new SelectNodesCircular(network);
		
		//snc.getNodes(network.getNode("545"), 20000, selectedNodes);
		//snc.getNodes(network.getNode("4058"), 20000, selectedNodes);
		
		snc.getNodes(network.getNode("9582"), 20000, selectedNodes);
		System.out.println("Found ... included Nodes: " + selectedNodes.size());
	}
	
	// nicht selektierte Nodes aus dem Netzwerk entfernen
	protected void removeOtherNodes()
	{
		ArrayList<Node> allNodes = new GetAllNodes().getAllNodes(network);
		
		for(int i = 0; i < allNodes.size(); i++)
		{
			if(!selectedNodes.contains(allNodes.get(i))) network.removeNode(allNodes.get(i));
		}
	}
	
	protected void getIncludedLinks()
	{
		GetAllIncludedLinks gail = new GetAllIncludedLinks();
		gail.getAllLinks(network, selectedNodes);
		
	}
	
	protected void createKMLFile()
	{
		MyKMLNetWriterTest kmlWriter = new MyKMLNetWriterTest();
		kmlWriter.setKmzFileName(kmzFile);
		kmlWriter.setNetwork(network);
		kmlWriter.createKmzFile();
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
