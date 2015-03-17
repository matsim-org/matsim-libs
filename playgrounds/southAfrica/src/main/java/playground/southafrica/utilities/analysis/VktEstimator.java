/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to estimate the vehicle kilometres travelled (vkt) for activity
 * chains of different types.
 * 
 * @author jwjoubert
 */
public class VktEstimator {
	final private static Logger LOG = Logger.getLogger(VktEstimator.class);
	final static Double DISTANCE_MULTIPLIER = 1.3;

	
	/**
	 * Estimates the vehicle kilometres travelled (vkt) of a {@link Plan} 
	 * within a given {@link Geometry}. The distance is calculated as the 
	 * overlap/intersection between the straight-line segments of the plan and 
	 * the geometry, and then multiplied by the (currently hard coded) distance
	 * multiplier.  
	 *  
	 * @param plan
	 * @param geom
	 * @return
	 */
	public static double estimateVktFromPlan(Plan plan, Geometry geom){
		double vktLeg = 0.0;
		double vktAct = 0.0;
		Geometry envelope = geom.getEnvelope();
		
		GeometryFactory gf = new GeometryFactory();
		
		Activity lastActivity = null;
		boolean lastActivityInside = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				if(lastActivity == null){
					lastActivity = act;
					Coord c = act.getCoord();
					if(c != null){
						Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
						if(envelope.covers(p)){
							if(geom.covers(p)){
								lastActivityInside = true;
							}
						}
					}
				} else{
					/* Check if current activity is inside. */
					boolean inside = false;
					Coord c = act.getCoord();
					if(c != null){
						Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
						if(envelope.covers(p)){
							if(geom.covers(p)){
								inside = true;
							}
						}
						
						/* Do something with the activity pair. */
						Coordinate c1 = new Coordinate(lastActivity.getCoord().getX(), lastActivity.getCoord().getY());
						Coordinate c2 = new Coordinate(act.getCoord().getX(), act.getCoord().getY());
						Coordinate[] ca = {c1, c2};
						LineString ls = gf.createLineString(ca);
						
						if(lastActivityInside && inside){
							/* Both are inside and we can use the entire segment. */
							vktAct += ls.getLength()*DISTANCE_MULTIPLIER;
						} else if(lastActivityInside && !inside /* Entry */ ||
								!lastActivityInside && inside   /* Exit  */){ 
							/* It is an exit. Calculate vkt accordingly. */
							Geometry intersection = geom.intersection(ls);
							if(intersection instanceof LineString){
								vktAct += ((LineString)intersection).getLength()*DISTANCE_MULTIPLIER;
							} else{
								LOG.warn("Don't know what to do if the intersection is of type " + intersection.getClass().getName());
							}
						} else{
							LOG.warn("Activity pair outside geometry.");
						}
					}
					
					/* Update the last activity. */
					lastActivity = act;
					lastActivityInside = inside;
				}
			} else{
				Leg leg = (Leg)pe;
				/* Do something with the leg. */
				double legDistance = 0.0;
				if(leg.getRoute() != null){
					legDistance = leg.getRoute().getDistance();
				} else{
					/* Cannot do anything with it... */
				}
				vktLeg += legDistance;
			}
		}
		
		double vkt;
		if(vktLeg == 0){
			vkt = vktAct;
		} else{
			vkt = (vktLeg + vktAct)/2.0;
		}
		return vkt;
	}

}
