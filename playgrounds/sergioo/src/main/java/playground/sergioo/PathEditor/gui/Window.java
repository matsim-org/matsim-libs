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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.GTFS.Stop;
import playground.sergioo.GTFS.Trip;
import playground.sergioo.PathEditor.kernel.RoutePath;

public class Window extends JFrame implements ActionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Enumerations
	public enum Option {
		SELECT,
		ZOOM;
	}
	public enum Tool {
		ADD("Add",0,0,4,1,"add"),
		REMOVE("Remove",0,1,4,1,"remove"),
		ROUTE("Route",4,0,2,2,"route"),
		UNDO("Undo",6,0,2,2,"undo"),
		INCREASE_DISTANCE("+Distance",8,0,2,1,"incDistance"),
		DECREASE_DISTANCE("-Distance",8,1,2,1,"decDistance"),
		INCREASE_NUM_CANDIDATES("+Candidates",10,0,2,1,"incCandidates"),
		DECREASE_NUM_CANDIDATES("-Candidates",10,1,2,1,"decDistance"),
		CALCULATE("Calculate!",12,0,4,2,"calculate"),
		IS_OK("Is ok?",16,0,4,2,"isOk"),
		SAVE("Save",20,0,4,2,"save");
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
	public static int MAX_WIDTH = 1920;
	public static int MAX_HEIGHT = 800;
	public static int FRAMESIZE = 50;
	
	//Attributes
	public static int width;
	public static int height;
	private PanelPathEditor panel;
	private RoutePath routePath;
	private RoutePath previous;
	private Option option;
	private int selectedIndex = -1;
	private boolean finish = false;
	
	//Methods
	public Window(Network network, Trip trip, Map<String,Stop> stops) {
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		routePath = new RoutePath(network, trip, stops);
		option = Option.SELECT;
		panel = new PanelPathEditor(this);
		this.setSize(width, height+280);
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
		buttonsPanel.setLayout(new GridLayout(1,Option.values().length));
		for(Option option:Option.values()) {
			JButton optionButton = new JButton(option.name());
			optionButton.setActionCommand(option.name());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.SOUTH);
		addKeyListener(this);
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
		selectedIndex = routePath.getIndexNearestLink(x, y);
	}
	public void unselectLink(double x, double y) {
		selectedIndex = -1;
	}
	public void add() {
		panel.waitSecondCoord();
	}
	public void add(Coord second) {
		if(selectedIndex!=-1) {
			routePath.addLink(selectedIndex, second);
			selectedIndex++;
		}
	}
	public void remove() {
		if(selectedIndex!=-1)
			routePath.removeLink(selectedIndex);
		if(selectedIndex==routePath.links.size())
			selectedIndex = -1;
	}
	public void route() {
		if(selectedIndex!=-1)
			routePath.addShortestPath(selectedIndex);
	}
	public void undo() {
		if(previous!=null) {
			routePath = previous;
			previous = null;
		}
	}
	public void incDistance() {
		routePath.increaseMinDistance();
	}
	public void decDistance() {
		routePath.decreaseMinDistance();
	}
	public void incCandidates() {
		routePath.increaseNumCandidates();
	}
	public void decCandidates() {
		routePath.decreaseNumCandidates();
	}
	public void calculate() {
		routePath.calculatePath();
	}
	public void isOk() {
		selectedIndex = routePath.isPathJoined();
		JOptionPane.showMessageDialog(this, selectedIndex==-1?"Yes!":"No");
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
	public Link getSelectedLink() {
		return selectedIndex==-1?null:routePath.getLink(selectedIndex);
	}
	@Override
	public void keyTyped(KeyEvent e) {
		if(e.getKeyCode()==KeyEvent.VK_RIGHT) {
			selectedIndex++;
			if(selectedIndex==routePath.getLinks().size())
				selectedIndex=-1;
		}
		else if(e.getKeyCode()==KeyEvent.VK_LEFT) {
			selectedIndex--;
			if(selectedIndex<0)
				selectedIndex=-1;
		}
		panel.repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
