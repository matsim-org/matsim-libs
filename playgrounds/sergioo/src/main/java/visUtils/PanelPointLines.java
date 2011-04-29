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

package visUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JPanel;

import util.geometry.Line2D;
import util.geometry.Point2D;
import util.geometry.Vector2D;

public class PanelPointLines extends JPanel implements Observer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private Point2D upLeftCorner;
	private Vector2D size;
	private Color backgroundColor = Color.WHITE;
	private Color pointsColor = Color.BLACK;
	private Color linesColor = Color.GRAY;
	private int pointsSize = 5;
	private Stroke pointsStroke = new BasicStroke(3);
	private Stroke linesStroke = new BasicStroke(1);
	private PointLines pointsLines;
	private String title;
	//Methods
	public PanelPointLines(PointLines pointsLines) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		calculateBoundaries();
		((Observable) pointsLines).addObserver(this);
	}
	public PanelPointLines(PointLines pointsLines, String title) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		this.title = title;
		calculateBoundaries();
		((Observable) pointsLines).addObserver(this);
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2=(Graphics2D)g;
		paintLines(g2);
		paintPoints(g2);
		if(title!=null) {
			g2.setFont(new Font("Arial",Font.ITALIC,20));
			g2.setColor(new Color(0,0,0));
			g2.drawString(title, 400, 50);
		}
	}
	private void paintPoints(Graphics2D g2) {
		g2.setColor(pointsColor);
		g2.setStroke(pointsStroke);
		for(Point2D point:pointsLines.getPoints()) {
			g2.drawLine(getIntX(point.getX())-pointsSize, getIntY(point.getY())+pointsSize, getIntX(point.getX())+pointsSize, getIntY(point.getY())-pointsSize);
			g2.drawLine(getIntX(point.getX())-pointsSize, getIntY(point.getY())-pointsSize, getIntX(point.getX())+pointsSize, getIntY(point.getY())+pointsSize);
		}
	}
	private void paintLines(Graphics2D g2) {
		g2.setColor(linesColor);
		g2.setStroke(linesStroke);
		for(Line2D line:pointsLines.getLines()) {
			if(line.getThickness()!=-1) {
				g2.setColor(new Color(1,1-(float)line.getThickness()/10,0));
				g2.setStroke(new BasicStroke((float)line.getThickness()));
			}
			g2.drawLine(getIntX(line.getPI().getX()), getIntY(line.getPI().getY()), getIntX(line.getPF().getX()), getIntY(line.getPF().getY()));
		}
		/*for(Line2D line:((SimpleMapMatcher)pointsLines).getLines2())
			g2.drawLine(getIntX(line.getPI().getX()), getIntY(line.getPI().getY()), getIntX(line.getPF().getX()), getIntY(line.getPF().getY()));*/
	}
	public int getIntX(double x) {
		return (int) ((x-upLeftCorner.getX())*(Window.width-2*Window.FRAMESIZE)/this.size.getX())+Window.FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((upLeftCorner.getY()-y)*(Window.height-2*Window.FRAMESIZE)/this.size.getY())+Window.FRAMESIZE;
	}
	private void calculateBoundaries() {
		double xMin=Double.POSITIVE_INFINITY, yMin=Double.POSITIVE_INFINITY, xMax=Double.NEGATIVE_INFINITY, yMax=Double.NEGATIVE_INFINITY;
		for(Point2D point:pointsLines.getPoints()) {
			if(point.getX()<xMin)
				xMin = point.getX();
			if(point.getX()>xMax)
				xMax = point.getX();
			if(point.getY()<yMin)
				yMin = point.getY();
			if(point.getY()>yMax)
				yMax = point.getY();
		}
		for(Line2D line:pointsLines.getLines()) {
			Point2D point = line.getPI();
			if(point.getX()<xMin)
				xMin = point.getX();
			if(point.getX()>xMax)
				xMax = point.getX();
			if(point.getY()<yMin)
				yMin = point.getY();
			if(point.getY()>yMax)
				yMax = point.getY();
			point = line.getPF();
			if(point.getX()<xMin)
				xMin = point.getX();
			if(point.getX()>xMax)
				xMax = point.getX();
			if(point.getY()<yMin)
				yMin = point.getY();
			if(point.getY()>yMax)
				yMax = point.getY();
		}
		upLeftCorner = new Point2D(xMin, yMax);
		size = new Vector2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
		Window.width=(int) (size.getX()/size.getY()>(double)Window.MAX_WIDTH/(double)Window.MAX_HEIGHT?Window.MAX_WIDTH:Window.MAX_HEIGHT*size.getX()/size.getY());
		Window.height=(int) (size.getY()/size.getX()>(double)Window.MAX_HEIGHT/(double)Window.MAX_WIDTH?Window.MAX_HEIGHT:Window.MAX_WIDTH*size.getY()/size.getX());
	}
	@Override
	public void update(Observable o, Object arg) {
		//calculateBoundaries();
		paintComponent(this.getGraphics());
	}
	
}
