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

/**
 * @author Sergio Ordóñez
 */
public class Point2D {
	
	//Attributes
	private double y;
	private double x;
	
	//Methods
	public Point2D() {
		super();
		x = 0;
		y = 0;
	}
	public Point2D(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	public Point2D(Vector2D v) {
		super();
		x = v.getX();
		y = v.getY();
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public double getR() {
		return Math.hypot(x, y);
	}
	public double getTheta() {
		return Math.atan2(y, x);
	}
	public void translate(double dx, double dy) {
		this.x+=dx;
		this.y+=dy;
	}
	public void translate(Vector2D v) {
		this.x+=v.getX();
		this.y+=v.getY();
	}
	public Point2D getTranslated(double dx, double dy) {
		return new Point2D(x+dx, y+dy);
	}
	public Point2D getTranslated(Vector2D v) {
		return new Point2D(x+v.getX(), y+v.getY());
	}
	public double getDistance() {
		return Math.hypot(x, y);
	}
	public double getDistanceSqr() {
		return Math.pow(x,2)+Math.pow(y,2);
	}
	public double getDistance(Point2D p2) {
		return Math.hypot(x-p2.getX(),y-p2.getY());
	}
	public double getDistanceSqr(Point2D p2) {
		return Math.pow(x-p2.getX(),2)+Math.pow(y-p2.getY(),2);
	}
	public Point2D clone() {
		return new Point2D(x, y);
	}
	public String toString() {
		return x+","+y;
	}
	public static Point2D parsePoint2D(String point2D) {
		String[] parts=point2D.split(",");
		return new Point2D(Double.parseDouble(parts[0]),Double.parseDouble(parts[1]));
	}
	
}
