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

package playground.sergioo.PathEditor.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.GTFS.Stop;
import playground.sergioo.GTFS.Trip;
import playground.sergioo.PathEditor.kernel.RoutePath;
import playground.sergioo.PathEditor.kernel.RoutesPathsGenerator;
import util.geometry.Point2D;

public class Window extends JFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Enumerations
	public enum Option {
		SELECT_LINK("<html>S<br/>E<br/>L<br/>E<br/>C<br/>T<br/> <br/>L<br/>I<br/>N<br/>K</html>"),
		SELECT_STOP("<html>S<br/>E<br/>L<br/>E<br/>C<br/>T<br/> <br/>S<br/>T<br/>O<br/>P</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		public String caption;
		private Option(String caption) {
			this.caption = caption;
		}
	}
	public enum Tool {
		ADD("Add first",0,0,2,1,"addFirst"),
		ADD_NEXT("Add next",0,1,2,1,"addNext"),
		ROUTE("Route",2,0,2,1,"route"),
		REMOVE("Remove",2,1,2,1,"remove"),
		REMOVE_FROM("Remove From",4,0,4,1,"removeFrom"),
		REMOVE_TO("Remove To",4,1,4,1,"removeTo"),
		INCREASE_DISTANCE("+Distance",8,0,2,1,"incDistance"),
		DECREASE_DISTANCE("-Distance",8,1,2,1,"decDistance"),
		INCREASE_NUM_CANDIDATES("+Candidates",10,0,2,1,"incCandidates"),
		DECREASE_NUM_CANDIDATES("-Candidates",10,1,2,1,"decCandidates"),
		ADD_LINK_STOP("Add link to stop",12,0,2,1,"addLinkStop"),
		REMOVE_LINK_STOP("Remove link to stop",12,1,2,1,"removeLinkStop"),
		CALCULATE("Calculate!",14,0,4,2,"calculate"),
		IS_OK("Is ok?",18,0,4,2,"isOk"),
		SAVE("Save",22,0,4,2,"save");
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
	public enum Check {
		SHAPE_COST("Shape cost","ShapeCost"),
		INSIDE_STOPS("Stops inside","InsideStops"),
		ANGLE_SHAPE("Angle shape","AngleShape");
		String caption;
		String text;
		private Check(String caption,String text) {
			this.caption = caption;
			this.text = text;
		}
	}
	public enum Label {
		LINK("LinkText"),
		STOP("StopText"),
		MIN_DISTANCE("MinDistanceText"),
		NUM_CANDIDATES("NumCandidatesText"),
		US("UsText"),
		REPS("RepsText"),
		INSIDE_STOPS("InsideStopsText");
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
	public static int FRAMESIZE = 50;
	
	//Attributes
	public static int width;
	public static int height;
	private PanelPathEditor panel;
	private RoutePath routePath;
	private RoutePath previous;
	private Option option;
	private int selectedLinkIndex = -1;
	private String selectedStopId = "";
	public List<Link> links;
	private RoutesPathsGenerator routesPathsGenerator;
	private JButton saveButton;
	private JLabel[] labels;
	private JCheckBox[] checks;
	private boolean first;
	
	//Methods
	public Window(String title, Network network, Trip trip, Map<String,Stop> stops, List<Link> links, RoutesPathsGenerator routesPathsGenerator) {
		this(title,network,trip,stops,null,links,routesPathsGenerator);
	}
	public Window(String title, Network network, Trip trip, Map<String, Stop> stops,String[] linksS, List<Link> linksE, RoutesPathsGenerator routesPathsGenerator) {
		setTitle(title);
		this.routesPathsGenerator = routesPathsGenerator;
		this.links = linksE;
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		if(linksS==null)
			routePath = new RoutePath(network, trip, stops);
		else {
			List<Link> links = new ArrayList<Link>();
			for(String link:linksS)
				links.add(network.getLinks().get(new IdImpl(link)));
			routePath = new RoutePath(network, trip, stops, links);
		}
		option = Option.SELECT_LINK;
		panel = new PanelPathEditor(this);
		this.setSize(width+GAPX, height+GAPY);
		this.add(panel, BorderLayout.CENTER);
		JPanel toolsPanel = new JPanel();
		toolsPanel.setLayout(new GridBagLayout());
		for(Tool tool:Tool.values()) {
			JButton toolButton = new JButton(tool.caption);
			toolButton.setActionCommand(tool.name());
			if(tool.equals(Tool.SAVE)) {
				saveButton = toolButton;
				saveButton.setEnabled(false);
			}
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
		JPanel checksPanel = new JPanel();
		checksPanel.setLayout(new GridLayout(1,Check.values().length));
		checksPanel.setBorder(new TitledBorder("Algorithm options"));
		checks = new JCheckBox[Check.values().length];
		int c=0;
		for(Check check:Check.values()) {
			checks[c]=new JCheckBox(check.caption);
			checks[c].addActionListener(this);
			checks[c].setActionCommand(check.text);
			checksPanel.add(checks[c]);
			try {
				Method m = RoutePath.class.getMethod("isWith"+check.text, new Class[] {});
				checks[c].setSelected((Boolean) m.invoke(routePath, new Object[]{}));
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		infoPanel.add(checksPanel, BorderLayout.WEST);
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new GridLayout(1,Label.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JLabel[Label.values().length];
		int l=0;
		for(Label label:Label.values()) {
			try {
				Method m = RoutePath.class.getMethod("get"+label.text, new Class[] {});
				labels[l]=new JLabel(m.invoke(routePath, new Object[]{}).toString());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			labelsPanel.add(labels[l]);
			l++;
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		this.add(infoPanel, BorderLayout.SOUTH);
	}
	public Option getOption() {
		return option;
	}
	public String refreshLink() {
		return routePath.getLink(selectedLinkIndex).getId()+"("+selectedLinkIndex+")";
	}
	public String refreshStop() {
		return selectedStopId+"("+routePath.getIndexStop(selectedStopId)+")";
	}
	public void selectLink(double x, double y) {
		selectedLinkIndex = routePath.getIndexNearestLink(x, y);
		labels[Label.LINK.ordinal()].setText(refreshLink());
	}
	public void unselectLink(double x, double y) {
		selectedLinkIndex = -1;
		labels[Label.LINK.ordinal()].setText("");
	}
	public void selectStop(double x, double y) {
		selectedStopId = routePath.getIdNearestStop(x, y);
		labels[Label.STOP.ordinal()].setText(refreshStop());
	}
	public void unselectStop(double x, double y) {
		selectedStopId = "";
		labels[Label.STOP.ordinal()].setText("");
	}
	public void addFirst() {
		panel.waitSecondCoord();
		first = true;
	}
	public void addNext() {
		panel.waitSecondCoord();
		first = false;
	}
	public void add(Coord second) {
		if(first) {
			routePath.addLinkFirst(second);
			selectedLinkIndex = 0;
		}
		else if(selectedLinkIndex!=-1) {
			routePath.addLinkNext(selectedLinkIndex, second);
			selectedLinkIndex++;
		}
	}
	public void remove() {
		if(selectedLinkIndex!=-1)
			routePath.removeLink(selectedLinkIndex);
		if(selectedLinkIndex==routePath.links.size())
			selectedLinkIndex = -1;
	}
	public void removeFrom() {
		if(selectedLinkIndex!=-1)
			routePath.removeLinksFrom(selectedLinkIndex);
		selectedLinkIndex = -1;
	}
	public void removeTo() {
		if(selectedLinkIndex!=-1)
			routePath.removeLinksTo(selectedLinkIndex);
		selectedLinkIndex = -1;
	}
	public void route() {
		if(selectedLinkIndex!=-1)
			routePath.addShortestPath(selectedLinkIndex);
	}
	public void undo() {
		if(previous!=null) {
			routePath = previous;
			previous = null;
		}
	}
	public void incDistance() {
		routePath.increaseMinDistance();
		labels[Label.MIN_DISTANCE.ordinal()].setText(routePath.getMinDistanceText());
	}
	public void decDistance() {
		routePath.decreaseMinDistance();
		labels[Label.MIN_DISTANCE.ordinal()].setText(routePath.getMinDistanceText());
	}
	public void incCandidates() {
		routePath.increaseNumCandidates();
		labels[Label.NUM_CANDIDATES.ordinal()].setText(routePath.getNumCandidatesText());
	}
	public void decCandidates() {
		routePath.decreaseNumCandidates();
		labels[Label.NUM_CANDIDATES.ordinal()].setText(routePath.getNumCandidatesText());
	}
	public void addLinkStop() {
		if(selectedLinkIndex!=-1 && !selectedStopId.equals(""))
			if(!routePath.addLinkStop(selectedLinkIndex,selectedStopId)) {
				int res = JOptionPane.showConfirmDialog(this, "This is a fixed stop, are you sure do you want to change?");
				if(res == JOptionPane.YES_OPTION) {
					routePath.forceAddLinkStop(selectedLinkIndex,selectedStopId);
					try {
						routesPathsGenerator.restartTripsStops(selectedStopId);
					} catch (IOException e) {
						e.printStackTrace();
					}
					JOptionPane.showMessageDialog(this, "Restart the program");
				}
			}
	}
	public void removeLinkStop() {
		if(!selectedStopId.equals(""))
			routePath.removeLinkStop(selectedStopId);
	}
	public void calculate() {
		routePath.initStops();
		routePath.calculatePath();
	}
	public void changeUs() {
		routePath.setUs();
		labels[Label.US.ordinal()].setText(routePath.getUsText());
	}
	public void changeReps() {
		routePath.setReps();
		labels[Label.REPS.ordinal()].setText(routePath.getRepsText());
	}
	public void changeInStops() {
		routePath.setInStops();
		labels[Label.INSIDE_STOPS.ordinal()].setText(routePath.getInsideStopsText());
	}
	public void isOk() {
		Point2D center = panel.getCenter();
		Coord coord = new CoordImpl(center.getX(), center.getY());
		selectedLinkIndex = routePath.isPathJoined();
		if(selectedLinkIndex==-1) {
			selectedLinkIndex = routePath.isUs()?routePath.isPathWithoutUs():-1;
			if(selectedLinkIndex==-1) {
				selectedLinkIndex = routePath.isReps()?routePath.isPathWithoutRepeatedLink():-1;
				if(selectedLinkIndex==-1) {
					panel.withStops();
					selectedStopId = routePath.allStopsWithLink();
					if(selectedStopId.equals("")) {
						selectedStopId = routePath.isInStops()?routePath.allStopsWithCorrectLink():"";
						if(selectedStopId.equals("")) {
							selectedStopId = routePath.allStopsWithInRouteLink();
							if(selectedStopId.equals("")) {
								selectedLinkIndex = routePath.isFirstLinkWithStop();
								if(selectedLinkIndex==-1) {
									JOptionPane.showMessageDialog(this, "Yes!!!");
									saveButton.setEnabled(true);
									return;
								}
								else {
									JOptionPane.showMessageDialog(this, "No, the first link is not related to a stop");
									routePath.getLink(selectedLinkIndex).getCoord();
								}
							}
							else {
								JOptionPane.showMessageDialog(this, "No, the stop doesn't have a link inside the path");
								coord=routePath.getStop(selectedStopId);
							}					
						}
						else {
							JOptionPane.showMessageDialog(this, "No, the stop doesn't have a correct link");
							coord=routePath.getStop(selectedStopId);
						}
					}
					else {
						JOptionPane.showMessageDialog(this, "No, the stop doesn't have a link");
						coord = routePath.getStop(selectedStopId);
					}
				}
				else {
					JOptionPane.showMessageDialog(this, "No, the path has a repeated link");
					coord = routePath.getLink(selectedLinkIndex).getCoord();
				}
			}
			else {
				JOptionPane.showMessageDialog(this, "No, the path has a U turn");
				coord = routePath.getLink(selectedLinkIndex).getCoord();
			}
		}
		else {
			JOptionPane.showMessageDialog(this, "No, the path is not joined");
			coord = routePath.getLink(selectedLinkIndex).getCoord();
		}
		saveButton.setEnabled(false);
		panel.centerCamera(coord.getX(), coord.getY());
	}
	public void save() {
		links.addAll(routePath.getLinks());
	}
	public Collection<Coord> getPoints() {
		return routePath.getShapePoints();
	}
	public Collection<Coord> getStopPoints() {
		return routePath.getStopPoints();
	}
	public List<Link> getLinks() {
		return routePath.getLinks();
	}
	public Collection<Link> getStopLinks() {
		return routePath.getStopLinks();
	}
	public Link getSelectedLink() {
		return selectedLinkIndex==-1?null:routePath.getLink(selectedLinkIndex);
	}
	public Coord getSelectedStop() {
		return selectedStopId==""?null:routePath.getStop(selectedStopId);
	}
	public void increaseSelectedLink() {
		selectedLinkIndex++;
		if(selectedLinkIndex==routePath.getLinks().size())
			selectedLinkIndex=-1;
		labels[Label.LINK.ordinal()].setText(selectedLinkIndex==-1?"":refreshLink());
	}
	public void decreaseSelectedLink() {
		selectedLinkIndex--;
		if(selectedLinkIndex<0)
			selectedLinkIndex=-1;
		labels[Label.LINK.ordinal()].setText(selectedLinkIndex==-1?"":refreshLink());
	}
	public Collection<Link> getNetworkLinks(double xMin, double yMin, double xMax, double yMax) {
		return routePath.getNetworkLinks(xMin, yMin, xMax, yMax);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Option option:Option.values())
			if(e.getActionCommand().equals(option.name()))
				this.option = option;
		for(Check check:Check.values())
			if(e.getActionCommand().equals(check.text)) {
				try {
					Method m = RoutePath.class.getMethod("setWith"+check.text, new Class[] {});
					m.invoke(routePath, new Object[]{});
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
			}
		for(Tool tool:Tool.values())
			if(e.getActionCommand().equals(tool.name())) {
				try {
					Method m = Window.class.getMethod(tool.function, new Class[] {});
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
				panel.repaint();
			}
	}
}
