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

package playground.sergioo.workplaceCapacities2012.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.commons.math3.ml.clustering.CentroidCluster;

import others.sergioo.util.clustering.Cluster;
import playground.sergioo.visualizer2D2012.LayersWindow;
import playground.sergioo.workplaceCapacities2012.hits.PointPerson;


public class ClustersWindow extends LayersWindow implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	public enum Options implements LayersWindow.Options {
		ZOOM("<html>Z<br>O<br>O<br>M</html>");
		private String caption;
		private Options(String caption) {
			this.caption = caption;
		}
		@Override
		public String getCaption() {
			return caption;
		}
	}
	public enum Labels implements LayersWindow.Labels {
		LINK("Link"),
		NODE("Node");
		private String text;
		private Labels(String text) {
			this.text = text;
		}
		@Override
		public String getText() {
			return text;
		}
	}
	
	//Attributes
	private JButton readyButton;
	
	//Methods
	public ClustersWindow(String title, Map<Integer, Cluster<Double>> clusters, int numTotalPoints) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout()); 
		layersPanels.put(PanelIds.ONE, new ClustersPanel(this, clusters, numTotalPoints));
		this.add(layersPanels.get(PanelIds.ONE), BorderLayout.CENTER);
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.getCaption());
			optionButton.setActionCommand(option.getCaption());
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
		labelsPanel.setLayout(new GridLayout(1,Labels.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labels[i].setBorder(null);
			labelsPanel.add(labels[i]);
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		pack();
	}
	public ClustersWindow(String title,	List<CentroidCluster<PointPerson>> clusters) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout()); 
		layersPanels.put(PanelIds.ONE, new ClustersPanel(this, clusters));
		this.add(layersPanels.get(PanelIds.ONE), BorderLayout.CENTER);
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.getCaption());
			optionButton.setActionCommand(option.getCaption());
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
		labelsPanel.setLayout(new GridLayout(1,Labels.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labels[i].setBorder(null);
			labelsPanel.add(labels[i]);
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		pack();
	}
	public void refreshLabel(playground.sergioo.visualizer2D2012.LayersWindow.Labels label) {
		labels[label.ordinal()].setText(((ClustersPanel)layersPanels.get(PanelIds.ONE)).getLabelText(label));
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	
	
}
