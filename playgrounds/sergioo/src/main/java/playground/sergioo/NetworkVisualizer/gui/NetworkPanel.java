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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;

import org.matsim.api.core.v01.network.Link;

import playground.sergioo.NetworkVisualizer.gui.NetworkWindow.Label;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkManager;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkPainter;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.SimpleNetworkPainter;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.publicTransport.PublicTransportNetworkPainter;

import util.geometry.Point2D;

public class NetworkPanel extends LayersPanel implements MouseListener, MouseMotionListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private final NetworkWindow window;
	private int iniX;
	private int iniY;
	private Color backgroundColor = Color.WHITE;
	private boolean withNetwork = true;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	
	//Methods
	public NetworkPanel(NetworkWindow window, NetworkPainter networkPainter) {
		super();
		this.window = window;
		layers.add(new Layer(networkPainter));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public Camera getCamera() {
		return camera;
	}
	private void calculateBoundaries() {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		for(Link link:((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().getNetworkLinks()) {
			if(link!=null) {
				if(link.getFromNode().getCoord().getX()<xMin)
					xMin = link.getFromNode().getCoord().getX();
				if(link.getFromNode().getCoord().getX()>xMax)
					xMax = link.getFromNode().getCoord().getX();
				if(link.getFromNode().getCoord().getY()<yMin)
					yMin = link.getFromNode().getCoord().getY();
				if(link.getFromNode().getCoord().getY()>yMax)
					yMax = link.getFromNode().getCoord().getY();
				if(link.getToNode().getCoord().getX()<xMin)
					xMin = link.getToNode().getCoord().getX();
				if(link.getToNode().getCoord().getX()>xMax)
					xMax = link.getToNode().getCoord().getX();
				if(link.getToNode().getCoord().getY()<yMin)
					yMin = link.getToNode().getCoord().getY();
				if(link.getToNode().getCoord().getY()>yMax)
					yMax = link.getToNode().getCoord().getY();
			}
		}
		setBoundaries();
	}
	private void setBoundaries() {
		camera.setBoundaries(xMin, yMin, xMax, yMax);
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		for(Layer layer:layers)
			try {
				layer.paint(g2, camera);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public void zoomIn(double x, double y) {
		camera.zoomIn(x, y);
	}
	public void zoomOut(double x, double y) {
		camera.zoomOut(x, y);
	}
	public void centerCamera(double x, double y) {
		camera.centerCamera(x, y);
	}
	public Point2D getCenter() {
		return camera.getCenter();
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
		else {
			if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().selectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().unselectLink();
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().selectNode(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().unselectNode();
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
				window.cameraChange(camera);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
				window.cameraChange(camera);
			}
		}
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
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
		camera.move(e.getX(),iniX,e.getY(),iniY);
		iniX = e.getX();
		iniY = e.getY();
		repaint();
		window.cameraChange(camera);
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		window.setCoords(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'n':
			withNetwork  = !withNetwork;
			break;
		case 's':
			((SimpleNetworkPainter)layers.get(0).getPainter()).changeSelected();
			break;
		case 'o':
			((NetworkPainter)layers.get(0).getPainter()).getNetworkManager().selectOppositeLink();
			window.refreshLabel(Label.LINK);
			break;
		case 'v':
			setBoundaries();
			break;
		case 't':
			((PublicTransportNetworkPainter)layers.get(0).getPainter()).setWeight(((PublicTransportNetworkPainter)layers.get(0).getPainter()).getWeight()/1.5f);
			break;
		case 'g':
			((PublicTransportNetworkPainter)layers.get(0).getPainter()).setWeight(((PublicTransportNetworkPainter)layers.get(0).getPainter()).getWeight()*1.5f);
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
	public String getLabelText(Label label) {
		try {
			return (String) NetworkManager.class.getMethod("refresh"+label.text, new Class[0]).invoke(((NetworkPainter)layers.get(0).getPainter()).getNetworkManager(), new Object[0]);
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
	
}
