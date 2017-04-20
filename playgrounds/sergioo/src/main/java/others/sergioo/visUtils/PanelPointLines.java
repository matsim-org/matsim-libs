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

package others.sergioo.visUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import others.sergioo.util.geometry.Line2D;
import others.sergioo.util.geometry.Point2D;
import others.sergioo.util.geometry.Vector2D;


public class PanelPointLines extends JPanel implements Observer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int FRAMESIZE = 10;
	
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
	private JFrame window;
	
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
	public PanelPointLines(PointLines pointsLines, JFrame window) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		calculateBoundaries(window);
		this.window = window;
		((Observable) pointsLines).addObserver(this);
	}
	public PanelPointLines(PointLines pointsLines, String title, JFrame window) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		this.title = title;
		calculateBoundaries(window);
		this.window = window;
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
			if(line.getThickness()!=-1)
				g2.setStroke(new BasicStroke((float)line.getThickness()));
			else
				g2.setStroke(linesStroke);
			g2.drawLine(getIntX(line.getPI().getX()), getIntY(line.getPI().getY()), getIntX(line.getPF().getX()), getIntY(line.getPF().getY()));
		}
		/*for(Line2D line:((SimpleMapMatcher)pointsLines).getLines2())
			g2.drawLine(getIntX(line.getPI().getX()), getIntY(line.getPI().getY()), getIntX(line.getPF().getX()), getIntY(line.getPF().getY()));*/
	}
	public int getIntX(double x) {
		return (int) ((x-upLeftCorner.getX())*(window.getWidth()-2*FRAMESIZE)/this.size.getX())+FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((upLeftCorner.getY()-y)*(window.getHeight()-2*FRAMESIZE)/this.size.getY())+FRAMESIZE;
	}
	private void calculateBoundaries(JFrame window) {
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
		Dimension scSize = Toolkit.getDefaultToolkit().getScreenSize();		
		int width=(int) (size.getX()/size.getY()>(double)scSize.getWidth()/(double)scSize.getHeight()?scSize.getWidth():scSize.getHeight()*size.getX()/size.getY());
		int height=(int) (size.getY()/size.getX()>(double)scSize.getHeight()/(double)scSize.getWidth()?scSize.getHeight():scSize.getWidth()*size.getY()/size.getX());
		window.setSize(width, height);
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
	}
	@Override
	public void update(Observable o, Object arg) {
		//calculateBoundaries();
		paintComponent(this.getGraphics());
	}
	
}
