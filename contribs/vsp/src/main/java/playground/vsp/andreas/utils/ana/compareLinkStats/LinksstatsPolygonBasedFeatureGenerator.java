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

package playground.vsp.andreas.utils.ana.compareLinkStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LinksstatsPolygonBasedFeatureGenerator implements FeatureGenerator{

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final PolygonFeatureFactory factory;
	private final HashMap<String, ArrayList<Double>> compareResultMap;

	public LinksstatsPolygonBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.factory = initFeatureFactory();

		this.compareResultMap = EvaluateLinkstats.compareLinkstatFiles(LinkStatsCompareConfig.linkStatsFileOne, LinkStatsCompareConfig.linkStatsFileTwo);
	}

	private PolygonFeatureFactory initFeatureFactory() {
		PolygonFeatureFactory.Builder builder = new PolygonFeatureFactory.Builder();
		builder.setCrs(this.crs);
		builder.setName("links");
		
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("visWidth", Double.class);
		builder.addAttribute("Diff", Integer.class);

		// 3 hour average
		for (int i = 0; i < 8; i++) {
			builder.addAttribute("HRS" + (i + i * 2) + "-" + (i + i * 2 + 3) + "avg", Double.class);
		}
		
		// 1 hour average
		for (int i = 0; i < 24; i++) {
			builder.addAttribute("HRS" + i + "-" + (i+1) + "avg", Double.class);
		}
		
		// 24 hour average
		builder.addAttribute("HRS 24 avg", Double.class);
		
		return builder.create();
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
		
		// 3 hour average
		if (this.compareResultMap.get(link.getId().toString()) != null) {
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < 8; i++) {
				attributes.put("HRS" + (i + i * 2) + "-" + (i + i * 2 + 3) + "avg", Double.valueOf((tempArray.get(i + i * 2).doubleValue() + tempArray.get(i + 1 + i * 2).doubleValue() + tempArray.get(i + 2 + i * 2).doubleValue()) / 3));
			}
		}
		
		double average24hours = 0.0;
		
		// 1 hour average
		if (this.compareResultMap.get(link.getId().toString()) != null) {
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < tempArray.size(); i++) {
				attributes.put("HRS" + i + "-" + (i+1) + "avg", tempArray.get(i));
				average24hours += tempArray.get(i).doubleValue();
			}
		}
		
		// 24 hour average
		attributes.put("HRS 24 avg", Double.valueOf(average24hours / 24));
		
		return this.factory.createPolygon(new Coordinate[] {from, to, to2, from2, from}, attributes, link.getId().toString());
	}

}
