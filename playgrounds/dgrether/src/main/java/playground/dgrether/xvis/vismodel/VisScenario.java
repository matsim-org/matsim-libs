/* *********************************************************************** *
 * project: org.matsim.*
 * NewVisNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.vismodel;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.contrib.signals.otfvis.VisSignalSystem;


/**
 * Container for visualization data. 
 * @author dgrether
 *
 */
public class VisScenario {
	
	private static final Logger log = Logger.getLogger(VisScenario.class);
	
	private float offsetX;
	private float offsetY;
	private Float minXY;
	private Float maxXY;
	private CoordinateTransformation visTransform = null;

	private Map<String, VisLinkWLanes> lanesLinkData =  new HashMap<String, VisLinkWLanes>();
	
	private Map<String, VisSignalSystem> signalSystems = new HashMap<String, VisSignalSystem>();
	
	
	public VisScenario(Network net){
		this.setBoundingBoxFromNetwork(net);
	}

	public Map<String, VisLinkWLanes> getLanesLinkData(){
		return this.lanesLinkData;
	}
	
	public Map<String, VisSignalSystem> getVisSignalSystemsByIdMap(){
		return this.signalSystems;
	}


	private void setBoundingBoxFromNetwork(Network n){
		double minX = java.lang.Double.POSITIVE_INFINITY;
		double maxX = java.lang.Double.NEGATIVE_INFINITY;
		double minY = java.lang.Double.POSITIVE_INFINITY;
		double maxY = java.lang.Double.NEGATIVE_INFINITY;

		for (org.matsim.api.core.v01.network.Node node : n.getNodes().values()) {
			minX = Math.min(minX, node.getCoord().getX());
			maxX = Math.max(maxX, node.getCoord().getX());
			minY = Math.min(minY, node.getCoord().getY());
			maxY = Math.max(maxY, node.getCoord().getY());
		}
		
		log.debug("Bounding box Matsim Coords: ");
		log.debug("minX: " + minX + " minY: " + minY);
		log.debug("maxX: "+ maxX + " maxY: "+maxY);
		double deltaY = maxY - minY;
		offsetX = (float) ( - minX + 100.0);
		offsetY = (float) ( - minY - deltaY - 100.0);
		log.debug("Offset X: " + offsetX + " Y: " + offsetY);
		Point2D.Double min = this.calcVisPosition(new Coord(minX, minY));
		Point2D.Double max = this.calcVisPosition(new Coord(maxX, maxY));
		this.minXY = new Point2D.Float((float)min.x, (float)max.y);
		this.maxXY = new Point2D.Float((float)max.x, (float)min.y); 
		log.debug("Bounding box View Coords: ");
		log.debug("minX: " + this.minXY.x + " minY: " + this.minXY.y);
		log.debug("maxX: "+ this.maxXY.x + " maxY: "+this.maxXY.y);
		this.visTransform = new CoordinateTransformation() {
			@Override
			public Coord transform(Coord c) {
				final double y = -(c.getY() + offsetY);
				
				double elevation;
				try{
					elevation = c.getZ();
					return new Coord(c.getX() + offsetX, y, elevation);
				} catch (Exception e){
					return new Coord(c.getX() + offsetX, y);
				}
			}
		};
	}
	
	private Point2D.Double calcVisPosition(Coord c){
		Point2D.Double p = new Point2D.Double(c.getX() + this.offsetX,
				-(c.getY() + this.offsetY));		
		return p;
	}
	
	public Point2D.Float getBoundingBoxMin(){
		return this.minXY;
	}
	
	public Point2D.Float getBoundingBoxMax(){
		return this.maxXY;
	}

	public CoordinateTransformation getVisTransform() {
		return this.visTransform;
	}
	
}
