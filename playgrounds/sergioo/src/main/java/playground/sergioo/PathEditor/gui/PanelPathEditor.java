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
import java.awt.Stroke;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JPanel;

import util.geometry.Line2D;
import util.geometry.Point2D;
import util.geometry.Vector2D;

public class PanelPathEditor extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private Point2D upLeftCorner;
	private Vector2D sizeDownRight;
	private Color backgroundColor = Color.WHITE;
	private Color pointsColor = Color.RED;
	private Color linesColor = Color.GRAY;
	private int pointsSize = 1;
	private Stroke pointsStroke = new BasicStroke(1);
	private Stroke linesStroke = new BasicStroke(1);
	
	//Methods
	public PanelPathEditor() {
		this.setBackground(backgroundColor);
		calculateBoundaries();
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		paintLines(g2);
		paintPoints(g2);
	}
	private void paintPoints(Graphics2D g2) {
		g2.setColor(pointsColor);
		g2.setStroke(pointsStroke);
		
	}
	private void paintLines(Graphics2D g2) {
		g2.setColor(linesColor);
		g2.setStroke(linesStroke);

	}
	public int getIntX(double x) {
		return (int) ((x-upLeftCorner.getX())*(Window.WIDTH-2*Window.FRAMESIZE)/sizeDownRight.getX())+Window.FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((upLeftCorner.getY()-y)*(Window.HEIGHT-2*Window.FRAMESIZE)/sizeDownRight.getY())+Window.FRAMESIZE;
	}
	private void calculateBoundaries() {
		double xMin=Double.POSITIVE_INFINITY, yMin=Double.POSITIVE_INFINITY, xMax=Double.NEGATIVE_INFINITY, yMax=Double.NEGATIVE_INFINITY;
		upLeftCorner = new Point2D(xMin, yMax);
		sizeDownRight = new Vector2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
	}
	
}
