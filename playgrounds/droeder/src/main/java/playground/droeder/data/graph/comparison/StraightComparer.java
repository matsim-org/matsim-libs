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
import org.matsim.core.utils.geometry.CoordinateTransformation;

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
	private Double angle;
	private Double avDist;
	private Double matchedLengthOne;
	private Double matchedLengthTwo;
	private boolean match = false;
	private boolean oneIsUnderShot = false;;
	
	public StraightComparer(Straight one, Straight two){
		this.one = one;
		this.two = two;
		this.angle = GeoCalculator.angleBeetween2Straights(
				new Tuple<Coord, Coord>(this.one.getStart(), this.one.getEnd()), 
				new Tuple<Coord, Coord>(this.two.getStart(), this.two.getEnd()));
		this.setNoMatch();
		this.computeValues();
	}

	private void computeValues() {
		if(this.angle == 0.0){
			this.match = true;
			if(isVertical(this.one) && isVertical(this.two)){
				this.handleVertical();
			}else if(isHorizontal(this.one) && isHorizontal(this.two)){
				this.handleHorizontal();
			}else{
				this.handleSomeAngle();
			}
		}else if(this.angle < (Math.PI / 2)){
			//if the angle beetween two straights is bigger than Pi/2 they point into different directions and can't match
			this.match = true;
			this.handleSomeAngle();
		}else{
			this.setNoMatch();
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
		this.handleParallel(one.getStart().getX(), one.getEnd().getX(), two.getStart().getX(), two.getEnd().getX());
	}
	
	/**
	 * 
	 */
	private void handleVertical() {
		this.handleParallel(one.getStart().getY(), one.getEnd().getY(), two.getStart().getY(), two.getEnd().getY());
	}

	/**
	 * 
	 */
	private void handleParallel(Double oneOne, Double oneTwo, Double twoOne, Double twoTwo) {
		/*
		 * prepare for easier handling
		 * oneOne <= oneTwo and twoOne <= twoTwo
		 */
		double v11, v12, v21, v22;
		v11 = oneOne;
		v12 = oneTwo;
		if(v11 <= v12){
			v21 = twoOne;
			v22 = twoTwo;
			if((v21 > v22)){
				log.error("if the angle is smaller than Pi/2 this should not happen!");
			}
		}else{
			v11 = oneTwo;
			v12 = oneOne;
			v21 = twoTwo;
			v22 = twoOne;
			if((v21 > v22)){
				log.error("if the angle is smaller than Pi/2 this should not happen!");
			}
		}
		
		/* 	x11 --- 
		 *  x21  -- 
		 */ 
		if(v11 <= v21){
			if(v12 <= v22){
				/* 	x11 --  x12
				 *  x21  -- x22
				 */ 
				if(v12 >= v21){
					this.matchedLengthOne = (v12-v21);
					this.matchedLengthTwo = this.matchedLengthOne;
					this.oneIsUnderShot = true;
				}else{
					this.setNoMatch();
				}
			}
			/* 	x11 ---- x12
			 *  x21  --  x22
			 */ 
			else{
				this.matchedLengthOne = (v22-v21);
				this.matchedLengthTwo = this.matchedLengthOne;
			}
		}
		/* x11  ----
		 * x21 -----
		 */
		else{
			if(v12 <= v22){
				/* x11  ---- x12
				 * x21 ----- x22
				 */
				if(v12 >= v21){
					this.matchedLengthOne = (v12 - v11);
					this.matchedLengthTwo = this.matchedLengthOne;
					this.oneIsUnderShot = true;
				}else{
					this.setNoMatch();
				}
			}
			/* x11  ---- x12
			 * x21 ----  x22
			 */
			else{
				this.matchedLengthOne = (v22- v11);
				this.matchedLengthTwo = this.matchedLengthOne;
			}
		}
	}

	/**
	 * 
	 */
	private void handleSomeAngle() {
		/*
		 * change the coordinateSystem for easier handling
		 * rotate it to the right, until one is parallel to the x-axis
		 */
		Double angle2e1 = -GeoCalculator.angleStraight2e1(this.one);
		Straight one, two;
		one = new Straight(transform(angle2e1, this.one.getStart()), transform(angle2e1, this.one.getEnd()));
		two = new Straight(transform(angle2e1, this.two.getStart()), transform(angle2e1, this.two.getEnd()));

		if(one.getStart().getX() > one.getEnd().getX()){
			this.setNoMatch();
			log.error("this should not happen after coordinateTransformation");
		}else if(!partlyCongruent(one, two)){
			this.setNoMatch();
		}else{
			Coord baseAB_C = getBase(one, two.getStart());
			Coord baseAB_D = getBase(one, two.getEnd());
			Coord baseCD_A = getBase(two, one.getStart());
			Coord baseCD_B = getBase(two, one.getEnd());
			if(one.getEnd().getX() < two.getEnd().getX()){
				oneIsUnderShot = true;
			}
			if(basePartOfStraight(one, baseAB_C)){
				if(!(basePartOfStraight(one, baseAB_D) || basePartOfStraight(two, baseCD_B))){
					this.setNoMatch();
					return;
				}
			}
			if(basePartOfStraight(two, baseCD_A)){
				if(!(basePartOfStraight(one, baseAB_D) || basePartOfStraight(two, baseCD_B))){
					this.setNoMatch();
					return;
				}
			}
			
//			if(!(basePartOfStraight(one, baseAB_C) || basePartOfStraight(one, baseAB_D))){
//				this.setNoMatch();
//				return;
//			}
//			if(!(basePartOfStraight(two, baseCD_A) || basePartOfStraight(two, baseCD_B))){
//				this.setNoMatch();
//				return;
//			}
			
			Coord oneOne, oneTwo, twoOne, twoTwo;
			if(basePartOfStraight(one, baseAB_C)){
				oneOne = baseAB_C;
			}else{
				oneOne = one.getStart();
			}
			if(basePartOfStraight(one, baseAB_D)){
				oneTwo = baseAB_D;
			}else{
				oneTwo = one.getEnd();
			}
			if(basePartOfStraight(two, baseCD_A)){
				twoOne = baseCD_A;
			}else{
				twoOne = two.getStart();
			}
			if(basePartOfStraight(two, baseCD_B)){
				twoTwo = baseCD_B;
			}else{
				twoTwo = two.getEnd();
			}
			
			this.matchedLengthOne = GeoCalculator.distanceBetween2Points(oneOne, oneTwo);
			this.matchedLengthTwo = GeoCalculator.distanceBetween2Points(twoOne, twoTwo);
			this.avDist = (0.5 * getMin(GeoCalculator.distanceBetween2Points(one.getStart(), two.getStart()), 
								GeoCalculator.distanceBetween2Points(one.getStart(), baseCD_A), 
								GeoCalculator.distanceBetween2Points(two.getStart(), baseAB_C)))
								+
							(0.5 * getMin(GeoCalculator.distanceBetween2Points(one.getEnd(), two.getEnd()), 
									GeoCalculator.distanceBetween2Points(one.getEnd(), baseAB_D), 
									GeoCalculator.distanceBetween2Points(two.getEnd(), baseCD_B)));
		}
		//TODO debug
//		log.info("rotation " + angle2e1);
//		System.out.println(this.one);
//		System.out.println(one);
//		System.out.println(this.two);
//		System.out.println(two);
	}
	
	/**
	 * @param one2
	 * @param two2
	 * @return
	 */
	private boolean partlyCongruent(Straight one, Straight two) {
		if(one.getStart().getX() < two.getStart().getX() && two.getStart().getX() < one.getEnd().getX()){
			return true;
		}else if(one.getStart().getX() < two.getEnd().getX() && two.getEnd().getX() < one.getEnd().getX()){
			return true;
		}else if(two.getStart().getX() < one.getStart().getX() && one.getStart().getX() < two.getEnd().getX()){
			return true;
		}else if(two.getStart().getX() < one.getEnd().getX() && one.getEnd().getX() < two.getEnd().getX()){
			return true;
		}
		return false;
	}

	private Double getMin(Double one, Double two, Double three){
		Double temp = Math.min(one, two);
		return Math.min(temp, three);
	}
	
	private boolean basePartOfStraight(Straight s, Coord base){
		if((s.getStart().getX() <= base.getX()) && (s.getEnd().getX() >= base.getX())){
			if((s.getStart().getY() <= base.getY()) && (s.getEnd().getX() >= base.getY())){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	private Coord transform(Double angle, Coord point){
		Double x = (Math.cos(angle) * point.getX()) - (Math.sin(angle) * point.getY());
		Double y = (Math.sin(angle) * point.getX()) + (Math.cos(angle) * point.getY());
		Double rnd = 1 * Math.pow(10, 10);
		return new CoordImpl(Math.round(x * rnd)/rnd, Math.round(y*rnd)/rnd);
	}
	
	private void setNoMatch(){
		this.match = false;
		this.oneIsUnderShot = false;
		this.matchedLengthOne = Double.NaN;
		this.matchedLengthTwo = Double.NaN;
		this.avDist = Double.NaN;
	}
	
	public boolean possibleMatch(){
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
		Coord base = new CoordImpl(p.getX(), p.getY());
		
		//TODO debug
//		System.out.print(base.getX() + "\t" + base.getY() + "\t");
//		System.out.println(basePartOfStraight(s, base));
//		
		return base;
	}
	
	public String toString(){
		StringBuffer b = new StringBuffer();
		b.append("angle: " + this.angle + "\t");
		b.append("avDist: " + this.avDist + "\t");
		b.append("lengthOne: " + this.matchedLengthOne + "\t");
		b.append("lengthTwo: " + this.matchedLengthTwo + "\t");
		b.append("sOneIsUndershot: " + this.oneIsUnderShot + "\t");
		b.append("match: " + this.match);
		return b.toString();
	}
}

