/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.plausibility.log;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.HashMap;
import java.util.Map;

public class DirectionChangeWarning extends PlausibilityWarningAbstract {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private final Link link1;
	private final Link link2;
	private final double diff;

	public DirectionChangeWarning(TransitLine transitLine, TransitRoute transitRoute, Link link1, Link link2, double diff) {
		super(3, "direction", transitLine, transitRoute);
		this.link1 = link1;
		this.link2 = link2;
		this.diff = diff;

		expected = 0;
		actual = 0;
		difference = diff;

		coordinates = new Coordinate[3];
		coordinates[0] = MGC.coord2Coordinate(link1.getFromNode().getCoord());
		coordinates[1] = MGC.coord2Coordinate(link1.getToNode().getCoord());
		coordinates[2] = MGC.coord2Coordinate(link2.getToNode().getCoord());

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	@Override
	public String toString() {
		return "\tDIRECTION CHANGE\tlinks: "+link1.getId()+"\t->\t"+link2.getId()+"\t\tdifference: "+diff*200/Math.PI +" gon";
	}
}
