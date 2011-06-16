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
package playground.droeder.data.graph;

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

	public StraightComparer(Straight one, Straight two, ResultsOfSegmentComparison results){
		this.one = one;
		this.two = two;
		this.computeBases();
		results.setAvDist(this.getAverageDistance());
		results.setMatchedLengthOne(this.getTotalMatchedLengthStraightOne());
		results.setMatchedLengthTwo(this.getTotalMatchedLengthStraightTwo());
		results.setAngle(this.getAngle());
	}
	
	public boolean straightOneIsUndershot(){
		if(one.getEnd().getX() < two.getEnd().getX()){
			return true;
		}
		return false;
	}
	
	private void computeBases() {
		this.baseA_CD = this.getBase(two, one.getStart());
		this.baseB_CD = this.getBase(two, one.getEnd());
		this.baseC_AB = this.getBase(one, two.getStart());
		this.baseD_AB = this.getBase(one, two.getEnd());
	}
	
	public double getAverageDistance(){
		return Math.min(getMinDist(one.getStart(), baseC_AB, two.getStart(), baseA_CD), getMinDist(one.getEnd(), baseD_AB, two.getEnd(), baseB_CD));
	}
	
	private Double getMinDist(Coord point1, Coord baseOn1, Coord point2, Coord baseOn2){
		if(baseOn1 == null && baseOn2 == null){
			return GeoCalculator.distanceBetween2Points(point1, point2);
		}else if(baseOn1 == null){
			return Math.min(GeoCalculator.distanceBetween2Points(point1, point2), GeoCalculator.distanceBetween2Points(point1, baseOn2));
		}else if(baseOn2 == null){
			return Math.min(GeoCalculator.distanceBetween2Points(point1, point2), GeoCalculator.distanceBetween2Points(point2, baseOn1));
		}else{
			return Double.MAX_VALUE;
		}
	}
	
	public Double getTotalMatchedLengthStraightOne(){
		if(this.baseA_CD == null && this.baseB_CD == null && this.baseC_AB == null && this.baseD_AB == null){
			return 0.0;
		}else if(this.baseC_AB == null && this.baseD_AB == null){
			return GeoCalculator.distanceBetween2Points(one.getStart(), one.getEnd());
		}else if(this.baseC_AB == null){
			return GeoCalculator.distanceBetween2Points(one.getStart(), this.baseD_AB);
		}else if (this.baseD_AB == null){
			return GeoCalculator.distanceBetween2Points(this.baseC_AB, one.getEnd());
		}else{
			return GeoCalculator.distanceBetween2Points(this.baseC_AB, this.baseD_AB);
		}
	}

	public Double getTotalMatchedLengthStraightTwo(){
		if(this.baseA_CD == null && this.baseB_CD == null && this.baseC_AB == null && this.baseD_AB == null){
			return 0.0;
		}else if(this.baseA_CD == null && this.baseB_CD== null){
			return GeoCalculator.distanceBetween2Points(two.getStart(), two.getEnd());
		}else if(this.baseA_CD == null){
			return GeoCalculator.distanceBetween2Points(two.getStart(), this.baseB_CD);
		}else if(this.baseB_CD == null){
			return GeoCalculator.distanceBetween2Points(this.baseA_CD, two.getEnd());
		}else{
			return GeoCalculator.distanceBetween2Points(baseA_CD, baseB_CD);
		}
		
	}
	/*
	 * computes the base of the perpendicular of the "point" on the straight s
	 */
	private Coord getBase(Straight s, Coord point){
		Vector2D a, b, c, r1, r2, p, p2;
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
		p2 = c.add(new Vector2D(answers[1], r2));
		if(!(p.equals(p2))){
			log.error(p.toString() + " " + p2.toString());
		}
		
		// return the point only, if it is beetween the point a && b
		if(a.getX() < p.getX() && p.getX() < b.getX()){
			return new CoordImpl(p.getX(), p.getY());
		}else{
			return null;
		}
	}
	
	public Double getAngle(){
		return GeoCalculator.angleBeetween2Straights(
				new Tuple<Coord, Coord>(this.one.getStart(), this.one.getEnd()), 
				new Tuple<Coord, Coord>(this.two.getStart(), this.two.getEnd()));
	}
}

class Straight{
	
	private Coord start;
	private Coord end;

	public Straight(Coord one, Coord two){
		if(one.getX() < two.getX()){
			this.start = one;
			this.end = two;
		}else{
			this.start = two;
			this.end = one;
		}
	}
	
	public Coord getStart(){
		return this.start;
	}
	
	public Coord getEnd(){
		return this.end;
	}
}


