/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptTripAnalysis.utils;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;

/**
 * @author droeder
 *
 */
public class Vector2D {
	
	private double x;
	private double y;

	public Vector2D(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public Vector2D(double factor, Vector2D v){
		this.x = v.getX() * factor;
		this.y = v.getY() * factor;
	}
	
	public Vector2D(double factor, double x, double y){
		this.x = factor * x;
		this.y = factor * y;
	}
	
	public Vector2D orthogonal(){
		if((this.x == 0.0) && (this.y == 0.0)){
			return new Vector2D(Math.random(), Math.random());
		}else if(this.x == 0){
			return new Vector2D(1.0, 0.0);
		}else if(this.y == 0){
			return new Vector2D(0.0, 1.0);
		}else{
			return new Vector2D((1 / this.absolut()), this.y, -this.x);
		}
	}
	
	public Vector2D add(Vector2D v){
		return new Vector2D(this.x + v.getX(), this.y + v.getY());
	}
	
	public Vector2D subtract(Vector2D v){
		return new Vector2D(this.x - v.getX(), this.y - v.getY());
	}
	
	public double absolut(){
		return Math.sqrt(Math.pow(x,2) + Math.pow(y, 2));
	}
	
	public Vector2D getUnitVector(){
		return new Vector2D(1/this.absolut(), this);
	}
	/**
	 * @return
	 */
	public double getY() {
		return this.y;
	}

	/**
	 * @return
	 */
	public double getX() {
		return this.x;
	}
	
	public double scalarProduct(Vector2D v){
		return ((this.x * v.getX()) + (this.y * v.getY()));
	}
	
	public Vector2D addFactor(double f){
		return new Vector2D(this.x * f, this.y * f);
	}
	
	@Override
	public String toString(){
		return "{" + this.x + "; " + this.y + "}";
	}
	
	@Override
	 public boolean equals(final Object o){
		if(!(o instanceof Vector2D)) return false;
		Vector2D v = (Vector2D) o;
		double error = 0.0001, 
				x = Math.abs(1-(v.getX() / this.getX())),  
				y = Math.abs(1 - (v.getY() / this.getY()));
		if(x < error && y < error){
			return true;
		}else{
			return false;
		}
	}
	
	public Coord getCoord(){
		return new Coord(this.x, this.y);
	}
	
	public Coordinate getCoordinate(){
		return new Coordinate(this.x, this.y);
	}
}
