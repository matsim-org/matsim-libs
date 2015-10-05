/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.southafrica.utilities.grid;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Smoothing function for both point and line features when given a 
 * {@link Geometry}.
 *   
 * @author jwjoubert
 */
public class KernelDensityEstimator {
	final private static Logger LOG = Logger.getLogger(KernelDensityEstimator.class);

	private final GeometryFactory gf = new GeometryFactory();
	private KdeType kdeType;
	private GeneralGrid grid;
	private double radius;
	private Map<Point, Double> weight;

	private final Counter pointCounter = new Counter("  points #");
	private final Counter lineCounter = new Counter("  lines #");

	public KernelDensityEstimator(GeneralGrid grid, KdeType type, double radius) {
		if(radius <= grid.getCellWidth() & type != KdeType.CELL){
			LOG.error("The radius is less than or equal to the cell width. This only works if the KdeType CELL is used.");
			throw new IllegalArgumentException("Increase the width, or use KdeType.CELL");
		}

		this.kdeType = type;
		this.grid = grid;
		this.radius = radius;

		this.weight = new HashMap<Point, Double>(grid.getGrid().size());

	}

	public double getWeight(Point p){
		if(!this.weight.containsKey(p)){
			return 0.0;
//			throw new IllegalArgumentException("The point is not in the weight map: " + p.toString());
		}
		return this.weight.get(p);
	}


	public void processPoint(Point p, double weight){
		/* For a line, we use different search radii for different KDE-types. */
		double usedRadius;
		switch (this.kdeType) {
		case CELL:
			/* The larger of the given radius, or half the cell width. This is 
			 * to ensure that if a point is on the shared boundary of two 
			 * geometries, they share the weight evenly, even though the given
			 * radius might have been 0.0. */
			usedRadius = Math.max(this.radius, this.grid.getCellWidth()/2.0);
			break;
		case GAUSSIAN:
			/* Three times the given radius is used. */
			usedRadius = 3.0*this.radius;
			break;
		default:
			usedRadius = this.radius;
			break;
		}
		
		/* Find all (possible) cells around the point. */
		double sum = 0.0;
		Collection<Point> neighbours = this.grid.getGrid().getDisk(p.getX(), p.getY(), usedRadius);
		Map<Point, Double> interimMap = new HashMap<Point, Double>(neighbours.size());

		/* Determine the interim weights. */
		Iterator<Point>	iterator = neighbours.iterator();
		while(iterator.hasNext()){
			Point centroid = iterator.next();
			Geometry cell = this.grid.getCellGeometry(centroid);

			switch (this.kdeType) {
			case CELL:
				/* Only consider the cell in which the point occurs. */
				if(cell.covers(p)){
					double w = weight;
					interimMap.put(centroid, w);
					sum += w;
				}
				break;
				
			case EPANECHNIKOV:
			case GAUSSIAN:
			case TRIANGULAR:
			case TRIWEIGHT:
			case UNIFORM:
				/* Calculate the weight for a cell proportional to the distance 
				 * from the cell's centroid to the point. */
				double distance = centroid.distance(p);
				double w = getFunctionFromDistance(distance)*weight;
				interimMap.put(centroid, w);
				sum += w;
				break;
			default:
				break;
			}
		}

		/* Normalise the weights. */
		for(Point cell : interimMap.keySet()){
			if(!this.weight.containsKey(cell)){
				this.weight.put(cell, interimMap.get(cell)/sum*weight);
			} else{
				this.weight.put(cell, this.weight.get(cell) + interimMap.get(cell)/sum*weight);
			}
		}
		pointCounter.incCounter();
	}


