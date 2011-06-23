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

import util.geometry.Point2D;

public class PanelNetwork extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private Camera camera;
	private Window window;
	
	private int iniX;
	private int iniY;
	private double pointsSize;
	private Color backgroundColor = Color.WHITE;
	private Color linkSelectedColor = Color.GREEN;
	private Color nodeSelectedColor = Color.MAGENTA;
	private Color linesColor = Color.BLACK;
	private Color pointsColor = Color.RED;
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke selectedStroke = new BasicStroke(2);
	private Stroke linesStroke = new BasicStroke(1);
	private Stroke pointsStroke = new BasicStroke(1);
	private Stroke networkStroke = new BasicStroke(0.5f);
	private boolean withSelected = true;
	private boolean withLines = true;
	private boolean withPoints = true;
	private boolean withNetwork = true;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	
	//Methods
	public PanelNetwork(Window window) {
		this.window = window;
		this.setBackground(backgroundColor);
		camera = new Camera();
		calculateBoundaries();
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setFocusable(true);
	}
	private void calculateBoundaries() {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		for(Link link:window.getNetworkLinks()) {
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
			paintNetwork(g2);
		if(withLines)
			paintLines(g2);
		if(withPoints)
			paintPoints(g2);
		if(withSelected)
			paintSelected(g2);
	}
	private void paintNetwork(Graphics2D g2) {
		g2.setColor(networkColor);
		g2.setStroke(networkStroke);
		pointsSize=0.5;
		for(Link link:window.getNetworkLinks(camera.getUpLeftCorner().getX(),camera.getUpLeftCorner().getY()+camera.getSize().getY(),camera.getUpLeftCorner().getX()+camera.getSize().getX(),camera.getUpLeftCorner().getY()))
			paintLink(g2,link);
	}
	private void paintSelected(Graphics2D g2) {
		g2.setStroke(selectedStroke);
		g2.setColor(linkSelectedColor);
		Link link=window.getSelectedLink();
		pointsSize=3;
		if(link!=null)
			paintLink(g2, link);
		g2.setColor(nodeSelectedColor);
		Node node = window.getSelectedNode();
		pointsSize = 5;
		if(node!=null)
			paintCircle(g2, node.getCoord());
		Tuple<Coord, Coord> line = window.getSelectedLine();
		pointsSize = 3;
		if(line!=null)
			paintLine(g2, line);
		Coord point = window.getSelectedPoint();
		pointsSize = 5;
		if(point!=null)
			paintCircle(g2, point);
	}
	private void paintLines(Graphics2D g2) {
		g2.setColor(linesColor);
		g2.setStroke(linesStroke);
		for(Tuple<Coord, Coord> line:window.getLines())
			paintLine(g2, line);
	}
	private void paintPoints(Graphics2D g2) {
		g2.setColor(pointsColor);
		g2.setStroke(pointsStroke);
		pointsSize = 3;
		for(Coord point:window.getPoints())
			paintCircle(g2, point);
	}
	private void paintLink(Graphics2D g2, Link link) {
		paintLine(g2, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()));
		paintCircle(g2,link.getToNode().getCoord());
	}
	private void paintLine(Graphics2D g2, Tuple<Coord,Coord> coords) {
		g2.drawLine(camera.getIntX(coords.getFirst().getX()),
				camera.getIntY(coords.getFirst().getY()),
				camera.getIntX(coords.getSecond().getX()),
				camera.getIntY(coords.getSecond().getY()));
	}
	private void paintCircle(Graphics2D g2, Coord coord) {
		Shape circle = new Ellipse2D.Double(camera.getIntX(coord.getX())-pointsSize,camera.getIntY(coord.getY())-pointsSize,pointsSize*2,pointsSize*2);
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
	@Override
	public void mouseClicked(MouseEvent e) {
		this.requestFocus();
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
		else {
			if(window.getOption().equals(Window.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1)
				window.selectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Window.Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3)
				window.unselectLink();
			else if(window.getOption().equals(Window.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1)
				window.selectNode(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Window.Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3)
				window.unselectNode();
			else if(window.getOption().equals(Window.Option.SELECT_POINT) && e.getButton()==MouseEvent.BUTTON1)
				window.selectPoint(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Window.Option.SELECT_POINT) && e.getButton()==MouseEvent.BUTTON3)
				window.unselectPoint();
			else if(window.getOption().equals(Window.Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Window.Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
		}
		repaint();
	}
	public void withStops() {
		withPoints = true;
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
			window.selectOppositeLink();
			break;
		case 'v':
			setBoundaries();
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
