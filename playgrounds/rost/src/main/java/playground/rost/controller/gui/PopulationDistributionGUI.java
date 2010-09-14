/******************************************************************************
 *project: org.matsim.*
 * PopulationDistributionGUI.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.rost.controller.map.PlacePplMap;
import playground.rost.controller.uicontrols.PopulationDensity;
import playground.rost.controller.vismodule.VisModuleContainerImpl;
import playground.rost.controller.vismodule.implementations.LinkVisModule;
import playground.rost.controller.vismodule.implementations.MarkNodeVisModule;
import playground.rost.controller.vismodule.implementations.NodeVisModule;
import playground.rost.controller.vismodule.implementations.PopulationBlockVisModule;
import playground.rost.controller.vismodule.implementations.PopulationDistributionPointVisModule;
import playground.rost.graph.BlockCreator;
import playground.rost.graph.BoundingBox;
import playground.rost.graph.PopulateBlocks;
import playground.rost.graph.block.Block;
import playground.rost.graph.block.Blocks;
import playground.rost.graph.evacarea.EvacArea;
import playground.rost.graph.nodepopulation.PopulationNodeMap;
import playground.rost.graph.populationpoint.PopulationPointCollection;
import playground.rost.util.PathTracker;

public class PopulationDistributionGUI extends AbstractBasicMapGUIImpl {

	private static final Logger log = Logger.getLogger(PopulationDistributionGUI.class);
	
	protected NetworkImpl network;
	protected BlockCreator popDistri;
	protected PopulationDensity popVis;
	protected Blocks blocks;
	protected PlacePplMap pplMap;
	
	public PopulationDistributionGUI(NetworkImpl network, Blocks blocks) {
		super("PPL Distribution");
		
		this.network = network;
		this.blocks = blocks;
		
		this.vMContainer = new VisModuleContainerImpl(this);
		this.vMContainer.addVisModule(new NodeVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new LinkVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new MarkNodeVisModule(vMContainer, network));
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel btnPanel = new JPanel();
		
		JButton readPopPoints = new JButton("read population points");
		readPopPoints.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				readPopulationPoints(PathTracker.resolve("populationPoints"));
			}
		});
		btnPanel.add(readPopPoints);
		
		JButton outputPopPoints = new JButton("write population points");
		outputPopPoints.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				writePopulationPoints(PathTracker.resolve("populationPoints"));
			}
		});
		
		JButton outputNodePopulation = new JButton("write population for nodes in original network");
		
		outputNodePopulation.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				outputDistributionForNodes(PathTracker.resolve("populationForNodes"));
			}
		});
		
		
		btnPanel.add(outputPopPoints);
		btnPanel.add(outputNodePopulation);
		mainPanel.add(btnPanel);
		
		popVis = new PopulationDensity(this, 10000);
		mainPanel.add(popVis);
		this.ownContainer = mainPanel;
		
		
		this.map = new PlacePplMap(10000, network, popVis);
		this.pplMap = (PlacePplMap)this.map;
		this.vMContainer.addVisModule(new PopulationDistributionPointVisModule(vMContainer, (PlacePplMap)this.map));
		this.vMContainer.addVisModule(new PopulationBlockVisModule(vMContainer, blocks));
		
		BoundingBox bBox = new BoundingBox();
		bBox.run(network);
		this.map.setBoundingBox(bBox);
		this.buildUI();
		this.map.addMapPaintCallback(this.vMContainer);
		this.map.addMapPaintCallback(this.popVis);
	}
	
	protected void readPopulationPoints(String filename)
	{
		PopulationPointCollection pplPoints;
		try {
			pplPoints = PopulationPointCollection.readXMLFile(filename);
			if(pplPoints.get().size() > 0)
				((PlacePplMap)this.map).setPplPoints(pplPoints);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	protected void writePopulationPoints(String fileName)
	{
		try {
			((PlacePplMap)this.map).getPplPoints().writeXMLFile(fileName);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void outputDistributionForNodes(String fileName)
	{
		//load evacuation area of original network
		EvacArea evacArea = null;
		try {
			evacArea = EvacArea.readXMLFile(PathTracker.resolve("evacArea"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(evacArea != null)
		{
			//calculate the population for every block
			Map<Block, Integer> populationOfBlock = PopulateBlocks.getPopulation(pplMap.getPplPoints(), blocks, pplMap.getNetwork());
			
			//calculate the population for every single Node
			PopulationNodeMap nodePopulationMap = new PopulationNodeMap();
			
			for(Block b : populationOfBlock.keySet())
			{
				Set<String> ids = new HashSet<String>();
				for(Node n : b.border)
				{
					String id = n.getId().toString();
					if(evacArea.evacAreaNodeIds.contains(id) ||
						evacArea.evacBorderNodeIds.contains(id))
					{
						ids.add(id);
					}
				}
				for(String id : ids)
				{
					Integer population = (int)(populationOfBlock.get(b) * b.area_size / ids.size());
					if(nodePopulationMap.populationForNode.containsKey(id))
					{
						population += nodePopulationMap.populationForNode.get(id);
					}
					nodePopulationMap.populationForNode.put(id, population);
				}
			}
			try {
				nodePopulationMap.writeXMLFile(PathTracker.resolve("populationForNodes"));
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws JAXBException 
	 */
	public static void main(String[] args) {
		parseNetworkAndBlocksAndShowGUI();
	}
	
	public static PopulationDistributionGUI parseNetworkAndBlocksAndShowGUI()
	{
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(scenario);
		try {
			nReader.parse(PathTracker.resolve("flatNetwork"));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//read blocks
		Blocks blocks = null;
		try {
			blocks = Blocks.readXMLFile(network, PathTracker.resolve("flatNetworkBlocks"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return(new PopulationDistributionGUI(network, blocks));

	}
}
