/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class RandomRouteStartExtension extends PStrategy implements PPlanStrategy{
	
	private static final Logger log = Logger
	.getLogger(RandomRouteStartExtension.class);
	public static final String STRATEGY_NAME = "RandomRouteStartExtension";
	private double distanceFactor;
	private QuadTree<TransitStopFacility> tree = null;
	
	/**
	 * Finds the (already used) stop, with the greatest distance to the start-stop and adds another stop after. The new stop will be found by aiming 
	 * from the first stop to the stop in the greatest distance and spread out a rectangle to find stops within this rectangle...
	 *
	 * @param parameter 
	 */
	public RandomRouteStartExtension(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("only one parameter allowed. set the distancefactor to 0.5");
			this.distanceFactor = 0.5;
		}else{
			Double d = Double.parseDouble(parameter.get(0));
			if((d >= 0.0) && (d <= 1.0)){
				this.distanceFactor = d; 
			}else{
				log.error("the parameter should be a double between 0.0 and 1.0... set the distancefactor to 0.5");
				this.distanceFactor = 0.5;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PPlanStrategy#run(playground.andreas.P2.pbox.Cooperative)
	 */
	@Override
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		this.initQuadTree(cooperative);
		PPlan oldPlan = cooperative.getBestPlan();
		PPlan newPlan = new PPlan(oldPlan.getId());
		newPlan.setStartTime(oldPlan.getStartTime());
		newPlan.setEndTime(oldPlan.getEndTime());
		
		List<TransitStopFacility> stopsToServe = createNewStopsToServe(cooperative, tree); 
		if(stopsToServe == null){
			return null;
		}
		newPlan.setStopsToBeServed((ArrayList<TransitStopFacility>) stopsToServe);
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), 
																	newPlan.getStartTime(), 
																	newPlan.getEndTime(), 
																	1, 
																	(ArrayList<TransitStopFacility>) stopsToServe, 
																	new IdImpl(cooperative.getCurrentIteration())));
		
		if(cooperative.getFranchise().planRejected(newPlan)){
			// plan is rejected by franchise system
			return null;
		}
		return newPlan;
	}
	
	/**
	 * @param cooperative
	 * @param tree 
	 * @return
	 */
	private List<TransitStopFacility> createNewStopsToServe(Cooperative cooperative, QuadTree<TransitStopFacility> tree) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		List<TransitStopFacility> stops2serve = new ArrayList<TransitStopFacility>();
		stops2serve.addAll(cooperative.getBestPlan().getStopsToBeServed());
		// get already served stops in the correct order
		List<TransitStopFacility> alreadyServed = new ArrayList<TransitStopFacility>(); 
		for(TransitRoute r: cooperative.getBestPlan().getLine().getRoutes().values()){
			for(TransitRouteStop s: r.getStops()){
				alreadyServed.add(s.getStopFacility());
			}
			//all routes should be the same here, so break
			break;
		}
		
		/* find the stop within the greatest distance from the stops2beServed-list, assuming that this is the turning-point of the route  
		 * use stops2serve and NOT alreadyServed, because alreadyServed might not be consistent or not necessary for the new route
		 */
		TransitStopFacility stopInGreatestDistance = findStopInGreatestDistance(stops2serve);
		if(stopInGreatestDistance == null){
			// normally this should not happen, because there have to be at least 2 stops...
			log.error("can not find a second stop. returning original stops2serve...");
			stops2serve = null;
		}else{
			//find candidate-stops which are within a specified area and not served already...
			Collection<TransitStopFacility> candidates = findCandidates(alreadyServed, stopInGreatestDistance);
			if(candidates.size() == 0){
				// should only happen if the end of the route is at the periphery of the network
				log.error("there is no unserved stop within the specified distance. can not add a new stop. returning original stops2serve...");
				stops2serve = null;
			}else{
				stops2serve = addCandidate(candidates, stops2serve);
			}
		}
		return stops2serve;
	}

	/**
	 * @param stops2serve
	 * @return
	 */
	private TransitStopFacility findStopInGreatestDistance(List<TransitStopFacility> stops2serve) {
		TransitStopFacility first = stops2serve.get(0);
		TransitStopFacility temp;
		Integer index = null;
		double maxDist = -1., currentDist;
		
		for(int i = 1; i < stops2serve.size(); i++ ){
			temp = stops2serve.get(i);
			currentDist = CoordUtils.calcDistance(temp.getCoord(), first.getCoord());
			if(currentDist > maxDist){
				maxDist =  currentDist;
				index = i;
			}
		}
		/*
		 * what may be NULL here? 
		 * 
		 * could be "index", but even if it is a circle-line and the stops2serve consists of the same stop twice,
		 * index is set once, because 0 > Double.MIN_VALUE
		 * 
		 * is stops2serve.size() ==  1 possible? then index may be NULL
		 */
		if(index == null){
			log.error("this should never happen, stops2Serve should consist of at least 2 stops...");
//			for(TransitStopFacility f: stops2serve){
//				log.error(f.getId().toString() + "\t" + f.getCoord().toString());
//			}
			return null;
		}
		return stops2serve.get(index);
	}

	/**
	 * @param alreadyServed
	 * @param stopInGreatestDistance
	 * @return
	 */
	private Collection<TransitStopFacility> findCandidates(List<TransitStopFacility> alreadyServed, TransitStopFacility stopInGreatestDistance) {
		Geometry g = createCandidateArea(alreadyServed.get(0), stopInGreatestDistance);
		/*
		 * this step is necessary due to the fact that the QuadTree can only process its own "Rect",
		 * which is always orthogonal to a ordinary coordSystem, because you can only specify max/min Values instead of boarderPoints
		 * 
		 * Processing all stops, checking if they are part of the Geometry, might be to expensive for larger networks...
		 */
		Collection<TransitStopFacility> reduced =  reduceCandidates(g);
		List<TransitStopFacility> candidates =  new ArrayList<TransitStopFacility>();
		
		for(TransitStopFacility s: reduced){
			if(!alreadyServed.contains(s)){
				if(g.contains(MGC.coord2Point(s.getCoord()))){
					candidates.add(s);
				}
			}
		}
		
		return candidates;
	}

	/**
	 * @param g 
	 * @return
	 */
	private Collection<TransitStopFacility> reduceCandidates(Geometry g) {
		double minX, minY, maxX, maxY, x,y;
		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = Double.MIN_VALUE;
		maxY = Double.MIN_VALUE;
		
		for(Coordinate c: g.getCoordinates()){
			x = c.x;
			if(x < minX){
				minX = x;
			}
			if(x > maxX){
				maxX = x;
			}
			y = c.y;
			if(y < minY){
				minY = y;
			}
			if(y > maxY){
				maxY = y;
			}
		}
		
		return this.tree.get(minX, minY, maxX, maxY, new ArrayList<TransitStopFacility>());
	}

	/**
	 * @param firstServed
	 * @param stopInGreatestDistance
	 * @return
	 */
	private Geometry createCandidateArea(TransitStopFacility firstServed,TransitStopFacility stopInGreatestDistance) {
		Coord first, base, direction, target1, target2, normal;
		first = firstServed.getCoord();
		base = stopInGreatestDistance.getCoord();
		direction = CoordUtils.minus(first, base);
		target1 = CoordUtils.plus(first, CoordUtils.scalarMult((1-this.distanceFactor), direction));
		target2 = CoordUtils.plus(first, direction);

		double length = CoordUtils.length(direction);
		normal = CoordUtils.scalarMult(1/length, CoordUtils.rotateToRight(direction));
		
		Coordinate[] c = new Coordinate[4];
		c[0] = MGC.coord2Coordinate(CoordUtils.plus(target1, CoordUtils.scalarMult(length/2, normal)));
		c[1] = MGC.coord2Coordinate(CoordUtils.plus(target1, CoordUtils.scalarMult(-1*length/2, normal)));
		c[2] = MGC.coord2Coordinate(CoordUtils.plus(target2, CoordUtils.scalarMult(length/2, normal)));
		c[3] = MGC.coord2Coordinate(CoordUtils.plus(target2, CoordUtils.scalarMult(-1*length/2, normal)));
		
		GeometryFactory f = new GeometryFactory();
		return f.createMultiPoint(c).convexHull();
	}

	/**
	 * @param candidates
	 * @param stops2serve
	 * @param indexStopInGreatestDistance 
	 * @return
	 */
	private List<TransitStopFacility> addCandidate(Collection<TransitStopFacility> candidates, List<TransitStopFacility> stops2serve) {
		//draw a random stop from the candidatesList
		TransitStopFacility temp = (TransitStopFacility) candidates.toArray()[(int)(MatsimRandom.getRandom().nextDouble() * candidates.size())];
//		do{
//			for(TransitStopFacility s: candidates){
//				if(MatsimRandom.getRandom().nextDouble() < (1.0/candidates.size())){
//					temp = s;
//					break;
//				}
//			}
//		}while(temp == null);
		//insert before the first stop
		stops2serve.add(0, temp);
		return stops2serve;
	}

	/**
	 * @param cooperative
	 */
	private void initQuadTree(Cooperative cooperative) {
		//init only once
		if(this.tree != null) return;
		//find bounding box
		double minX, minY, maxX, maxY, x,y;
		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = Double.MIN_VALUE;
		maxY = Double.MIN_VALUE;
		for(TransitStopFacility f: cooperative.getRouteProvider().getAllPStops()){
			x = f.getCoord().getX();
			if(x < minX){
				minX = x;
			}
			if(x > maxX){
				maxX = x;
			}
			y = f.getCoord().getY();
			if(y < minY){
				minY = y;
			}
			if(y > maxY){
				maxY = y;
			}
		}
		//add values 
		QuadTree<TransitStopFacility> tree = new QuadTree<TransitStopFacility>(minX, minY, maxX, maxY);
		for(TransitStopFacility f: cooperative.getRouteProvider().getAllPStops()){
			tree.put(f.getCoord().getX(), f.getCoord().getY(), f);
		}
		this.tree = tree;
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PPlanStrategy#getName()
	 */
	@Override
	public String getName() {
		return RandomRouteStartExtension.STRATEGY_NAME;
	}


}
