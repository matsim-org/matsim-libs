package org.matsim.contrib.matrixbasedptrouter.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import com.vividsolutions.jts.geom.Envelope;

public final class BoundingBox {
	
	private double boundingBox [] = null;
	
	/**
	 * This sets a user defined bounding box
	 * 
	 * @param xmin
	 * @param ymin
	 * @param xmax
	 * @param ymax
	 * @return TODO
	 */
	public static BoundingBox createBoundingBox(double xmin, double ymin, double xmax, double ymax){
		// no real reasons why these are static factory methods instead of constructors; was just easier to refactor. kai, mar'14
		
		return new BoundingBox( xmin, ymin, xmax, ymax ) ;
	}
	
	private BoundingBox( double xmin, double ymin, double xmax, double ymax ) {
		// no real reasons why these are static factory methods instead of constructors; was just easier to refactor. kai, mar'14
		
		boundingBox = new double[4];
		boundingBox[0] = xmin;
		boundingBox[1] = ymin;
		boundingBox[2] = xmax;
		boundingBox[3] = ymax;
		
	}
	
	/**
	 * This determines and set the bounding box based on the network extend
	 * 
	 * @param network
	 * @return TODO
	 */
	public static BoundingBox createBoundingBox(Network network){
		// no real reasons why these are static factory methods instead of constructors; was just easier to refactor. kai, mar'14
		return new BoundingBox( NetworkUtils.getBoundingBox( network.getNodes().values() ) ) ;
	}
	
	private BoundingBox( double[] bounds ) {
		// no real reasons why these are static factory methods instead of constructors; was just easier to refactor. kai, mar'14
		this.boundingBox = bounds ;
	}
	
	////////////////////////////////
	// getter methods
	////////////////////////////////
	
	public BoundingBox(Envelope env) {
		boundingBox = new double[4];
		boundingBox[0] = env.getMinX() ;
		boundingBox[1] = env.getMinY() ;
		boundingBox[2] = env.getMaxX() ;
		boundingBox[3] = env.getMaxY() ;
	}

	public double[] getBoundingBox(){
		return this.boundingBox;
	}
	public double getXMin(){
		return boundingBox[0];
	}
	public double getXMax(){
		return boundingBox[2];
	}
	public double getYMin(){
		return boundingBox[1];
	}
	public double getYMax(){
		return boundingBox[3];
	}
	
	@Override
	public String toString() {
		return "[xmin=" + this.getXMin() + " ymin=" + this.getYMin() + " xmax=" + this.getXMax() + " ymax= " + this.getYMax() + "]" ;
	}
}
