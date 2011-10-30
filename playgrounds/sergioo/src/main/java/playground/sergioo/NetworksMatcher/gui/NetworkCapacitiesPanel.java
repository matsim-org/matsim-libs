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

package playground.sergioo.NetworksMatcher.gui;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.sergioo.NetworksMatcher.gui.DoubleNetworkCapacitiesWindow.Labels;
import playground.sergioo.NetworksMatcher.gui.DoubleNetworkCapacitiesWindow.Options;
import playground.sergioo.NetworksMatcher.gui.MatchingsPainter.MatchingOptions;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainterManager;

public class NetworkCapacitiesPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final DoubleNetworkCapacitiesWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	
	//Methods
	public NetworkCapacitiesPanel(MatchingOptions matchingOption, DoubleNetworkCapacitiesWindow doubleNetworkWindow, NetworkPainter networkPainter, NetworkPainter linksPainter) {
		super();
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(networkPainter));
		addLayer(new Layer(linksPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public void setNetwork(Network network) {
		((NetworkNodesPainter)getLayer(0).getPainter()).setNetwork(network);
	}
	public Link getSelectedLink() {
		return ((NetworkPainterManager)((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager()).getSelectedLink();
	}
	public void selectLink(String id) {
		Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(id);
		if(link!=null)
			doubleNetworkWindow.centerCamera(link.getCoord());
	}
	public void selectNode(String id) {
		Node node = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectNode(id);
		if(node!=null)
			doubleNetworkWindow.centerCamera(node.getCoord());
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetwork().getLinks().values()) {
			if(link!=null) {
				coords.add(link.getFromNode().getCoord());
				coords.add(link.getToNode().getCoord());
			}
		}
		super.calculateBoundaries(coords);
	}
	public String getLabelText(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
		try {
			return (String) NetworkPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return "";
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		doubleNetworkWindow.setActivePanel(this);
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().selectLink(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().unselectLink();
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().selectNode(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().unselectNode();
				doubleNetworkWindow.refreshLabel(Labels.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
				doubleNetworkWindow.cameraChange(camera);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
				doubleNetworkWindow.cameraChange(camera);
			}
		}
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		this.requestFocus();
		doubleNetworkWindow.setActivePanel(this);
		doubleNetworkWindow.refreshLabel(Labels.ACTIVE);
		iniX = e.getX();
		iniY = e.getY();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		camera.move(getWorldX(iniX)-getWorldX(e.getX()),getWorldY(iniY)-getWorldY(e.getY()));
		iniX = e.getX();
		iniY = e.getY();
		doubleNetworkWindow.cameraChange(camera);
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		doubleNetworkWindow.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.requestFocus();
		doubleNetworkWindow.setActivePanel(this);
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		doubleNetworkWindow.cameraChange(camera);
		repaint();
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'o':
			((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().selectOppositeLink();
			doubleNetworkWindow.refreshLabel(Labels.LINK);
			break;
		case 'v':
			viewAll();
			doubleNetworkWindow.cameraChange(camera);
			break;
		case 'n':
			getLayer(1).changeVisible();
			break;
		case 'm':
			((NetworkCapacitiesPainter)getLayer(1).getPainter()).changeMode();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_ENTER:
			doubleNetworkWindow.applyCapacity();
			break;
		}
		doubleNetworkWindow.setVisible(true);
		doubleNetworkWindow.repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
