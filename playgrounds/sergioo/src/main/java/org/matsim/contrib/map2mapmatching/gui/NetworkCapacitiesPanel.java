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

package org.matsim.contrib.map2mapmatching.gui;

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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkCapacitiesWindow.Labels;
import org.matsim.contrib.map2mapmatching.gui.DoubleNetworkCapacitiesWindow.Options;
import org.matsim.contrib.map2mapmatching.gui.MatchingsPainter.MatchingOptions;
import org.matsim.contrib.map2mapmatching.gui.core.Layer;
import org.matsim.contrib.map2mapmatching.gui.core.LayersPanel;
import org.matsim.contrib.map2mapmatching.gui.core.LayersWindow;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainter;
import org.matsim.contrib.map2mapmatching.gui.core.network.painter.NetworkPainterManager;

public class NetworkCapacitiesPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final DoubleNetworkCapacitiesWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	private boolean activePath;
	private boolean controlKeyPressed;
	
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
		((NetworkTwoNodesPainter)getLayer(0).getPainter()).setNetwork(network);
	}
	public Node getSelectedNode() {
		return ((NetworkPainterManager)((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager()).getSelectedNode();
	}
	
	public Link getSelectedLink() {
		return ((NetworkPainterManager)((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager()).getSelectedLink();
	}
	public Link getOppositeToSelectedLink() {
		return ((NetworkPainterManager)((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager()).getOppositeToSelectedLink();
	}
	public void selectLink(String id) {
		Link link = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectLink(id);
		if(link!=null)
			doubleNetworkWindow.centerCamera(new double[]{link.getCoord().getX(), link.getCoord().getY()});
	}
	public void selectNode(String id) {
		Node node = ((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager().selectNode(id);
		if(node!=null)
			doubleNetworkWindow.centerCamera(new double[]{node.getCoord().getX(), node.getCoord().getY()});
	}
	public void setPathActive() {
		activePath = true;
	}
	private void calculateBoundaries() {
		Collection<double[]> coords = new ArrayList<double[]>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager().getNetworkLinks()) {
			if(link!=null) {
				coords.add(new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()});
				coords.add(new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()});
			}
		}
		super.calculateBoundaries(coords);
	}
	public String getLabelText(LayersWindow.Labels label) {
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
		double[] p = getWorld(e.getX(), e.getY());
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(p);
		else {
			if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().selectLink(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().unselectLink();
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().selectNode(p[0], p[1]);
				doubleNetworkWindow.refreshLabel(Labels.NODE);
				if(activePath) {
					doubleNetworkWindow.applyCapacityPath2();
					activePath = false;
				}
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getLayer(1).getPainter()).getNetworkPainterManager().unselectNode();
				doubleNetworkWindow.refreshLabel(Labels.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(p[0], p[1]);
				doubleNetworkWindow.cameraChange(camera);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(p[0], p[1]);
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
		camera.move(iniX-e.getX(), iniY-e.getY());
		iniX = e.getX();
		iniY = e.getY();
		doubleNetworkWindow.cameraChange(camera);
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		double[] p = getWorld(e.getX(), e.getY());
		doubleNetworkWindow.setCoords(p[0], p[1]);
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
		case KeyEvent.VK_SPACE:
			doubleNetworkWindow.applyCapacityOpp();
			break;
		case KeyEvent.VK_ALT:
			doubleNetworkWindow.applyCapacityPath();
			break;
		case KeyEvent.VK_BACK_SPACE:
			doubleNetworkWindow.deleteCapacity();
			break;
		case KeyEvent.VK_DELETE:
			doubleNetworkWindow.deleteCapacityOpp();
			break;
		case KeyEvent.VK_CONTROL:
			controlKeyPressed = true;
			break;
		case KeyEvent.VK_Z:
			if(controlKeyPressed)
				doubleNetworkWindow.undo();
			break;
		}
		doubleNetworkWindow.setVisible(true);
		doubleNetworkWindow.repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_CONTROL:
			controlKeyPressed = false;
			break;
		}
	}
	
	
}
