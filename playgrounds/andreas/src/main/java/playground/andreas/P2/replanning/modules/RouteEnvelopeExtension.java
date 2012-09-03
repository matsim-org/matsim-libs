/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.AbstractPStrategyModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class RouteEnvelopeExtension extends AbstractPStrategyModule {
	
	
	private static final Logger log = Logger
			.getLogger(RouteEnvelopeExtension.class);
	public static final String STRATEGY_NAME = "RouteEnvelopeExtension";
	
	private Double dist = Double.MIN_VALUE;

	/**
	 * @param parameter
	 */
	public RouteEnvelopeExtension(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("a parameter determining the distance for the enevelope has to be defined. Default == Double.MIN_VALUE");
		}else{
			this.dist = Double.parseDouble(parameter.get(0));
		}
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#run(playground.andreas.P2.pbox.Cooperative)
	 */
	@Override
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			log.info("can not create a new plan for cooperative " + cooperative.getId() + " in iteration " + 
					cooperative.getCurrentIteration() + ". To Few vehicles.");
			return null;
		}
		
		// get a List of served stop-facilities in the sequence they are served
		List<TransitStopFacility> currentlyUsedStops = this.getUsedFacilities(cooperative);
		// create envelope in the specified distance 
		Geometry envelope = this.createEnvelope(currentlyUsedStops);
		// find currently unused stops inside the convex-hull
		List<TransitStopFacility> newEnvelopeInteriorStops = 
			this.findNewEnvelopeInteriorStops(cooperative.getRouteProvider().getAllPStops(), currentlyUsedStops, envelope);
		
		// draw a random stop from the candidates-list
		TransitStopFacility newStop = this.drawStop(newEnvelopeInteriorStops);
		if(newStop == null){
			log.info("can not create a new plan for cooperative " + cooperative.getId() + " in iteration " + 
					cooperative.getCurrentIteration() + ". No unused stop in the envelope of the old route.");
			return null;
		}else{
			// create a new plan 
			PPlan oldPlan = cooperative.getBestPlan();
			PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName());
			newPlan.setStartTime(oldPlan.getStartTime());
			newPlan.setEndTime(oldPlan.getEndTime());
			//insert the new stop at the correct point (minimum average Distance from the subroute to the new Stop) in the sequence of stops 2 serve
			List<TransitStopFacility> stopsToServe = createNewStopsToServe(cooperative, newStop); 
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
				log.info("can not create a new plan for cooperative " + cooperative.getId() + " in iteration " + 
						cooperative.getCurrentIteration() + ". rejected by franchise.");
				return null;
			}
			
			return newPlan;
		}
	}


	/**
	 * @param cooperative
	 * @param newStop
	 * @return
	 */
	private List<TransitStopFacility> createNewStopsToServe(Cooperative cooperative, TransitStopFacility newStop) {
		// find the subroutes, between the stops to be served
		List<List<TransitStopFacility>> subrouteFacilities = this.findSubroutes(cooperative, newStop);
		List<Double> avDist = calcAvDist(subrouteFacilities, newStop);
		if(avDist.size() > cooperative.getBestPlan().getStopsToBeServed().size()){
			log.info("can not create a new plan for cooperative " + cooperative.getId() + " in iteration " + 
					cooperative.getCurrentIteration() + ". more subroutes then expected were found.");
			return null;
		}
		
		//calculate the average distance from the new stop to the subroute and add the new stop between the 2 "stops2beServed" of the subroute
		int index = 0;
		double temp = Double.MAX_VALUE;
		for(int i = 0; i < avDist.size(); i++){
			if(avDist.get(i) < temp){
				temp = avDist.get(i);
				index = i;
			}
		}
		List<TransitStopFacility> stops2serve = new ArrayList<TransitStopFacility>();
		stops2serve.addAll(cooperative.getBestPlan().getStopsToBeServed());
		stops2serve.add(index + 1, newStop);
		
		return stops2serve;
	}

	/**
	 * @param subrouteFacilities
	 * @return
	 */
	private List<Double> calcAvDist(List<List<TransitStopFacility>> subrouteFacilities, TransitStopFacility newStop) {
		List<Double> dist = new ArrayList<Double>();
		double temp;
		
		for(List<TransitStopFacility> subroute: subrouteFacilities){
			temp = 0;
			
			for(TransitStopFacility t: subroute){
				temp += CoordUtils.calcDistance(t.getCoord(), newStop.getCoord());
			}
			temp = temp/subroute.size();
			dist.add(temp);
		}
		return dist;
	}

	/**
	 * finds the subroutes between stops2beServed+
	 * 
	 * @param cooperative
	 * @param newStop
	 * @return
	 */
	private List<List<TransitStopFacility>> findSubroutes(Cooperative cooperative, TransitStopFacility newStop) {
		List<List<TransitStopFacility>> subroutes = new ArrayList<List<TransitStopFacility>>();
		ArrayList<TransitStopFacility> temp = null;
		TransitStopFacility fac;
		
		for(TransitRouteStop s: cooperative.getBestPlan().getLine().getRoutes().values().iterator().next().getStops()){
			fac = s.getStopFacility();
			if(temp == null){
				temp = new ArrayList<TransitStopFacility>();
				temp.add(fac);
			}else{
				temp.add(fac);
				if(cooperative.getBestPlan().getStopsToBeServed().contains(s.getStopFacility())){
					subroutes.add(temp);
					temp = new ArrayList<TransitStopFacility>();
					temp.add(fac);
				}
			}
		}
		
		return subroutes;
	}

	/**
	 * @param cooperative
	 * @return
	 */
	private List<TransitStopFacility> getUsedFacilities(Cooperative cooperative) {
		List<TransitStopFacility> currentlyUsedStops = new ArrayList<TransitStopFacility>();
		
		for (TransitRouteStop stop : cooperative.getBestPlan().getLine().getRoutes().values().iterator().next().getStops()) {
			currentlyUsedStops.add(stop.getStopFacility());
		}
		return currentlyUsedStops;
	}

	/**
	 * @param currentlyUsedStops
	 * @return
	 */
	private Geometry createEnvelope(List<TransitStopFacility> currentlyUsedStops) {
		Geometry[] objects = new Geometry[currentlyUsedStops.size() - 1];
		GeometryFactory fac = new GeometryFactory();
		
		Coord one, two;
		for(int i = 1; i < currentlyUsedStops.size(); i++){
			one = currentlyUsedStops.get(i-1).getCoord();
			two = currentlyUsedStops.get(i).getCoord();
			// create a hull in the specified distance
			objects[i-1] = fac.createMultiPoint(getHullForTwoCoords(one,two)).convexHull();
		}
		// merge geometries. we only need one...
		return fac.createGeometryCollection(objects).buffer(0);
	}

	/**
	 * @param one
	 * @param two
	 * @return
	 */
	private Coordinate[] getHullForTwoCoords(Coord one, Coord two) {
		Coord first, second, direction, normal, unit;
		first = one;
		second = two;
		direction = CoordUtils.minus(second, first);
		unit = CoordUtils.scalarMult(1/CoordUtils.length(direction), direction);
		first = CoordUtils.minus(first, CoordUtils.scalarMult(this.dist, unit));
		second = CoordUtils.minus(second, CoordUtils.scalarMult(this.dist, unit));
		normal = CoordUtils.scalarMult(1/CoordUtils.length(direction), CoordUtils.rotateToRight(direction));
		
		Coordinate[] c = new Coordinate[4];
		c[0] = MGC.coord2Coordinate(CoordUtils.plus(first, CoordUtils.scalarMult(this.dist, normal)));
		c[1] = MGC.coord2Coordinate(CoordUtils.plus(first, CoordUtils.scalarMult(-1*this.dist, normal)));
		c[2] = MGC.coord2Coordinate(CoordUtils.plus(second, CoordUtils.scalarMult(this.dist, normal)));
		c[3] = MGC.coord2Coordinate(CoordUtils.plus(second, CoordUtils.scalarMult(-1*this.dist, normal)));
		return c;
	}

	/**
	 * returns stops within the convex hull, which are not part of the current route, but of its convex hull
	 * 
	 * @param stopsToBeServed
	 * @param currentlyUsedStops
	 * @param envelope
	 * @return
	 */
	private List<TransitStopFacility> findNewEnvelopeInteriorStops(Collection<TransitStopFacility> possibleStops, 
																List<TransitStopFacility> currentlyUsedStops, 
																Geometry envelope) {
		List<TransitStopFacility> stopCandidates = new ArrayList<TransitStopFacility>();
		
		for(TransitStopFacility s: possibleStops){
			if(!currentlyUsedStops.contains(s)){
				if(envelope.contains(MGC.coord2Point(s.getCoord()))){
					stopCandidates.add(s);
				}
			}
		}
		return stopCandidates;
	}
	
	/**
	 * @param newHullInteriorStops
	 * @return
	 */
	private TransitStopFacility drawStop(
			List<TransitStopFacility> newHullInteriorStops) {
		if(newHullInteriorStops.size() == 0){
			return null;
		}else{
			int rnd = (int)(MatsimRandom.getRandom().nextDouble() * (newHullInteriorStops.size() - 1));
			return newHullInteriorStops.get(rnd);
		}
	}

	/* (non-Javadoc)
	 * @see playground.andreas.P2.replanning.PStrategy#getName()
	 */
	@Override
	public String getName() {
		return STRATEGY_NAME;
	}

}
