/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.map2mapmatching.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.map2mapmatching.gui.MatchingsPainter.MatchingOptions;
import org.matsim.contrib.map2mapmatching.gui.core.Camera;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.LayersWindow;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainter;
import org.matsim.contrib.map2mapmatching.kernel.CrossingMatchingStep;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

public class DoubleNetworkCapacitiesWindow extends LayersWindow implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		A,
		B,
		ACTIVE,
		DOUBLE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
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
		APPLY_CAPACITY("Apply capacity",0,0,1,1,"applyCapacity"),
		APPLY_CAPACITY_OPP("Apply capacity opposite",1,0,1,1,"applyCapacityOpp"),
		APPLY_CAPACITY_PATH("Apply capacity to path",2,0,1,1,"applyCapacityPath"),
		DELETE_CAPACITY("Delete capacity",3,0,1,1,"deleteCapacity"),
		DELETE_CAPACITY_OPP("Delete capacity with opposite",4,0,1,1,"deleteCapacityOpp"),
		UNDO("Undo",5,0,1,1,"undo"),
		FIND_LINK("Find link",6,0,1,1,"findLink"),
		FIND_NODE("Find node",7,0,1,1,"findNode"),
		SAVE("Save",8,0,1,1,"save");
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
		LINK("Link"),
		NODE("Node"),
		ACTIVE("Active");
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
	private boolean networksSeparated = true;
	private JPanel panelsPanel;
	private Map<Link, Tuple<Link,Double>> linksChanged;
	private Map<Link, Tuple<Link,Double>> undoLinksChanged;
	private Node firstNode;
	private AStarLandmarks aStarLandmarksB;

	//Methods
	private DoubleNetworkCapacitiesWindow(String title) {
		setTitle(title);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.getCaption());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new FlowLayout());
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
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
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	public DoubleNetworkCapacitiesWindow(String title, Network networkA, Network networkB, Map<Link, Tuple<Link,Double>> linksChanged) {
		this(title);
		this.linksChanged = linksChanged;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		option = Options.SELECT_LINK;
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
		layersPanels.put(PanelIds.A, new NetworkCapacitiesPanel(MatchingOptions.A, this, new NetworkPainter(networkA, new Color(150,150,255)), new NetworkCapacitiesPainter(networkA, true, linksChanged)));
		layersPanels.put(PanelIds.B, new NetworkCapacitiesPanel(MatchingOptions.B, this, new NetworkPainter(networkB, new Color(150,150,255)), new NetworkCapacitiesPainter(networkB, false, linksChanged)));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A));
		panelsPanel.add(layersPanels.get(PanelIds.B));
		this.add(panelsPanel, BorderLayout.CENTER);
		TravelDisutility travelMinCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		TravelTime timeFunction = new TravelTime() {	

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(networkB);
		aStarLandmarksB = new AStarLandmarks(networkB, preProcessData, timeFunction);
	}
	public void cameraChange(Camera camera) {
		if(networksSeparated) {
			if(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)) {
				layersPanels.get(PanelIds.B).getCamera().copyCamera(camera);
				layersPanels.get(PanelIds.B).repaint();
			}
			else {
				layersPanels.get(PanelIds.A).getCamera().copyCamera(camera);
				layersPanels.get(PanelIds.A).repaint();
			}
		}
	}
	public void setActivePanel(LayersPanel panel) {
		layersPanels.put(PanelIds.ACTIVE, panel);
	}
	public void applyCapacity() {
		saveUndoLinksChanged();
		Link linkA=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.A)).getSelectedLink();
		Link linkB=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getSelectedLink();
		if(linkA!=null && linkB!=null)
			linksChanged.put(linkB,new Tuple<Link, Double>(linkA, linkA.getCapacity()));
	}
	public void applyCapacityOpp() {
		applyCapacity();
		Link linkA=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.A)).getSelectedLink();
		Link linkBOpp=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getOppositeToSelectedLink();
		if(linkA!=null && linkBOpp!=null)
			linksChanged.put(linkBOpp,new Tuple<Link, Double>(linkA, linkA.getCapacity()));
	}
	public void applyCapacityPath() {
		firstNode =((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getSelectedNode();
		if(firstNode!=null)
			((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).setPathActive();
	}
	public void applyCapacityPath2() {
		saveUndoLinksChanged();
		Link linkA=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.A)).getSelectedLink();
		Node secondNode=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getSelectedNode();
		Path path = aStarLandmarksB.calcLeastCostPath(firstNode, secondNode, 0, null, null);
		if(path!=null && linkA!=null)
			for(Link linkB:path.links)
				linksChanged.put(linkB,new Tuple<Link, Double>(linkA, linkA.getCapacity()));
	}
	public void deleteCapacity() {
		saveUndoLinksChanged();
		Link linkB = ((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getSelectedLink();
		if(linkB!=null)
			linksChanged.remove(linkB);
	}
	public void deleteCapacityOpp() {
		deleteCapacity();
		Link linkBOpp=((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).getOppositeToSelectedLink();
		if(linkBOpp!=null)
			linksChanged.remove(linkBOpp);
	}
	private void saveUndoLinksChanged() {
		undoLinksChanged = new HashMap<Link, Tuple<Link,Double>>();
		for(Entry<Link, Tuple<Link,Double>> linkEntry:linksChanged.entrySet())
			undoLinksChanged.put(linkEntry.getKey(), new Tuple<Link, Double>(linkEntry.getValue().getFirst(), linkEntry.getValue().getSecond()));
	}
	public void undo() {
		if(undoLinksChanged!=null) {
			linksChanged.clear();
			for(Entry<Link, Tuple<Link,Double>> linkEntry:undoLinksChanged.entrySet())
				linksChanged.put(linkEntry.getKey(), new Tuple<Link, Double>(linkEntry.getValue().getFirst(), linkEntry.getValue().getSecond()));
			undoLinksChanged = null;
		}
	}
	public void findLink() {
		String res = JOptionPane.showInputDialog("Please write \"A\" for the left network and \"B\" for the right one");
		if(res!=null && (res.equals("A") || res.equals("B"))) {
			boolean isA = true;
			if(res.equals("B"))
				isA = false;
			res = JOptionPane.showInputDialog("Please write the link Id");
			if(res!=null && !res.equals(""))
				if(isA)
					((NetworkCapacitiesPanel)layersPanels.get(PanelIds.A)).selectLink(res);
				else
					((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).selectLink(res);
		}
	}
	public void findNode() {
		String res = JOptionPane.showInputDialog("Please write \"A\" for the left network and \"B\" for the right one");
		if(res!=null && (res.equals("A") || res.equals("B"))) {
			boolean isA = true;
			if(res.equals("B"))
				isA = false;
			res = JOptionPane.showInputDialog("Please write the node Id");
			if(res!=null && !res.equals(""))
				if(isA)
					((NetworkCapacitiesPanel)layersPanels.get(PanelIds.A)).selectNode(res);
				else
					((NetworkCapacitiesPanel)layersPanels.get(PanelIds.B)).selectNode(res);
		}
	}
	public void centerCamera(double[] coord) {
		layersPanels.get(PanelIds.A).centerCamera(coord);
		layersPanels.get(PanelIds.B).centerCamera(coord);
	}
	public void save() {
		try {
			PrintWriter writer = new PrintWriter(CrossingMatchingStep.CAPACITIES_FILE);
			for(Entry<Link, Tuple<Link,Double>> linkE:linksChanged.entrySet())
				writer.println(linkE.getKey().getId()+":::"+linkE.getValue().getFirst().getId()+":::"+linkE.getValue().getSecond());
			writer.close();
			JOptionPane.showMessageDialog(this, "Saved");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		for(Tool tool:Tool.values())
			if(e.getActionCommand().equals(tool.name())) {
				try {
					Method m = DoubleNetworkCapacitiesWindow.class.getMethod(tool.function, new Class[] {});
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
	}
	@Override
	public void refreshLabel(LayersWindow.Labels label) {
		if(label.equals(Labels.ACTIVE))
			labels[label.ordinal()].setText(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)?"A":"B");
		else
			labels[label.ordinal()].setText(((NetworkCapacitiesPanel)layersPanels.get(PanelIds.ACTIVE)).getLabelText(label));
		repaint();
	}
	@Override
	public void dispose() {
		int res = JOptionPane.showConfirmDialog(this, "Do you want to save the capacities?");
		if(res == JOptionPane.YES_OPTION)
			save();
		if(!(res == JOptionPane.CANCEL_OPTION)) {
			this.setVisible(false);
			readyToExit = true;
		}
	}
	
}
