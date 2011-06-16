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
package playground.droeder;

/**
 * @author droeder
 *
 */
public class Vector2D {
	
	private Double x;
	private Double y;

	public Vector2D(Double x, Double y){
		this.x = x;
		this.y = y;
	}
	
	public Vector2D(Double factor, Vector2D v){
		this.x = v.getX() * factor;
		this.y = v.getY() * factor;
	}
	
	public Vector2D orthogonal(){
		if((this.x == 0.0) && (this.y == 0.0)){
			return new Vector2D(Math.random(), Math.random());
		}else if(this.x == 0){
			return new Vector2D(1.0, 0.0);
		}else if(this.y == 0){
			return new Vector2D(0.0, 1.0);
		}else{
			double y = Math.random();
			return new Vector2D(this.y*y/this.x, y);
		}
	}
	
	public Vector2D add(Vector2D v){
		return new Vector2D(this.x + v.getX(), this.y + v.getY());
	}
	
	public Vector2D subtract(Vector2D v){
		return new Vector2D(this.x - v.getX(), this.y - v.getY());
	}

	/**
	 * @return
	 */
	public Double getY() {
		return this.y;
	}

	/**
	 * @return
	 */
	public Double getX() {
		return this.x;
	}
	
	public Double scalarProduct(Vector2D v){
		return ((this.x * v.getX()) + (this.y * v.getY()));
	}
	
	public void addFactor(Double f){
		this.x = this.x * f;
		this.y = this.y * f;
	}
	
	@Override
	public String toString(){
		return "{" + this.x + "; " + this.y + "}";
	}
	
	@Override
	 public boolean equals(final Object o){
		if(!(o instanceof Vector2D)) return false;
		Vector2D v = (Vector2D) o;
		
		if(v.getX() == this.getX() && v.getY() == this.getY()){
			return true;
		}else{
			return false;
		}
	}
}
