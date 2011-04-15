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
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.PathEditor.gui.Window.Option;
import util.geometry.Point2D;

public class PanelPathEditor extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private Camera camera;
	private Window window;
	private Color backgroundColor = Color.WHITE;
	private Color pointsColor = Color.BLUE;
	private Color pointsColor2 = Color.BLACK;
	private Color linesColor = Color.GRAY;
	private Color linesColor2 = Color.ORANGE;
	private Color selectedColor = Color.RED;
	private Color nodeSelectedColor = Color.MAGENTA;
	private Color networkColor = Color.LIGHT_GRAY;
	private int pointsSize = 1;
	private Stroke pointsStroke = new BasicStroke(1);
	private Stroke linesStroke = new BasicStroke(1);
	private Stroke selectedStroke = new BasicStroke(2);
	private Stroke networkStroke = new BasicStroke(0.5f);
	private boolean wait;
	private int iniX;
	private int iniY;
	private boolean withStops = false;
	private boolean withNetwork = false;
	private double xMax;
	private double yMax;
	private double xMin;
	private double yMin;
	private boolean paintAll = false;
	//Methods
	public PanelPathEditor(Window window) {
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
		Collection<Coord> points = window.getPoints();
		if(points.size()==0)
			points = window.getStopPoints();
		for(Coord point:points) {
			if(point.getX()<xMin)
				xMin = point.getX();
			if(point.getX()>xMax)
				xMax = point.getX();
			if(point.getY()<yMin)
				yMin = point.getY();
			if(point.getY()>yMax)
				yMax = point.getY();
		}
		setBoundaries();
	}
	private void calculateBoundariesAll() {
		xMin=Double.POSITIVE_INFINITY; yMin=Double.POSITIVE_INFINITY; xMax=Double.NEGATIVE_INFINITY; yMax=Double.NEGATIVE_INFINITY;
		Collection<List<Link>> links = window.getAllLinks();
		for(List<Link> linkR:links)
			for(Link link:linkR) {
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
		setBoundaries();
	}
	private void setBoundaries() {
		camera.setBoundaries(xMin, yMin, xMax, yMax);
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		if(paintAll)
			paintAllT(g2);
		else {
			if(withNetwork)
				paintNetwork(g2);
			paintPoints(g2);
			paintLines(g2);
			paintSelected(g2);
		}
	}
	private void paintAllT(Graphics2D g2) {
		if(withNetwork)
			paintNetwork(g2);
		paintAllLines(g2);
	}
	private void paintPoints(Graphics2D g2) {
		g2.setColor(pointsColor);
		g2.setStroke(pointsStroke);
		for(Coord point:window.getPoints()) {
			g2.drawLine(camera.getIntX(point.getX())-pointsSize, camera.getIntY(point.getY())+pointsSize, camera.getIntX(point.getX())+pointsSize, camera.getIntY(point.getY())-pointsSize);
			g2.drawLine(camera.getIntX(point.getX())-pointsSize, camera.getIntY(point.getY())-pointsSize, camera.getIntX(point.getX())+pointsSize, camera.getIntY(point.getY())+pointsSize);
		}
		if(withStops) {
			g2.setColor(pointsColor2);
			for(Coord point:window.getStopPoints()) {
				g2.drawLine(camera.getIntX(point.getX())-2*pointsSize, camera.getIntY(point.getY()), camera.getIntX(point.getX())+2*pointsSize, camera.getIntY(point.getY()));
				g2.drawLine(camera.getIntX(point.getX()), camera.getIntY(point.getY())-2*pointsSize, camera.getIntX(point.getX()), camera.getIntY(point.getY())+2*pointsSize);
			}
		}
	}
	private void paintNetwork(Graphics2D g2) {
		g2.setColor(networkColor);
		g2.setStroke(networkStroke);
		for(Link link:window.getNetworkLinks(camera.getUpLeftCorner().getX(),camera.getUpLeftCorner().getY()+camera.getSize().getY(),camera.getUpLeftCorner().getX()+camera.getSize().getX(),camera.getUpLeftCorner().getY()))
			paintLink(link,g2);
	}
	private void paintLines(Graphics2D g2) {
		g2.setColor(linesColor);
		g2.setStroke(linesStroke);
		for(Link link:window.getLinks())
			paintLink(link, g2);
		if(withStops) {
			g2.setColor(linesColor2);
			g2.setStroke(selectedStroke);
			for(Link link:window.getStopLinks())
				paintLink(link, g2);
		}
	}
	private void paintAllLines(Graphics2D g2) {
		g2.setStroke(networkStroke);
		for(List<Link> linksR:window.getAllLinks()) {
			g2.setColor(new Color((float)Math.random()*3/4, (float)Math.random()*3/4, (float)Math.random()*3/4));
			for(Link link:linksR)
				paintLink(link, g2);
		}
		if(withStops) {
			g2.setColor(linesColor2);
			g2.setStroke(selectedStroke);
			for(Link link:window.getAllStopLinks())
				paintLink(link, g2);
		}
	}
	private void paintSelected(Graphics2D g2) {
		g2.setColor(selectedColor);
		g2.setStroke(selectedStroke);
		Link link=window.getSelectedLink();
		if(link!=null) {
			paintLink(link, g2);
			Shape circle = new Ellipse2D.Double(camera.getIntX(link.getToNode().getCoord().getX())-pointsSize*3,camera.getIntY(link.getToNode().getCoord().getY())-pointsSize*3,pointsSize*6,pointsSize*6);
			g2.fill(circle);
		}
		if(withStops) {		
			Coord stop=window.getSelectedStop();
			if(stop!=null) {
				g2.drawLine(camera.getIntX(stop.getX())-2*pointsSize, camera.getIntY(stop.getY()), camera.getIntX(stop.getX())+2*pointsSize, camera.getIntY(stop.getY()));
				g2.drawLine(camera.getIntX(stop.getX()), camera.getIntY(stop.getY())-2*pointsSize, camera.getIntX(stop.getX()), camera.getIntY(stop.getY())+2*pointsSize);
			}
		}
		Node node = window.getSelectedNode();
		if(node!=null) {
			g2.setColor(nodeSelectedColor);
			Shape circle = new Ellipse2D.Double(camera.getIntX(node.getCoord().getX())-pointsSize*4,camera.getIntY(node.getCoord().getY())-pointsSize*4,pointsSize*8,pointsSize*8);
			g2.fill(circle);
		}
	}
	private void paintLink(Link link, Graphics2D g2) {
		g2.drawLine(camera.getIntX(link.getFromNode().getCoord().getX()),
				camera.getIntY(link.getFromNode().getCoord().getY()),
				camera.getIntX(link.getToNode().getCoord().getX()),
				camera.getIntY(link.getToNode().getCoord().getY()));
	}
	public void waitSecondCoord() {
		wait = true;
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
		if(e.getClickCount()==2) {
			camera.centerCamera(camera.getDoubleX(e.getX()), camera.getDoubleY(e.getY()));
		}
		else {
			if(wait) {
				window.add(new CoordImpl(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY())));
				wait=false;
			}
			else {
				if(window.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON1)
					window.selectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				else if(window.getOption().equals(Option.SELECT_LINK) && e.getButton()==MouseEvent.BUTTON3)
					window.unselectLink();
				else if(window.getOption().equals(Option.SELECT_STOP) && e.getButton()==MouseEvent.BUTTON1)
					window.selectStop(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				else if(window.getOption().equals(Option.SELECT_STOP) && e.getButton()==MouseEvent.BUTTON3)
					window.unselectStop();
				else if(window.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON1)
					window.selectNode(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
				else if(window.getOption().equals(Option.SELECT_NODE) && e.getButton()==MouseEvent.BUTTON3)
					window.unselectNode();
				else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
					camera.zoomIn(e.getX(), e.getY());
				else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
					camera.zoomOut(e.getX(), e.getY());
			}
		}
		repaint();
	}
	public void withStops() {
		withStops = true;
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
		
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case '+':
			window.increaseSelectedLink();
			break;
		case '-':
			window.decreaseSelectedLink();
			break;
		case 's':
			withStops = !withStops;
			break;
		case 'n':
			withNetwork  = !withNetwork;
			break;
		case 'v':
			setBoundaries();
			break;
		case 'u':
			window.changeUs();
			break;
		case 'r':
			window.changeReps();
			break;
		case 'i':
			window.changeInStops();
			break;
		case 'a':
			paintAll = !paintAll;
			calculateBoundariesAll();
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