	/**
	 * Currently (September 2014, JWJ) this class processes the area along a 
	 * given line segment by incrementally identifying points along the line, 
	 * and finding all the cells within the given radius from the search point.
	 * 
	 * @param l
	 * @param weight
	 */
	public void processLine(LineString l, double weight){
		/* For a line, we use different search radii for different KDE-types. */
		double usedRadius;
		switch (this.kdeType) {
		case CELL:
			/* The larger of the given radius, or half the cell width. This is 
			 * to ensure that if a line is on the shared boundary of two 
			 * geometries, they share the weight evenly, even though the given
			 * radius might have been 0.0. */
			usedRadius = Math.max(this.radius, this.grid.getCellWidth()/2.0);
			break;
		case GAUSSIAN:
			/* Three times the given radius is used. */
			usedRadius = 3.0*this.radius;
			break;
		default:
			usedRadius = this.radius;
			break;
		}

		/* Find all (possible) cells along the line segment. */
		double sum = 0.0;
		Coordinate c0 = l.getCoordinateN(0);
		Coordinate c1 = l.getCoordinateN(1);
		Collection<Point> neighbours = this.grid.getGrid().getElliptical(c0.x, c0.y, c1.x, c1.y, 2*usedRadius+l.getLength());
		Map<Point, Double> interimMap = new HashMap<Point, Double>(neighbours.size());

		/* Determine the interim weights. */
		Iterator<Point> iterator = neighbours.iterator();
		while(iterator.hasNext()){
			Point centroid = iterator.next();
			Geometry cell = this.grid.getCellGeometry(centroid);

			switch (this.kdeType) {
			case CELL:
				/* Only consider the intersection of the line with the cells. */
				Geometry intersection = cell.intersection(l);
				if(!intersection.isEmpty()){
					double w = intersection.getLength() / l.getLength();
					interimMap.put(centroid, w);
					sum += w;
				}
				break;
				
			case EPANECHNIKOV:
			case GAUSSIAN:
			case TRIANGULAR:
			case TRIWEIGHT:
			case UNIFORM:
				/* Calculate the weight for a cell proportional to the distance from
				 * the cell's centroid to the line segment.
				 * elliptical search radius was used, now only consider those
				 * points within the given radius. */
				double distance = centroid.distance(l);
				
				if(distance <= this.radius){
					double w = getFunctionFromDistance(distance)*weight;
					interimMap.put(centroid, w);
					sum += w;
				}	
				break;
			default:
				break;
			}
		}

		/* Normalise the weights. */
		for(Point p : interimMap.keySet()){
			if(!this.weight.containsKey(p)){
				this.weight.put(p, interimMap.get(p)/sum*weight);
			} else{
				this.weight.put(p, this.weight.get(p) + interimMap.get(p)/sum*weight);
			}
		}
		lineCounter.incCounter();
	}

	private double getFunctionFromDistance(double distance){
		double w = 0.0;
		switch (this.kdeType) {
		case CELL:
			w = 1.0;
			break;
		case EPANECHNIKOV:
			w = 3.0/4.0*(1 - Math.pow(distance/radius, 2.0)) / radius;
			break;
		case GAUSSIAN:
			w = (1 / Math.sqrt(2*Math.PI))*Math.exp(-0.5*Math.pow(distance/radius, 2.0)) / radius;
			break;
		case TRIANGULAR:
			w = (radius - distance) / radius;
			break;
		case TRIWEIGHT:
			w = 35.0/32.0*Math.pow(1 - Math.pow(distance/radius, 2.0), 3.0) / radius;
			break;
		case UNIFORM:
			w = (1.0 / (2.0 * radius));
			break;
		default:
		}
		return w;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

	/**
	 * Different implementations of Kernel Density Estimation (KDE).
	 * 
	 * @see <a href=http://en.wikipedia.org/wiki/Kernel_(statistics)>Kernel functions</a>.
	 *
	 * @author jwjoubert
	 */
	public static enum KdeType{
		CELL,
		EPANECHNIKOV,
		GAUSSIAN,
		TRIANGULAR,
		TRIWEIGHT,
		UNIFORM,
		;
	}

	public KdeType getKdeType(){
		return this.kdeType;
	}

	public double getRadius(){
		return this.radius;
	}

	public GeneralGrid getGrid(){
		return this.grid;
	}


	private class ProcessPointCallable implements Callable<Map<Point, Double>>{

		@Override
		public Map<Point, Double> call() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}


	private class ProcessLineCallable implements Callable<Map<Point, Double>>{

		@Override
		public Map<Point, Double> call() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
