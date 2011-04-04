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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.sergioo.PathEditor.gui.Window.Option;

public class PanelPathEditor extends JPanel implements MouseListener, MouseMotionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private Camera camera;
	private Window window;
	private Color backgroundColor = Color.WHITE;
	private Color pointsColor = Color.BLUE;
	private Color pointsColor2 = Color.GREEN;
	private Color linesColor = Color.GRAY;
	private Color selectedColor = Color.RED;
	private int pointsSize = 1;
	private Stroke pointsStroke = new BasicStroke(1);
	private Stroke linesStroke = new BasicStroke(1);
	private Stroke selectedStroke = new BasicStroke(2);
	private boolean wait;

	private int iniX;

	private int iniY;
	
	//Methods
	public PanelPathEditor(Window window) {
		this.window = window;
		this.setBackground(backgroundColor);
		camera = new Camera();
		calculateBoundaries();
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	private void calculateBoundaries() {
		double xMin=Double.POSITIVE_INFINITY, yMin=Double.POSITIVE_INFINITY, xMax=Double.NEGATIVE_INFINITY, yMax=Double.NEGATIVE_INFINITY;
		for(Coord point:window.getPoints()) {
			if(point.getX()<xMin)
				xMin = point.getX();
			if(point.getX()>xMax)
				xMax = point.getX();
			if(point.getY()<yMin)
				yMin = point.getY();
			if(point.getY()>yMax)
				yMax = point.getY();
		}
		camera.setBoundaries(xMin, yMin, xMax, yMax);
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		paintPoints(g2);
		paintLines(g2);
		paintSelected(g2);
	}
	private void paintPoints(Graphics2D g2) {
		g2.setColor(pointsColor);
		g2.setStroke(pointsStroke);
		for(Coord point:window.getPoints()) {
			g2.drawLine(camera.getIntX(point.getX())-pointsSize, camera.getIntY(point.getY())+pointsSize, camera.getIntX(point.getX())+pointsSize, camera.getIntY(point.getY())-pointsSize);
			g2.drawLine(camera.getIntX(point.getX())-pointsSize, camera.getIntY(point.getY())-pointsSize, camera.getIntX(point.getX())+pointsSize, camera.getIntY(point.getY())+pointsSize);
		}
		g2.setColor(pointsColor2);
		for(Coord point:window.getStopPoints()) {
			g2.drawLine(camera.getIntX(point.getX())-2*pointsSize, camera.getIntY(point.getY()), camera.getIntX(point.getX())+2*pointsSize, camera.getIntY(point.getY()));
			g2.drawLine(camera.getIntX(point.getX()), camera.getIntY(point.getY())-2*pointsSize, camera.getIntX(point.getX()), camera.getIntY(point.getY())+2*pointsSize);
		}
	}
	private void paintLines(Graphics2D g2) {
		g2.setColor(linesColor);
		g2.setStroke(linesStroke);
		for(Link link:window.getLinks())
			g2.drawLine(camera.getIntX(link.getFromNode().getCoord().getX()),
					camera.getIntY(link.getFromNode().getCoord().getY()),
					camera.getIntX(link.getToNode().getCoord().getX()),
					camera.getIntY(link.getToNode().getCoord().getY()));
	}
	private void paintSelected(Graphics2D g2) {
		g2.setColor(selectedColor);
		g2.setStroke(selectedStroke);
		Link link=window.getSelectedLink();
		if(link!=null) {
			g2.drawLine(camera.getIntX(link.getFromNode().getCoord().getX()),
					camera.getIntY(link.getFromNode().getCoord().getY()),
					camera.getIntX(link.getToNode().getCoord().getX()),
					camera.getIntY(link.getToNode().getCoord().getY()));
			Shape circle = new Ellipse2D.Double(camera.getIntX(link.getToNode().getCoord().getX())-pointsSize*3,camera.getIntY(link.getToNode().getCoord().getY())-pointsSize*3,pointsSize*6,pointsSize*6);
			g2.fill(circle);
		}
	}
	public void waitSecondCoord() {
		wait = true;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(wait) {
			window.add(new CoordImpl(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY())));
			wait=false;
		}
		else {
			if(window.getOption().equals(Option.SELECT) && e.getButton()==MouseEvent.BUTTON1)
				window.selectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Option.SELECT) && e.getButton()==MouseEvent.BUTTON3)
				window.unselectLink(camera.getDoubleX(e.getX()),camera.getDoubleY(e.getY()));
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON1)
				camera.zoomIn(e.getX(), e.getY());
			else if(window.getOption().equals(Option.ZOOM) && e.getButton()==MouseEvent.BUTTON3)
				camera.zoomOut(e.getX(), e.getY());
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
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		
	}
	
}
