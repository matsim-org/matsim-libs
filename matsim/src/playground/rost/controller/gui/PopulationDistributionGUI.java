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

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.network.NetworkLayer;
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
import playground.rost.graph.block.Blocks;
import playground.rost.util.PathTracker;

public class PopulationDistributionGUI extends AbstractBasicMapGUIImpl {

	protected NetworkLayer network;
	protected BlockCreator popDistri;
	protected PopulationDensity popVis;
	
	public PopulationDistributionGUI(NetworkLayer network, Blocks blocks) {
		super("PPL Distribution");
		
		this.network = network;
		
		this.vMContainer = new VisModuleContainerImpl(this);
		this.vMContainer.addVisModule(new NodeVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new LinkVisModule(vMContainer, network));
		this.vMContainer.addVisModule(new MarkNodeVisModule(vMContainer, network));
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel btnPanel = new JPanel();
		JButton outputPopPoints = new JButton("write population points");
		outputPopPoints.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				writePopulationPoints(PathTracker.resolve("populationPoints"));
			}
		});
		btnPanel.add(outputPopPoints);
		mainPanel.add(btnPanel);
		
		popVis = new PopulationDensity(this, 10000);
		mainPanel.add(popVis);
		this.ownContainer = mainPanel;
		
		
		this.map = new PlacePplMap(10000, network, popVis);
		this.vMContainer.addVisModule(new PopulationDistributionPointVisModule(vMContainer, (PlacePplMap)this.map));
		this.vMContainer.addVisModule(new PopulationBlockVisModule(vMContainer, blocks));
		
		BoundingBox bBox = new BoundingBox();
		bBox.run(network);
		this.map.setBoundingBox(bBox);
		this.buildUI();
		this.map.addMapPaintCallback(this.vMContainer);
		this.map.addMapPaintCallback(this.popVis);
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
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws JAXBException 
	 */
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException, JAXBException {
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(network);
		nReader.parse(PathTracker.resolve("flatNetwork"));
		
		//read blocks
		Blocks blocks = Blocks.readXMLFile(network, PathTracker.resolve("flatNetworkBlocks"));
		
		
		new PopulationDistributionGUI(network, blocks);
		
		String[] formats = ImageIO.getReaderFormatNames();
		for(String s : formats)
			System.out.println(s);

	}
}
