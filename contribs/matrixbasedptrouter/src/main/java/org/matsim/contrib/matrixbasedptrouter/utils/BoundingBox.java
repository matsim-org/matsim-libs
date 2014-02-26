package org.matsim.contrib.matrixbasedptrouter.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class BoundingBox {
	
	// Logger
	private final Logger log = Logger.getLogger(BoundingBox.class);
	private double boundingBox [] = null;
	
	/**
	 * This sets a user defined bounding box
	 * 
	 * @param xmin
	 * @param ymin
	 * @param xmax
	 * @param ymax
	 */
	public void setCustomBoundaryBox(double xmin, double ymin, double xmax, double ymax){
		
		log.info("Setting custom bounding box ...");
		
		boundingBox = new double[4];
		boundingBox[0] = xmin;
		boundingBox[1] = ymin;
		boundingBox[2] = xmax;
		boundingBox[3] = ymax;
		
		log.info("...done!");
	}
	
	/**
	 * This determines and set the bounding box based on the network extend
	 * 
	 * @param network
	 */
	public void setDefaultBoundaryBox(Network network){
		
		if(boundingBox != null) {
			System.out.flush();
			log.warn("Bounding box is already initialized and will not be overwritten!");
			System.err.flush();
		} else {
			System.out.flush();
			log.warn("Setting bounding box from network! For large networks this may lead to memory issues depending on available memory and/or grid resolution. In this case define a custom bounding box.");
			System.err.flush();
			// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
			boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
			log.info("... done!");
		}
	}
	
	////////////////////////////////
	// getter methods
	////////////////////////////////
	
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
}
