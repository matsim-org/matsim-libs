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

package playground.vsp.andreas.utils.ana.countVehOnLinks;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;

public class CountVehOnLinksPolygonBasedFeatureGenerator implements FeatureGenerator{

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final HashMap<String, Integer> compareResultMap;
	private final PolygonFeatureFactory factory;

	public CountVehOnLinksPolygonBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.factory = initFeatureFactory();

		this.compareResultMap = CountVehOnLinks.compareEventFiles(EventsCompareConfig.eventsFileOne, EventsCompareConfig.eventsFileTwo);
	}
	
	private PolygonFeatureFactory initFeatureFactory() {
		return new PolygonFeatureFactory.Builder().
				setCrs(this.crs).
				setName("link")
				.addAttribute("ID", String.class)
				.addAttribute("fromID", String.class)
				.addAttribute("toID", String.class)
				.addAttribute("length", Double.class)
				.addAttribute("freespeed", Double.class)
				.addAttribute("capacity", Double.class)
				.addAttribute("lanes", Double.class)
				.addAttribute("visWidth", Double.class)
				.addAttribute("Diff", Integer.class)
				.create();
	}

	@Override
	public SimpleFeature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);
		width += 0;

		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
		double length = from.distance(to);

		final double dx = -from.x   + to.x;
		final double dy = -from.y   + to.y;

		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double xfrom2 = from.x + Math.cos(theta) * 0  + Math.sin(theta) * width;
		double yfrom2 = from.y + Math.sin(theta) * 0 - Math.cos(theta) * width;
		double xto2 = from.x + Math.cos(theta) *  length + Math.sin(theta) * width;
		double yto2 = from.y + Math.sin(theta) * length - Math.cos(theta) * width;
		Coordinate from2 = new Coordinate(xfrom2,yfrom2);
		Coordinate to2 = new Coordinate(xto2,yto2);

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("ID", link.getId().toString());
		attributes.put("fromID", link.getFromNode().getId().toString());
		attributes.put("toID", link.getToNode().getId().toString());
		attributes.put("length", link.getLength());
		attributes.put("freespeed", link.getFreespeed());
		attributes.put("capacity", link.getCapacity());
		attributes.put("lanes", link.getNumberOfLanes());
		attributes.put("visWidth", width);
		if (this.compareResultMap.get(link.getId().toString()) != null) {
			attributes.put("Diff", this.compareResultMap.get(link.getId().toString()));
		}
		
		return this.factory.createPolygon(new Coordinate[] {from, to, to2, from2, from}, attributes, link.getId().toString());
	}

}
