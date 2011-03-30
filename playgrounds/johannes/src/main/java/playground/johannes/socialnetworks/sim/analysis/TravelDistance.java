/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class TravelDistance {

	private boolean geodesicMode = false;

	private Network network;
//	private DistanceCalculator distanceCalculator = new OrthodromicDistanceCalculator();
	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();

	private GeometryFactory geoFactory = new GeometryFactory();

	public TravelDistance(Network network) {
		this.network = network;
	}
	
	public void setGeodesicMode(boolean flag) {
		this.geodesicMode = flag;
	}
	
	public Map<String, DescriptiveStatistics> statistics(Set<Plan> plans) {
		Map<String, DescriptiveStatistics> statsMap = new HashMap<String, DescriptiveStatistics>();
		int cnt0 = 0;
		for (Plan plan : plans) {

			for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
//				Leg leg = (Leg) plan.getPlanElements().get(i);

				if (plan.getPlanElements().size() > i + 1) {
					Activity act = (Activity) plan.getPlanElements().get(i + 1);

					String type = act.getType();//.substring(0, 1);
					DescriptiveStatistics stats = statsMap.get(type);
					if (stats == null) {
						stats = new DescriptiveStatistics();
						statsMap.put(type, stats);
					}

					double d = getDistance(plan, i);
					if(!Double.isNaN(d) && d > 0) {
						stats.addValue(d);
						if(d==0)
							cnt0++;
					}
				}
			}
		}

		return statsMap;
	}

	private double getDistance(Plan plan, int idx) {
		if (geodesicMode) {
			Activity start = (Activity) plan.getPlanElements().get(idx - 1);
			Activity dest = (Activity) plan.getPlanElements().get(idx + 1);

			Point p1 = getPoint(start);//geoFactory.createPoint(new Coordinate(start.getCoord().getX(), start.getCoord().getY()));
			p1.setSRID(4326);
			Point p2 = getPoint(dest);//geoFactory.createPoint(new Coordinate(dest.getCoord().getX(), dest.getCoord().getY()));
			p2.setSRID(4326);

			double r = distanceCalculator.distance(p1, p2);

			return r;
		} else {
			Route route = ((Leg) plan.getPlanElements().get(idx)).getRoute();
			if (route == null)
				return 0;
			else {
				return route.getDistance();
			}
		}
	}
	
	private Point getPoint(Activity act) {
		if(act.getCoord() != null) {
			return geoFactory.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
		} else {
			Coord c = network.getLinks().get(act.getLinkId()).getCoord();
			return geoFactory.createPoint(new Coordinate(c.getX(), c.getY()));
		}
	}
}
