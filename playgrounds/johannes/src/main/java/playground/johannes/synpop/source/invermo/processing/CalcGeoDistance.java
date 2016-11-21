/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.invermo.processing;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.source.invermo.InvermoKeys;

/**
 * @author johannes
 *
 */
public class CalcGeoDistance implements EpisodeTask {

	private final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	
	private final DistanceCalculator dCalc = OrthodromicDistanceCalculator.getInstance();
	

	@Override
	public void apply(Episode plan) {
		for(int i = 0; i < plan.getLegs().size(); i++) {
			Attributable prev = plan.getActivities().get(i);
			Attributable leg = plan.getLegs().get(i);
			Attributable next = plan.getActivities().get(i + 1);
			
			String sourceStr = prev.getAttribute(InvermoKeys.COORD);
			String destStr = next.getAttribute(InvermoKeys.COORD);
			
			if(sourceStr != null && destStr != null) {
				Point source = string2Coord(sourceStr);
				Point dest = string2Coord(destStr);
				
				double d = dCalc.distance(source, dest);

				leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(d));
			}
		}

	}
	
	private Point string2Coord(String str) {
		String tokens[] = str.split(",");
		double x = Double.parseDouble(tokens[0]);
		double y = Double.parseDouble(tokens[1]);

		Point p = factory.createPoint(new Coordinate(x, y));
		p.setSRID(4326);
		return p;
	}
}
