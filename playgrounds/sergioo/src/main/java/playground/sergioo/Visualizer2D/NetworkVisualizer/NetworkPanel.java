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

package playground.sergioo.Visualizer2D.NetworkVisualizer;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkManager;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.SimpleSelectionNetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.SimpleNetworkWindow.Label;
import playground.sergioo.Visualizer2D.NetworkVisualizer.SimpleNetworkWindow.Option;

public class NetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final SimpleNetworkWindow window;
	private int iniX;
	private int iniY;
	private boolean withNetwork = true;
	
	//Methods
	public NetworkPanel(SimpleNetworkWindow window, NetworkPainter networkPainter) {
		super();
		this.window = window;
		addLayer(new Layer(networkPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkManager().getNetworkLinks()) {
			if(link!=null) {
				coords.add(link.getFromNode().getCoord());
				coords.add(link.getToNode().getCoord());
			}
		}
		super.calculateBoundaries(coords);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(window.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().selectLink(getWorldX(e.getX()),getWorldY(e.getY()));
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().unselectLink();
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().selectNode(getWorldX(e.getX()),getWorldY(e.getY()));
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager().unselectNode();
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
		}
		repaint();
	}
	public String getLabelText(Label label) {
		try {
			return (String) NetworkManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getActiveLayer().getPainter()).getNetworkManager(), new Object[0]);
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
	public void mousePressed(MouseEvent e) {
		this.requestFocus();
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
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		window.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'n':
			withNetwork  = !withNetwork;
			break;
		case 's':
			((SimpleSelectionNetworkPainter)getPrincipalLayer().getPainter()).changeSelected();
			break;
		case 'o':
			((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkManager().selectOppositeLink();
			window.refreshLabel(Label.LINK);
			break;
		case 'v':
			viewAll();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
