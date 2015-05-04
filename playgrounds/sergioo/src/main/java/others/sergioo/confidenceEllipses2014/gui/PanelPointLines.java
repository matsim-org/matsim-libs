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

package others.sergioo.confidenceEllipses2014.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

import others.sergioo.confidenceEllipses2014.kernel.ConfidenceEllipsesCalculator.EllipseData;
import others.sergioo.util.geometry.Line2D;
import others.sergioo.util.geometry.Point2D;
import others.sergioo.util.geometry.Vector2D;
import others.sergioo.visUtils.PointLines;


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
	private Color[] ellipseColor = new Color[]{Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.CYAN};
	private int pointsSize = 5;
	private Stroke pointsStroke = new BasicStroke(3);
	private Stroke linesStroke = new BasicStroke(1);
	private Stroke ellipseStroke = new BasicStroke(2);
	private PointLines pointsLines;
	private String title;
	private EllipseData[] ellipsesData;

	//Methods
	public PanelPointLines(PointLines pointsLines, EllipseData[] ellipsesData) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		this.ellipsesData = ellipsesData;
		calculateBoundaries();
		((Observable) pointsLines).addObserver(this);
	}
	public PanelPointLines(PointLines pointsLines, EllipseData[] ellipsesData, String title) {
		this.setBackground(backgroundColor);
		this.pointsLines = pointsLines;
		this.ellipsesData = ellipsesData;
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
			g2.drawString(title, this.getWidth()/2-title.length()*20/4, 50);
		}
		paintEllipses(g2);
	}
	private void paintEllipses(Graphics2D g2) {
		g2.setStroke(ellipseStroke);
		int n=0;
		for(EllipseData ellipseData:ellipsesData) {
			g2.setColor(ellipseColor[n++]);
			Ellipse2D e = new Ellipse2D.Double(0, 0, getIntX(ellipseData.getMayorSemiAxis()*2)-getIntX(0), getIntY(0)-getIntY(ellipseData.getMinorSemiAxis()*2));
			AffineTransform at = AffineTransform.getTranslateInstance(getIntX(ellipseData.getCenterX()), getIntY(ellipseData.getCenterY()));
			at.rotate(-ellipseData.getAngle());
			at.translate(-getIntX(ellipseData.getMayorSemiAxis())+getIntX(0), -getIntY(0)+getIntY(ellipseData.getMinorSemiAxis()));
			g2.draw(at.createTransformedShape(e));
	        g2.drawLine(getIntX(ellipseData.getCenterX())-pointsSize, getIntY(ellipseData.getCenterY())+pointsSize, getIntX(ellipseData.getCenterX())+pointsSize, getIntY(ellipseData.getCenterY())-pointsSize);
			g2.drawLine(getIntX(ellipseData.getCenterX())-pointsSize, getIntY(ellipseData.getCenterY())-pointsSize, getIntX(ellipseData.getCenterX())+pointsSize, getIntY(ellipseData.getCenterY())+pointsSize);
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
		return (int) ((x-upLeftCorner.getX())*(WindowEllipse.width-2*WindowEllipse.FRAMESIZE)/this.size.getX())+WindowEllipse.FRAMESIZE;
	}
	public int getIntY(double y) {
		return (int) ((upLeftCorner.getY()-y)*(WindowEllipse.height-2*WindowEllipse.FRAMESIZE)/this.size.getY())+WindowEllipse.FRAMESIZE;
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
		double zoom = 0.5;
		xMin-=(xMax-xMin)*zoom*2;
		xMax+=(xMax-xMin)*zoom/2;
		yMin-=(yMax-yMin)*zoom;
		yMax+=(yMax-yMin)*zoom/2;
		upLeftCorner = new Point2D(xMin, yMax);
		size = new Vector2D(new Point2D(xMin, yMin), new Point2D(xMax, yMax));
		WindowEllipse.width=(int) (size.getX()/size.getY()>(double)WindowEllipse.MAX_WIDTH/(double)WindowEllipse.MAX_HEIGHT?WindowEllipse.MAX_WIDTH:WindowEllipse.MAX_HEIGHT*size.getX()/size.getY());
		WindowEllipse.height=(int) (size.getY()/size.getX()>(double)WindowEllipse.MAX_HEIGHT/(double)WindowEllipse.MAX_WIDTH?WindowEllipse.MAX_HEIGHT:WindowEllipse.MAX_WIDTH*size.getY()/size.getX());
	}
	@Override
	public void update(Observable o, Object arg) {
		//calculateBoundaries();
		paintComponent(this.getGraphics());
	}
	
}
