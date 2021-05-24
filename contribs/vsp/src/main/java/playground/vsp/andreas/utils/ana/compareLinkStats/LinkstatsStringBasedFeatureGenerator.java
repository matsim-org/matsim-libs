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
import java.util.LinkedHashMap;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LinkstatsStringBasedFeatureGenerator implements FeatureGenerator{

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final PolylineFeatureFactory factory;
	private final HashMap<String, ArrayList<Double>> compareResultMap;

	public LinkstatsStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.factory = initFeatureFactory();

		this.compareResultMap = EvaluateLinkstats.compareLinkstatFiles(LinkStatsCompareConfig.linkStatsFileOne, LinkStatsCompareConfig.linkStatsFileTwo);
	}

	private PolylineFeatureFactory initFeatureFactory() {
		PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder();
		builder.setName("links");
		builder.setCrs(this.crs);
		
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("visWidth", Double.class);
		builder.addAttribute("type", String.class);
		
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

		Coordinate[] coordinates = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())};

		Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		attributes.put("ID", link.getId().toString());
		attributes.put("fromID", link.getFromNode().getId().toString());
		attributes.put("toID", link.getToNode().getId().toString());
		attributes.put("length", link.getLength());
		attributes.put("freespeed", link.getFreespeed());
		attributes.put("capacity", link.getCapacity());
		attributes.put("lanes", link.getNumberOfLanes());
		attributes.put("visWidth", width);
		attributes.put("type", NetworkUtils.getType(((Link) link)));

		// 3 hour average
		if(this.compareResultMap.get(link.getId().toString()) != null){
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < 8; i++) {
				attributes.put("HRS" + (i + i * 2) + "-" + (i + i * 2 + 3) + "avg", 
					Double.valueOf((tempArray.get(i + i * 2).doubleValue() + tempArray.get(i + 1 + i * 2).doubleValue() + tempArray.get(i + 2 + i * 2).doubleValue()) / 3));
			}
		}
		
		double average24hours = 0.0;

		// 1 hour average
		if(this.compareResultMap.get(link.getId().toString()) != null){
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < tempArray.size(); i++) {
				attributes.put("HRS" + i + "-" + (i+1) + "avg", tempArray.get(i));
				average24hours += tempArray.get(i).doubleValue();
			}
		}
		
		// 24 hour average
		attributes.put("HRS 24 avg", new Double(average24hours / 24));
		
		return this.factory.createPolyline(coordinates, attributes, link.getId().toString());
	}

}
