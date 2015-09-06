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

package playground.sergioo.networkBusLaneAdder2012.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.sergioo.visualizer2D2012.LayersWindow;


public class BusLaneAdderWindow extends LayersWindow implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_NODES("<html>N<br/>O<br/>D<br/>E<br/>S</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		private String caption;
		private Options(String caption) {
			this.caption = caption;
		}
		@Override
		public String getCaption() {
			return caption;
		}
	}
	public enum Tool {
		FIND("Find address",0,0,2,1,"findAddress"),
		SELECT("Select path",2,0,2,1,"select"),
		ADD("Add new Links",4,0,2,1,"add"),
		SAVE("Save network",6,0,2,1,"save");
		String caption;
		int gx;int gy;
		int sx;int sy;
		String function;
		private Tool(String caption, int gx, int gy, int sx, int sy, String function) {
			this.caption = caption;
			this.gx = gx;
			this.gy = gy;
			this.sx = sx;
			this.sy = sy;
			this.function = function;
		}
	}
	public enum Labels implements LayersWindow.Labels {
		NODES("Nodes");
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
	private Network network;
	private String finalNetworkFile;
	CoordinateTransformation coordinateTransformation;
	
	//Methods
	public BusLaneAdderWindow(String title, Network network, File imageFile, double[] upLeft, double[] downRight, String finalNetworkFile, CoordinateTransformation coordinateTransformation) throws IOException {
		setTitle(title);
		this.finalNetworkFile = finalNetworkFile;
		this.network = network;
		NetworkTwoNodesPainter networkPainter = new NetworkTwoNodesPainter(network, Color.BLACK);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout()); 
		layersPanels.put(PanelIds.ONE, new BusLaneAdderPanel(this, networkPainter, imageFile, upLeft, downRight, coordinateTransformation));
		this.add(layersPanels.get(PanelIds.ONE), BorderLayout.CENTER);
		option = Options.ZOOM;
		JPanel toolsPanel = new JPanel();
		toolsPanel.setLayout(new GridBagLayout());
		for(Tool tool:Tool.values()) {
			JButton toolButton = new JButton(tool.caption);
			toolButton.setActionCommand(tool.name());
			toolButton.addActionListener(this);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = tool.gx;
			gbc.gridy = tool.gy;
			gbc.gridwidth = tool.sx;
			gbc.gridheight = tool.sy;
			toolsPanel.add(toolButton,gbc);
		}
		this.add(toolsPanel, BorderLayout.NORTH);
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
		super.setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	public void refreshLabel(playground.sergioo.visualizer2D2012.LayersWindow.Labels label) {
		labels[label.ordinal()].setText(((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).getLabelText(label));
	}
	public Network getNetwork() {
		return network;
	}
	public void findAddress() {
		((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).findAddress();
	}
	public void select() {
		((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).selectLinks();
	}
	public void save() {
		new NetworkWriter(network).write(finalNetworkFile);
	}
	public void exitSave() {
		new NetworkWriter(network).write(finalNetworkFile+"l");
	}
	public void add() {
		List<Link> links = ((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).getLinks();
		Node prevNode = links.get(0).getFromNode();
		for(Link link:links)
			if(!link.getAllowedModes().contains("bus")) {
				exitSave();
				JOptionPane.showMessageDialog(this, "Wrong path, network saved");
			}
		for(int i=0; i<links.size(); i++) {
			Link link = links.get(i);
			Node node = null;
			if(link.getNumberOfLanes()==1) {
				Set<String> modes = new HashSet<String>();
				modes.add("bus");
				link.setAllowedModes(modes);
				node = link.getToNode();
			}
			else {
				Node oldNode = link.getToNode();
				if(i==links.size()-1 || oldNode.getInLinks().size()+oldNode.getOutLinks().size()>2)
					node = oldNode;
				else {
					node = network.getFactory().createNode(Id.createNodeId("fl"+oldNode.getId().toString()), oldNode.getCoord());
					network.addNode(node);
				}
				LinkImpl newLink = (LinkImpl) network.getFactory().createLink(Id.createLinkId("fl"+link.getId().toString()), prevNode, node);
				Set<String> modes = new HashSet<String>();
				modes.add("car");
				newLink.setAllowedModes(modes);
				newLink.setCapacity(link.getCapacity());
				newLink.setFreespeed(link.getFreespeed());
				newLink.setLength(link.getLength());
				newLink.setNumberOfLanes(link.getNumberOfLanes()-1);
				newLink.setOrigId(((LinkImpl)link).getOrigId());
				newLink.setType(((LinkImpl)link).getType());
				network.addLink(newLink);
				Set<String> modes2 = new HashSet<String>();
				modes2.add("bus");
				link.setAllowedModes(modes2);
				link.setCapacity(900);
				link.setNumberOfLanes(1);
			}
			prevNode=node;
		}
		((BusLaneAdderPanel)layersPanels.get(PanelIds.ONE)).clearSelection();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		for(Tool tool:Tool.values())
			if(e.getActionCommand().equals(tool.name())) {
				try {
					Method m = BusLaneAdderWindow.class.getMethod(tool.function, new Class[] {});
					m.invoke(this, new Object[]{});
				} catch (SecurityException e1) {
					e1.printStackTrace();
				} catch (NoSuchMethodException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
				setVisible(true);
				repaint();
			}
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	
	//Main
	public static final void main(String[] args) throws NumberFormatException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Coord c1 = coordinateTransformation.transform(new Coord(Double.parseDouble(args[2]), Double.parseDouble(args[3])));
		Coord c2 = coordinateTransformation.transform(new Coord(Double.parseDouble(args[4]), Double.parseDouble(args[5])));
		new BusLaneAdderWindow("Bus lanes adder", scenario.getNetwork(), new File(args[1]), new double[]{c1.getX(), c1.getY()}, new double[]{c2.getX(), c2.getY()}, args[6], coordinateTransformation).setVisible(true);
	}

}
