/******************************************************************************
 *project: org.matsim.*
 * BlockGUI.java
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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.xml.sax.SAXException;

import playground.rost.controller.vismodule.VisModuleContainerImpl;
import playground.rost.controller.vismodule.implementations.BlockVisModule;
import playground.rost.controller.vismodule.implementations.LinkVisModule;
import playground.rost.controller.vismodule.implementations.MarkNodeVisModule;
import playground.rost.controller.vismodule.implementations.NodeVisModule;
import playground.rost.controller.vismodule.implementations.SimplePolygonVisModule;
import playground.rost.graph.BlockCreator;
import playground.rost.graph.BoundingBox;
import playground.rost.graph.block.Blocks;
import playground.rost.util.PathTracker;

public class BlockGUI extends AbstractBasicMapGUIImpl {

	protected NetworkLayer network;
	protected BlockCreator blockCreator;
	protected JButton btnWriteNetworkAndBlocks;
	protected JPanel pInfo;
	
	public BlockGUI(NetworkLayer network) {
		super("Block GUI");
		this.network = network;
		
		blockCreator = new BlockCreator(network);
		this.network = blockCreator.network;
		
		this.vMContainer = new VisModuleContainerImpl(this);
		this.vMContainer.addVisModule(new NodeVisModule(vMContainer, this.network));
		this.vMContainer.addVisModule(new LinkVisModule(vMContainer, this.network));
		this.vMContainer.addVisModule(new BlockVisModule(vMContainer, blockCreator.blocks));
		this.vMContainer.addVisModule(new SimplePolygonVisModule(vMContainer, "Border", blockCreator.borderNodes));
		this.vMContainer.addVisModule(new MarkNodeVisModule(vMContainer, this.network));
		
		BoundingBox bBox = new BoundingBox();
		bBox.run(network);
		this.map.setBoundingBox(bBox);
		this.buildUI();
		this.map.addMapPaintCallback(this.vMContainer);
		
		
	}
	
	protected void writeNetworkAndBlocks()
	{
		//write Network
		new NetworkWriter(network).write(PathTracker.resolve("flatNetwork"));
		Blocks blockCollection = new Blocks(blockCreator.blocks);
		try {
			blockCollection.writeXMLFile(PathTracker.resolve("flatNetworkBlocks"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void buildUI()
	{
		super.buildUI();
		
		btnWriteNetworkAndBlocks = new JButton("write network and block");
		btnWriteNetworkAndBlocks.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				writeNetworkAndBlocks();
			}
		});
		this.ownContainer.add(btnWriteNetworkAndBlocks);
		
		pInfo = new JPanel();
		pInfo.setLayout(new BoxLayout(pInfo, BoxLayout.Y_AXIS));
		JLabel lblCaption = new JLabel("area statistics");
		pInfo.add(lblCaption);
		JLabel lblBlocks = new JLabel("  blocks: " + blockCreator.areaBlockSum);
		pInfo.add(lblBlocks);
		JLabel lblBorder = new JLabel("  border: " + blockCreator.areaBorder);
		pInfo.add(lblBorder);
		this.ownContainer.add(pInfo);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		createBlocksAndShowGUI();
	}
	
	public static BlockGUI createBlocksAndShowGUI()
	{
		// TODO Auto-generated method stub
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(scenario);
		try {
			nReader.parse(PathTracker.resolve("matExtract"));
			return(new BlockGUI(network));
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
		return null;
	}

}
