/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDistance.java
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
package playground.johannes.socialnetworks.sim.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ActivityDistance {

	private static Logger logger = Logger.getLogger(ActivityDistance.class);
	
	private int srid;
	
	private DistanceCalculator distanceCalculator;
	
	private Network network;
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	public ActivityDistance(Network network, int srid) {
		this.network = network;
		this.srid = srid;
		this.distanceCalculator = new OrthodromicDistanceCalculator();
	}
	
	public ActivityDistance(Network network, int srid, DistanceCalculator calculator) {
		this(network, srid);
		this.distanceCalculator = calculator;
	}
	
	public Map<String, DescriptiveStatistics> statistics(Set<Trajectory> trajectories) {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>();
		DescriptiveStatistics all = new DescriptiveStatistics();
		
		int cnt0 = 0;
		for (Trajectory trajectory : trajectories) {

			for (int i = 1; i < trajectory.getElements().size(); i += 2) {
				if (trajectory.getElements().size() > i + 1) {
					Activity act = (Activity) trajectory.getElements().get(i + 1);

					String type = act.getType();
					DescriptiveStatistics stats = map.get(type);
					if (stats == null) {
						stats = new DescriptiveStatistics();
						map.put(type, stats);
					}

					Activity start = (Activity) trajectory.getElements().get(i - 1);
					Activity dest = (Activity) trajectory.getElements().get(i + 1);

					Point p1 = getPoint(start);
					p1.setSRID(srid);
					Point p2 = getPoint(dest);
					p2.setSRID(srid);

					double d = distanceCalculator.distance(p1, p2);
					if(d > 0) {
						stats.addValue(d);
						all.addValue(d);
					} else
						cnt0++;
				}
			}
		}
		
		map.put("all", all);
		
		if(cnt0 > 0)
			logger.warn(String.format("Ignored %1$s locations with zero distance.", cnt0));
		
		return map;
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
