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

package util.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergio Ordóñez
 */
public class Polygon2D {
	
	//Attributes
	/**
	 * The points of the two-dimensional Polygon
	 */
	private List<Point2D> points;
	
	//Methods
	/**
     * Creates a new empty polygon
     */
    public Polygon2D () {
    	points = new ArrayList<Point2D>();
    }
	/**
     * Creates a new polygon according to a list of points 
     * @param points the list of points. points.size()>=3
     */
    public Polygon2D (List<Point2D> points) {
    	this.points = points;
    }
    /**
     * Creates a new polygon according to a list of coordinates
     * <b>pre: </b> xs.length=ys.length
     * @param xs x coordinates. xs.length>=3
     * @param ys y coordinates. ys.length>=3
     */
    public Polygon2D (double[] xs, double[] ys) {
    	points = new ArrayList<Point2D>();
    	for(int i=0; i<xs.length; i++)
    		points.add(new Point2D(xs[i], ys[i]));
    }
    /**
     * Add a point to the end of the polygon  
     * @param point The two-dimensional new point. point!=null
     */
    public void addPoint(Point2D point) {
    	points.add(point);
    }
    /**  
     * @param pos The position of the point. pos>=0 && pos<points.size()
     * @return the required point
     */
    public Point2D getPoint(int pos) {
    	return points.get(pos);
    }
    /**
     * @return the number of points of the polygon
     */
    public int getNumPoints() {
		return points.size();
	}
	/**
	 * Determines if a point is inside the polygon
	 * @param point The two-dimensional point. point!=null
	 * @return boolean
	 */
    public boolean contains(Point2D point) {
    	boolean inside=false;
    	for(int i=0, j=points.size()-1; i<points.size(); j=i++)
    		if((points.get(i).getY()>point.getY() != points.get(j).getY()>point.getY()) && (point.getX() < (points.get(j).getX()-points.get(i).getX())*(point.getY()-points.get(i).getY())/(points.get(j).getY()-points.get(i).getY())+points.get(i).getX()))
    			inside=!inside;
    	return inside;
    }
    /**
	 * Determines if a polygon intersects the polygon
	 * @param polygon The two-dimensional polygon. polygon!=null
	 * @return boolean
	 */
    public boolean intersects(Polygon2D polygon) {
    	for(Point2D p:polygon.points)
    		if(contains(p))
    			return true;
    	for(Point2D p:points)
    		if(polygon.contains(p))
    			return true;
    	return false;
    }
    /**
     * @return 1 or -1 according to the polygon normal
     */
    public double getNormal() {
    	int numP=0;
    	for(int i=0; i<points.size()-2; i++)
    		if(new Vector2D(points.get(i),points.get(i+1)).crossProduct(new Vector2D(points.get(i+1),points.get(i+2)))>0)
    			numP++;
    	if(new Vector2D(points.get(points.size()-2),points.get(points.size()-1)).crossProduct(new Vector2D(points.get(points.size()-1),points.get(0)))>0)
			numP++;
    	if(new Vector2D(points.get(points.size()-1),points.get(0)).crossProduct(new Vector2D(points.get(0),points.get(1)))>0)
			numP++;
    	return numP>points.size()/2?1:-1;
    }
	/**
     * Determines a polygon enlarged a distance d.
     * @param d The distance for being enlarged. d>0
     * @return Polygon
     */
	public Polygon2D getEnlargedPolygon(double d) {
		//TODO
		List<Point2D> nPoints=new ArrayList<Point2D>();
		int p=points.size()-1;
		for(int a=0; a<points.size(); a++) {
			int n=a+1;
			if(n==points.size())
				n=0;
			Point2D prev=points.get(p);
			Point2D actu=points.get(a);
			Point2D next=points.get(n);
			Vector2D v1=new Vector2D(prev, actu).getRotated(-getNormal()*Math.PI/2).getUnit().getScaled(d);
			Vector2D v2=new Vector2D(actu, next).getRotated(-getNormal()*Math.PI/2).getUnit().getScaled(d);
			Line2D l1=new Line2D(prev.getTranslated(v1),actu.getTranslated(v1));
			Line2D l2=new Line2D(actu.getTranslated(v2),next.getTranslated(v2));
			nPoints.add(l1.intersect(l2));
			p=a;
		}
		return new Polygon2D(nPoints);
	}
	/**
     * Determines a polygon enlarged a distance d.
     * <b>pre: </b> the polygon is convex
     * @param d The distance for being enlarged. d>0
     * @return Polygon
     */
	public Polygon2D getEnlargedPolygon2(double d) {
		List<Point2D> nPoints=new ArrayList<Point2D>();
		int p=points.size()-1;
		for(int a=0; a<points.size(); a++) {
			int n=a+1;
			if(n==points.size())
				n=0;
			Point2D prev=points.get(p);
			Point2D actu=points.get(a);
			Point2D next=points.get(n);
			double alfa=Math.atan2(actu.getY()-prev.getY(),actu.getX()-prev.getX());
			double beta=Math.acos((prev.getDistanceSqr(actu)+actu.getDistanceSqr(next)-prev.getDistanceSqr(next))/(2*prev.getDistance(actu)*actu.getDistance(next)));
			nPoints.add(new Point2D(actu.getX()+d*Math.cos(alfa-(beta/2))/Math.sin(beta/2),actu.getY()+d*Math.sin(alfa-(beta/2))/Math.sin(beta/2)));
			p=a;
		}
		return new Polygon2D(nPoints);
	}
	/**
	 * @param perp
	 * @return a list of the points result from the intersection between the polygon and the given line
	 */
	public List<Point2D> getLineIntersection(Line2D perp) {
		List<Point2D> res=new ArrayList<Point2D>();
		for(int i=0; i<points.size()-1; i++) {
			Line2D side=new Line2D(points.get(i), points.get(i+1));
			if(side.isIntersected(perp))
				res.add(side.intersect(perp));
		}
		return res;
	}
	/**
	 * Transform the polygon information in a text
	 * @return The text
	 */
	public String toString() {
		String ps="";
		for(Point2D p:points)
			ps+=p+";";
		return ps;
	}

}
