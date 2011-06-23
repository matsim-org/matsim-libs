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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class Window extends JFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Enumerations
	public enum Option {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		SELECT_LINE("<html>P<br/>O<br/>I<br/>N<br/>T</html>"),
		SELECT_POINT("<html>L<br/>I<br/>N<br/>E</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		public String caption;
		private Option(String caption) {
			this.caption = caption;
		}
	}
	public enum Label {
		LINK("LinkText"),
		NODE("NodeText"),
		LINE("LineText"),
		POINT("PointText");
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
	private PanelNetwork panel;
	private Network network;
	private Collection<Tuple<Coord,Coord>> lines;
	private Collection<Coord> points;
	private Option option;
	private Id selectedLinkId;
	private Id selectedNodeId;
	private Tuple<Coord,Coord> selectedLine;
	private Coord selectedPoint;
	private JButton saveButton;
	private JLabel[] labels;
	private JLabel[] lblCoords = {new JLabel(),new JLabel()};
	private boolean save = false;
	//Methods
	public Window(String title, Network network) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.network = network;
		lines = new ArrayList<Tuple<Coord,Coord>>();
		points = new ArrayList<Coord>();
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		option = Option.SELECT_LINK;
		panel = new PanelNetwork(this);
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
		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		saveButton.setActionCommand("Save");
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
	public boolean isSave() {
		return save;
	}
	public Link getSelectedLink() {
		if(selectedLinkId != null)
			return network.getLinks().get(selectedLinkId);
		return null;
	}
	public Node getSelectedNode() {
		if(selectedNodeId != null)
			return network.getNodes().get(selectedNodeId);
		return null;
	}
	public Tuple<Coord,Coord> getSelectedLine() {
		return selectedLine;
	}
	public Coord getSelectedPoint() {
		return selectedPoint;
	}
	public Collection<? extends Link> getNetworkLinks() {
		return network.getLinks().values();
	}
	public Collection<Link> getNetworkLinks(double xMin, double yMin, double xMax, double yMax) {
		Collection<Link> links =  new HashSet<Link>();
		for(Link link:network.getLinks().values()) {
			Coord from = link.getFromNode().getCoord();
			Coord to = link.getToNode().getCoord();
			if((xMin<from.getX()&&yMin<from.getY()&&xMax>from.getX()&&yMax>from.getY())||
					(xMin<to.getX()&&yMin<to.getY()&&xMax>to.getX()&&yMax>to.getY()))
				links.add(link);
		}
		return links;
	}
	public Collection<Tuple<Coord, Coord>> getLines() {
		return lines;
	}
	public Collection<Coord> getPoints() {
		return points;
	}
	private Id getIdNearestLink(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Link nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Link link: network.getLinks().values()) {
			double distance = ((LinkImpl) link).calcDistance(coord); 
			if(distance<nearestDistance) {
				nearest = link;
				nearestDistance = distance;
			}
		}
		return nearest.getId();
	}
	public Id getIdOppositeLink(Link link) {
		for(Link nLink: network.getLinks().values()) {
			if(nLink.getFromNode().equals(link.getToNode()) && nLink.getToNode().equals(link.getFromNode()))
				return nLink.getId();
		}
		return null;
	}
	private Id getIdNearestNode(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Node node:network.getNodes().values()) {
			double distance = CoordUtils.calcDistance(coord, node.getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = node;
			}
		}
		return nearest.getId();
	}
	private Tuple<Coord,Coord> getCoordsNearestLine(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Tuple<Coord,Coord> nearest = null;
		double nearestDistance = Double.POSITIVE_INFINITY;
		for(Tuple<Coord,Coord> line:lines) {
			double distance = CoordUtils.distancePointLinesegment(line.getFirst(), line.getSecond(), coord); 
			if(distance<nearestDistance) {
				nearest = line;
				nearestDistance = distance;
			}
		}
		return nearest;
	}
	private Coord getCoordNearestPoint(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		Coord nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Coord point:points) {
			double distance = CoordUtils.calcDistance(coord, point);
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = point;
			}
		}
		return nearest;
	}
	public void selectLink(double x, double y) {
		selectedLinkId = getIdNearestLink(x, y);
		labels[Label.LINK.ordinal()].setText(refreshLink());
	}
	public void selectOppositeLink() {
		selectedLinkId = getIdOppositeLink(network.getLinks().get(selectedLinkId));
		labels[Label.LINK.ordinal()].setText(selectedLinkId==null?"":refreshLink());
	}
	public void unselectLink() {
		selectedLinkId = null;
		labels[Label.LINK.ordinal()].setText("");
	}
	public void selectNode(double x, double y) {
		selectedNodeId = getIdNearestNode(x, y);
		labels[Label.LINK.ordinal()].setText(refreshNode());
	}
	public void unselectNode() {
		selectedNodeId = null;
		labels[Label.LINK.ordinal()].setText("");
	}
	public void selectLine(double x, double y) {
		selectedLine = getCoordsNearestLine(x, y);
		labels[Label.LINK.ordinal()].setText(refreshLine());
	}
	public void unselectLine() {
		selectedPoint = null;
		labels[Label.LINK.ordinal()].setText("");
	}
	public void selectPoint(double x, double y) {
		selectedPoint = getCoordNearestPoint(x, y);
		labels[Label.LINK.ordinal()].setText(refreshPoint());
	}
	public void unselectPoint() {
		selectedPoint = null;
		labels[Label.LINK.ordinal()].setText("");
	}
	private String refreshLink() {
		return selectedLinkId.toString();
	}
	private String refreshNode() {
		return selectedNodeId.toString();
	}
	private String refreshLine() {
		return selectedLine.getFirst().getX()+","+selectedLine.getFirst().getY()+" "+selectedLine.getSecond().getX()+","+selectedLine.getSecond().getY();
	}
	private String refreshPoint() {
		return selectedPoint.getX()+","+selectedPoint.getY();
	}
	public void addPoint(Coord point) {
		points.add(point);
	}
	public void addLine(Tuple<Coord,Coord> line) {
		lines.add(line);
	}
	public void setCoords(double x, double y) {
		NumberFormat nF = NumberFormat.getInstance();
		nF.setMaximumFractionDigits(4);
		nF.setMinimumFractionDigits(4);
		lblCoords[0].setText(nF.format(x)+" ");
		lblCoords[1].setText(" "+nF.format(y));
	}
	public void zoomIn(Coord coord) {
		panel.zoomIn(coord.getX(), coord.getY());
	}
	public void zoomOut(Coord coord) {
		panel.zoomOut(coord.getX(), coord.getY());
	}
	public void centerCamera(Coord coord) {
		panel.centerCamera(coord.getX(), coord.getY());
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Option option:Option.values())
			if(e.getActionCommand().equals(option.name()))
				this.option = option;
		if(e.getActionCommand().equals("Save")) {
			setVisible(false);
			save = true;
		}
	}
	
}
