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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class Window extends JFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Enumerations
	public enum Option {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_STOP("<html>S<br/>T<br/>O<br/>P</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		public String caption;
		private Option(String caption) {
			this.caption = caption;
		}
	}
	public enum Label {
		LINK("LinkText"),
		STOP("StopText");
		String text;
		private Label(String text) {
			this.text = text;
		}
	}
	//Constants
	private static int GAPX = 50;
	private static int GAPY = 120;
	public static int MAX_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width-GAPX;
	public static int MAX_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height-GAPY;
	public static int MIN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width/2;
	public static int MIN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height/3;
	public static int FRAMESIZE = 20;
	
	//Attributes
	public static int width;
	public static int height;
	private PanelPathEditor panel;
	private Network network;
	private Option option;
	private String selectedLinkId = "";
	private String selectedStopId = "";
	private Node selectedNode = null;
	public List<Link> links;
	private JButton saveButton;
	private JLabel[] labels;
	private JLabel[] lblCoords = {new JLabel(),new JLabel()};
	//Methods
	public Window(String title, Network network) {
		setTitle(title);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.network = network;
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		option = Option.ZOOM;
		panel = new PanelPathEditor(this);
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
		saveButton = new JButton("Exit");
		saveButton.addActionListener(this);
		saveButton.setActionCommand("Exit");
		infoPanel.add(saveButton, BorderLayout.WEST);
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
	public Option getOption() {
		return option;
	}
	public String refreshLink() {
		return selectedLinkId;
	}
	public String refreshStop() {
		return selectedStopId;
	}
	public void selectLink(double x, double y) {
		selectedLinkId = getIndexNearestLink(x, y);
		labels[Label.LINK.ordinal()].setText(refreshLink());
	}
	private String getIndexNearestLink(double x, double y) {
		// TODO Auto-generated method stub
		return null;
	}
	public void unselectLink() {
		selectedLinkId = "";
		labels[Label.LINK.ordinal()].setText("");
	}
	public void selectNode(double x, double y) {
		selectedNode = getNearestNode(x, y);
	}
	private Node getNearestNode(double x, double y) {
		// TODO Auto-generated method stub
		return null;
	}
	public void unselectNode() {
		selectedNode = null;
	}
	public Link getSelectedLink() {
		return selectedLinkId==""?null:getLink(selectedLinkId);
	}
	private Link getLink(String selectedLinkId2) {
		// TODO Auto-generated method stub
		return null;
	}
	public Node getSelectedNode() {
		return selectedNode;
	}
	public Collection<Link> getNetworkLinks(double xMin, double yMin, double xMax, double yMax) {
		//TODO
		return null;
	}
	public void setCoords(double x, double y) {
		NumberFormat nF = NumberFormat.getInstance();
		nF.setMaximumFractionDigits(4);
		nF.setMinimumFractionDigits(4);
		lblCoords[0].setText(nF.format(x)+" ");
		lblCoords[1].setText(" "+nF.format(y));
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Option option:Option.values())
			if(e.getActionCommand().equals(option.name()))
				this.option = option;
		if(e.getActionCommand().equals("Exit"))
			setVisible(false);
	}
	
}
