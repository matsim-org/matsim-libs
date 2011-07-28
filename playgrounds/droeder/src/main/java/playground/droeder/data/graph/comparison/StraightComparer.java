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
package playground.droeder.data.graph.comparison;

import org.apache.commons.math.linear.BlockRealMatrix;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.SingularValueDecompositionImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.droeder.GeoCalculator;
import playground.droeder.Vector2D;

/**
 * @author droeder
 *
 */
public class StraightComparer{
	
	private static final Logger log = Logger.getLogger(StraightComparer.class);
	
	private Straight one;
	private Straight two;
	private Coord baseA_CD, baseB_CD, baseC_AB, baseD_AB;
	private double angle;
	private double avDist;
	private double matchedLengthOne;
	private double matchedLengthTwo;
	private boolean match = false;
	private boolean oneIsUnderShot;
	
	public StraightComparer(Straight one, Straight two){
		this.one = one;
		this.two = two;
		this.angle = GeoCalculator.angleBeetween2Straights(
				new Tuple<Coord, Coord>(this.one.getStart(), this.one.getEnd()), 
				new Tuple<Coord, Coord>(this.two.getStart(), this.two.getEnd()));
		this.computeValues();
	}

	private void computeValues() {
		if(this.angle == 0.0){
			if(isVertical(this.one) && isVertical(this.two)){
				this.handleVertical();
			}else if(isHorizontal(this.one) && isHorizontal(this.two)){
				this.handleHorizontal();
			}else{
				this.handleParallel();
			}
		}else if(this.angle < (Math.PI / 2)){
			//if the angle beetween two straights is bigger than Pi/2 they point into different directions and can't match
			this.handleSomeAngle();
		}
	}

	/**
	 * @param one2
	 * @return
	 */
	private boolean isHorizontal(Straight s) {
		if(s.getStart().getY() == s.getEnd().getY()){
			return true;
		}
		return false;
	}

	
	/**
	 * @param one2
	 * @return
	 */
	private boolean isVertical(Straight s) {
		if(s.getStart().getX() == s.getEnd().getX()){
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 */
	private void handleHorizontal() {
		//prepare for easier handling
		double x11, x12, x21, x22;
		x11 = one.getStart().getX();
		x12 = one.getEnd().getX();
		if(x11 <= x12){
			x21 = two.getStart().getX();
			x22 = two.getEnd().getX();
			if(!(x21 > x22)){
				log.error("if the angle is smaller than Pi/2 this should not happen!");
			}
		}else{
			x11 = one.getEnd().getX();
			x12 = one.getStart().getX();
			x21 = two.getEnd().getX();
			x22 = two.getStart().getX();
			if(!(x21 > x22)){
				log.error("if the angle is smaller than Pi/2 this should not happen!");
			}
		}
		
		/* 	x11 --- x12
		 *  x21  -- x22
		 */ 
		if(x11 < x21){
			if(x12 < x22){
				/* 	x11 --  x12
				 *  x21  -- x22
				 */ 
				if(x12 > x21){
					
				}else{
					this.setNoMatch();
				}
			}
			/* 	x11 ---- x12
			 *  x21  --  x22
			 */ 
			else{
				
			}
		}else{
			
		}
	}
	
	/**
	 * 
	 */
	private void handleVertical() {
		double y11, y12, y21, y22;
		
		
		y11 = one.getStart().getY();
		y12 = one.getEnd().getY();
		y21 = two.getStart().getY();
		y22 = two.getEnd().getY();
	}

	/**
	 * 
	 */
	private void handleParallel() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 */
	private void handleSomeAngle() {
		// TODO Auto-generated method stub
		
	}
	
	private void setNoMatch(){
		this.match = false;
		this.matchedLengthOne = 0;
		this.matchedLengthTwo = 0;
		this.avDist = 0;
	}
	
	public boolean matched(){
		return this.match;
	}
	
	public boolean straightOneIsUndershot(){
		return this.oneIsUnderShot;
	}
	
	public Double getAngle(){
		return this.angle;
	}

	public double getAverageDistance(){
		return this.avDist;
	}
	
	public Double getMatchedLengthOne(){
		return this.matchedLengthOne;
	}

	public Double getMatchedLengthTwo(){
		return this.matchedLengthTwo;
	}
	
	
	
	/*
	 * computes the base of the perpendicular of the "point" on the straight s
	 */
	private Coord getBase(Straight s, Coord point){
		//handle vertical straights
		if(s.getStart().getX() == s.getEnd().getX()){
			if(s.getStart().getY() < s.getEnd().getY()){
				if(s.getStart().getY() < point.getY() && s.getEnd().getY() > point.getY()){
					return new CoordImpl(s.getStart().getX(), point.getY());
				}else{
					return null;
				}
			}else if(s.getStart().getY() > s.getEnd().getY()){
				if(s.getStart().getY() > point.getY() && s.getEnd().getY() < point.getY()){
					return new CoordImpl(s.getStart().getX(), point.getY());
				}else{
					return null;
				}
			}else{
				return new CoordImpl(s.getStart().getX(), point.getY());
			}
		}
		Vector2D a, b, c, r1, r2, p;
		a = new Vector2D(s.getStart().getX(), s.getStart().getY());
		b = new Vector2D(s.getEnd().getX(), s.getEnd().getY());
		c = new Vector2D(point.getX(), point.getY());
		
		//get direction vector of the straight and a direction vector of the straight through the point, orthogonal to the given straight
		r1 = a.subtract(b);
		r2 = r1.orthogonal();
		
		// prepare the set of linear equations to solve
		double[][] coeff = {{r1.getX(), -r2.getX()},
				{r1.getY(), -r2.getY()}};
		double values[] = {c.getX() - a.getX(), c.getY() - a.getY()};
		
		RealMatrix m = new BlockRealMatrix(coeff);
		SingularValueDecompositionImpl decomp = new SingularValueDecompositionImpl(m);
		DecompositionSolver solver = decomp.getSolver();
		double[] answers = solver.solve(values);
		
		// get the base of the perpendicular from c to the straight ab
		p = a.add(new Vector2D(answers[0], r1));
		
		/*
		 * return the point only, if it is beetween the point a && b, because otherwise it is not interesting
		 * for the distance-calculation
		 */
		if(a.getX() < b.getX()){
			if(a.getX() < p.getX() && p.getX() < b.getX()){
				return new CoordImpl(p.getX(), p.getY());
			}else{
				return null;
			}	
		}else if(b.getX() < a.getX()){
			if(a.getX() > p.getX() && p.getX() > b.getX()){
				return new CoordImpl(p.getX(), p.getY());
			}else{
				return null;
			}
		}
		return null;
	}
}


