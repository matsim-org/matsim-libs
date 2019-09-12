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

public class CountVehOnLinksStringBasedFeatureGenerator implements FeatureGenerator{

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final PolylineFeatureFactory factory;
	private final HashMap<String, Integer> compareResultMap;


	public CountVehOnLinksStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.factory = initFeatureFactory();

		this.compareResultMap = CountVehOnLinks.compareEventFiles(EventsCompareConfig.eventsFileOne, EventsCompareConfig.eventsFileTwo);
	}

	private PolylineFeatureFactory initFeatureFactory() {
		return new PolylineFeatureFactory.Builder()
			.setCrs(this.crs)
			.setName("link")
			.addAttribute("ID", String.class)
			.addAttribute("fromID", String.class)
			.addAttribute("toID", String.class)
			.addAttribute("length", Double.class)
			.addAttribute("freespeed", Double.class)
			.addAttribute("capacity", Double.class)
			.addAttribute("lanes", Double.class)
			.addAttribute("visWidth", Double.class)
			.addAttribute("type", String.class)
			.addAttribute("Diff", Double.class)
			.create();
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
		if (this.compareResultMap.get(link.getId().toString()) != null){
			attributes.put("Diff",  this.compareResultMap.get(link.getId().toString()));
		}

		return this.factory.createPolyline(coordinates, attributes, link.getId().toString());
	}

}
