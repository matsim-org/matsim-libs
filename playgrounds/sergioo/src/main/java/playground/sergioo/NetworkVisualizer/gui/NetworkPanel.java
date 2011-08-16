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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.NetworkVisualizer.gui.NetworkWindow.Label;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkPainter;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.publicTransport.PublicTransportNetworkPainter;

import util.geometry.Point2D;

public class NetworkPanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	
	
	private final Camera camera;
	private final NetworkWindow window;
	private final NetworkManager networkManager;
	private final NetworkPainter networkPainter;
	
	private int iniX;
	private int iniY;
	private Color backgroundColor = Color.WHITE;
	private Color linkSelectedColor = Color.GREEN;
	private Color nodeSelectedColor = Color.MAGENTA;
	private Color linesColor = Color.BLACK;
	private Color pointsColor = Color.RED;
	private Stroke selectedStroke = new BasicStroke(2);
	private Stroke linesStroke = new BasicStroke(1);
	private boolean withSelected = true;
	private boolean withLines = true;
	private boolean withPoints = true;
	private boolean withNetwork = true;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	
	//Methods
	public NetworkPanel(NetworkWindow window, NetworkManager networkManager, NetworkPainter networkPainter) {
		this.window = window;
		this.networkManager = networkManager;
		this.networkPainter = networkPainter;
		this.setBackground(backgroundColor);
		camera = new Camera();
		calculateBoundaries();
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	public Camera getCamera() {
		return camera;
	}
	public NetworkManager getNetworkManager() {
		return networkManager;
	}
	private void calculateBoundaries() {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		for(Link link:networkManager.getNetworkLinks()) {
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
		if(withNetwork)
			try {
				networkPainter.paintNetwork(g2, camera);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if(withLines)
			paintLines(g2);
		if(withPoints)
			paintPoints(g2);
		if(withSelected)
			paintSelected(g2);
	}
	private void paintSelected(Graphics2D g2) {
		Link link=networkManager.getSelectedLink();
		if(link!=null)
			paintLink(g2, link, selectedStroke, 3, linkSelectedColor);
		Node node = networkManager.getSelectedNode();
		if(node!=null)
			paintCircle(g2, node.getCoord(), 5, nodeSelectedColor);
		Tuple<Coord, Coord> line = networkManager.getSelectedLine();
		if(line!=null)
			paintLine(g2, line, selectedStroke, linesColor);
		Coord point = networkManager.getSelectedPoint();
		if(point!=null)
			paintCircle(g2, point, 5, pointsColor);
	}
	private void paintLines(Graphics2D g2) {
		for(Tuple<Coord, Coord> line:networkManager.getLines())
			paintLine(g2, line, linesStroke, linesColor);
	}
	private void paintPoints(Graphics2D g2) {
		for(Coord point:networkManager.getPoints())
			paintCircle(g2, point, 3, pointsColor);
	}
	private void paintLink(Graphics2D g2, Link link, Stroke stroke, double pointSize, Color color) {
		paintLine(g2, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()), stroke, color);
		paintCircle(g2,link.getToNode().getCoord(), pointSize, color);
	}
	private void paintLine(Graphics2D g2, Tuple<Coord,Coord> coords, Stroke stroke, Color color) {
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawLine(camera.getIntX(coords.getFirst().getX()),
				camera.getIntY(coords.getFirst().getY()),
				camera.getIntX(coords.getSecond().getX()),
				camera.getIntY(coords.getSecond().getY()));
	}
	private void paintCircle(Graphics2D g2, Coord coord, double pointSize, Color color) {
		Shape circle = new Ellipse2D.Double(camera.getIntX(coord.getX())-pointSize,camera.getIntY(coord.getY())-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
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
	public void withStops() {
		withPoints = true;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
		else {
			if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1) {
				networkManager.selectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3) {
				networkManager.unselectLink();
				window.refreshLabel(Label.LINK);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1) {
				networkManager.selectNode(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3) {
				networkManager.unselectNode();
				window.refreshLabel(Label.NODE);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_POINT) && e.getButton()==MouseEvent.BUTTON1) {
				networkManager.selectPoint(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.POINT);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_POINT) && e.getButton()==MouseEvent.BUTTON3) {
				networkManager.unselectPoint();
				window.refreshLabel(Label.POINT);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINE) && e.getButton()==MouseEvent.BUTTON1) {
				networkManager.selectLine(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				window.refreshLabel(Label.LINE);
			}
			else if(window.getOption().equals(SimpleNetworkWindow.Option.SELECT_LINE) && e.getButton()==MouseEvent.BUTTON3) {
				networkManager.unselectLine();
				window.refreshLabel(Label.LINE);
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
			withSelected  = !withSelected;
			break;
		case 'l':
			withLines = !withLines;
			break;
		case 'p':
			withPoints = !withPoints;
			break;
		case 'o':
			networkManager.selectOppositeLink();
			window.refreshLabel(Label.LINK);
			break;
		case 'v':
			setBoundaries();
			break;
		case 't':
			((PublicTransportNetworkPainter)networkPainter).setWeight(((PublicTransportNetworkPainter)networkPainter).getWeight()/1.5f);
			break;
		case 'g':
			((PublicTransportNetworkPainter)networkPainter).setWeight(((PublicTransportNetworkPainter)networkPainter).getWeight()*1.5f);
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
