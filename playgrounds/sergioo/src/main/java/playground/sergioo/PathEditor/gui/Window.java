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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.GTFS.Stop;
import playground.sergioo.GTFS.Trip;
import playground.sergioo.PathEditor.kernel.RoutePath;

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
		ADD("Add",0,0,2,1,"add"),
		REMOVE("Remove",0,1,2,1,"remove"),
		REMOVE_FROM("Remove From",2,0,2,1,"removeFrom"),
		REMOVE_TO("Remove To",2,1,2,1,"removeTo"),
		ROUTE("Route",4,0,4,2,"route"),
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
	//Constants
	private static int GAPX = 50;
	private static int GAPY = 120;
	public static int MAX_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width-GAPX;
	public static int MAX_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().width-GAPY;
	public static int FRAMESIZE = 50;
	
	//Attributes
	public static int width;
	public static int height;
	private PanelPathEditor panel;
	private RoutePath routePath;
	private RoutePath previous;
	private Option option;
	private int selectedLinkIndex = -1;
	private boolean finish = false;
	private String selectedStopId = "";
	private JLabel lblLink;
	private JLabel lblStop;
	private JLabel lblDistance;
	private JLabel lblNumCandidate;
	
	//Methods
	public Window(Network network, Trip trip, Map<String,Stop> stops) {
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		routePath = new RoutePath(network, trip, stops);
		option = Option.SELECT_LINK;
		panel = new PanelPathEditor(this);
		this.setSize(width+GAPX, height+GAPY);
		this.add(panel, BorderLayout.CENTER);
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
		buttonsPanel.setLayout(new GridLayout(Option.values().length,1));
		for(Option option:Option.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.name());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		lblLink=new JLabel("");
		lblStop=new JLabel("");
		lblDistance=new JLabel(routePath.getMinDistance()*6371000*Math.PI/180+"");
		lblNumCandidate=new JLabel(routePath.getNumCandidates()+"");
		infoPanel.setLayout(new GridLayout(1,4));
		infoPanel.add(lblLink);
		infoPanel.add(lblStop);
		infoPanel.add(lblDistance);
		infoPanel.add(lblNumCandidate);
		this.add(infoPanel, BorderLayout.SOUTH);
	}
	public Window(Network network, Trip trip, Map<String, Stop> stops,String[] linksS) {
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		List<Link> links = new ArrayList<Link>();
		for(String link:linksS)
			links.add(network.getLinks().get(link));
		routePath = new RoutePath(network, trip, stops, links);
		option = Option.SELECT_LINK;
		panel = new PanelPathEditor(this);
		this.setSize(width+GAPX, height+GAPY);
		this.add(panel, BorderLayout.CENTER);
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
		buttonsPanel.setLayout(new GridLayout(Option.values().length,1));
		for(Option option:Option.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.name());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		lblLink=new JLabel("");
		lblStop=new JLabel("");
		lblDistance=new JLabel(routePath.getMinDistance()*6371000*Math.PI/180+"");
		lblNumCandidate=new JLabel(routePath.getNumCandidates()+"");
		infoPanel.setLayout(new GridLayout(1,4));
		infoPanel.add(lblLink);
		infoPanel.add(lblStop);
		infoPanel.add(lblDistance);
		infoPanel.add(lblNumCandidate);
		this.add(infoPanel, BorderLayout.SOUTH);
	}
	public boolean isFinish() {
		return finish;
	}
	public Option getOption() {
		return option;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Option option:Option.values())
			if(e.getActionCommand().equals(option.name()))
				this.option = option;
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
	public void selectLink(double x, double y) {
		selectedLinkIndex = routePath.getIndexNearestLink(x, y);
		lblLink.setText(routePath.getLink(selectedLinkIndex).getId()+"("+selectedLinkIndex+")");
	}
	public void unselectLink(double x, double y) {
		selectedLinkIndex = -1;
		lblLink.setText("");
	}
	public void selectStop(double x, double y) {
		selectedStopId = routePath.getIdNearestStop(x, y);
		lblStop.setText(selectedStopId);
	}
	public void unselectStop(double x, double y) {
		selectedStopId = "";
		lblStop.setText("");
	}
	public void add() {
		panel.waitSecondCoord();
	}
	public void add(Coord second) {
		if(selectedLinkIndex!=-1) {
			routePath.addLink(selectedLinkIndex, second);
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
		lblDistance.setText(routePath.getMinDistance()*6371000*Math.PI/180+"");
	}
	public void decDistance() {
		routePath.decreaseMinDistance();
		lblDistance.setText(routePath.getMinDistance()*6371000*Math.PI/180+"");
	}
	public void incCandidates() {
		routePath.increaseNumCandidates();
		lblNumCandidate.setText(routePath.getNumCandidates()+"");
	}
	public void decCandidates() {
		routePath.decreaseNumCandidates();
		lblNumCandidate.setText(routePath.getNumCandidates()+"");
	}
	public void addLinkStop() {
		if(selectedLinkIndex!=-1 && !selectedStopId.equals(""))
			routePath.addLinkStop(selectedLinkIndex,selectedStopId);
	}
	public void romeveLinkStop() {
		if(!selectedStopId.equals(""))
			routePath.removeLinkStop(selectedStopId);
	}
	public void calculate() {
		routePath.initStops();
		routePath.calculatePath();
	}
	public void isOk() {
		selectedLinkIndex = routePath.isPathJoined();
		JOptionPane.showMessageDialog(this, selectedLinkIndex==-1?"Yes!":"No");
	}
	public void save() {
		finish  = true;
		this.setVisible(false);
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
	}
	public void decreaseSelectedLink() {
		selectedLinkIndex--;
		if(selectedLinkIndex<0)
			selectedLinkIndex=-1;
	}
	
}
