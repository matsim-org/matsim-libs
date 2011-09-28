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

import java.awt.Color;
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
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.sergioo.NetworksMatcher.gui.DoubleNetworkMatchingWindow.Labels;
import playground.sergioo.NetworksMatcher.gui.DoubleNetworkMatchingWindow.Options;
import playground.sergioo.NetworksMatcher.gui.MatchingsPainter.MatchingOptions;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;


public class NetworkNodesPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final DoubleNetworkMatchingWindow doubleNetworkWindow;
	private int iniX;
	private int iniY;
	private boolean matchingsAdded = false;
	
	//Methods
	public NetworkNodesPanel(DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkNodesPainter networkPainter) {
		super();
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(networkPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public NetworkNodesPanel(Collection<NodesMatching> nodesMatchings, MatchingOptions matchingOption, DoubleNetworkMatchingWindow doubleNetworkWindow, NetworkNodesPainter networkPainter, List<Color> colors) {
		super();
		this.doubleNetworkWindow = doubleNetworkWindow;
		addLayer(new Layer(networkPainter));
		addLayer(new Layer(new MatchingsPainter(nodesMatchings, matchingOption, colors), false));
		matchingsAdded = true;
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public void setMatchings(Collection<NodesMatching> nodesMatchings, MatchingOptions matchingOption, List<Color> colors) {
		if(!matchingsAdded) {
			addLayer(new Layer(new MatchingsPainter(nodesMatchings, matchingOption, colors), false));
			matchingsAdded = true;
		}
		else {
			removeLastLayer();
			addLayer(new Layer(new MatchingsPainter(nodesMatchings, matchingOption, colors), false));
		}
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Link link:((NetworkNodesPainter)getPrincipalLayer().getPainter()).getNetworkManager().getNetworkLinks()) {
			if(link!=null) {
				coords.add(link.getFromNode().getCoord());
				coords.add(link.getToNode().getCoord());
			}
		}
		super.calculateBoundaries(coords);
	}
	public String getLabelText(Labels label) {
		try {
			return (String) NetworkNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkNodesPainter)getPrincipalLayer().getPainter()).getNetworkManager(), new Object[0]);
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
				((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager().selectLink(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager().unselectLink();
				doubleNetworkWindow.refreshLabel(Labels.LINK);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager().selectNode(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager().unselectNode();
				doubleNetworkWindow.refreshLabel(Labels.NODE);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkNodesPainterManager)((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager()).selectNodeFromCollection(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODES);
			}
			else if(doubleNetworkWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkNodesPainterManager)((NetworkNodesPainter)getActiveLayer().getPainter()).getNetworkManager()).unselectNodeFromCollection(getWorldX(e.getX()),getWorldY(e.getY()));
				doubleNetworkWindow.refreshLabel(Labels.NODES);
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
		camera.move(getWorldX(e.getX()),getWorldX(iniX),getWorldY(e.getY()),getWorldY(iniY));
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
		case 's':
			((NetworkNodesPainter)getPrincipalLayer().getPainter()).changeVisibleSelectedElements();
			break;
		case 'o':
			((NetworkNodesPainter)getPrincipalLayer().getPainter()).getNetworkManager().selectOppositeLink();
			doubleNetworkWindow.refreshLabel(Labels.LINK);
			break;
		case 'n':
			getActiveLayer().changeVisible();
			break;
		case 'v':
			viewAll();
			doubleNetworkWindow.cameraChange(camera);
			break;
		case 'm':
			doubleNetworkWindow.setNetworksSeparated();
			break;
		case 'f':
			doubleNetworkWindow.finalNetworks();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_UP:
			doubleNetworkWindow.nextNetwork();
			break;
		case KeyEvent.VK_DOWN:
			doubleNetworkWindow.previousNetwork();
			break;
		}
		repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
