/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.sergioo.NetworkVisualizer.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkPainter;


public class SimpleNetworkWindow extends NetworkWindow {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Constants
	private static int GAPX = 50;
	private static int GAPY = 120;
	public static int MAX_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width-GAPX;
	public static int MAX_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height-GAPY;
	public static int MIN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width/2;
	public static int MIN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height/3;
	public static int FRAMESIZE = 20;
	
	//Attributes
	private JButton readyButton;
	
	//Methods
	public SimpleNetworkWindow(String title, NetworkPainter networkPainter) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		panel = new NetworkPanel(this, networkPainter);
		this.setSize(width+GAPX, height+GAPY);
		this.add(panel, BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Option.values().length,1));
		for(Option option:Option.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.name());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		readyButton = new JButton("Ready to exit");
		readyButton.addActionListener(this);
		readyButton.setActionCommand(READY_TO_EXIT);
		infoPanel.add(readyButton, BorderLayout.WEST);
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new GridLayout(1,Label.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JLabel[Label.values().length];
		labels[0]=new JLabel("");
		labelsPanel.add(labels[0]);
		labels[1]=new JLabel("");
		labelsPanel.add(labels[1]);
		infoPanel.add(labelsPanel, BorderLayout.CENTER);JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
	}
	@Override
	public void cameraChange(Camera camera) {
		
	}
	
}
