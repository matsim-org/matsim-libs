/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package vwExamples.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;


public class CreateStopsFromGrid {
	public static void main(String[] args) {

		String networkFile = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\network\\modifiedNetwork.xml.gz";
		final String networkModeDesignator = "drt";
		double gridCellSize = 600;
		String transitStopsOutputFile = "D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\virtualstops\\stopsGrid_" + Math.round(gridCellSize) + "m.xml";
		TransitScheduleFactory f = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = f.createTransitSchedule();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		NetworkFilterManager nfm = new NetworkFilterManager(network);
		nfm.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains(networkModeDesignator)) {
					if ((l.getFreespeed() > 7) && l.getFreespeed() < 20 && l.getLength() > 15)
						return true;
				}

				return false;
			}
		});
		Network stopNetwork = nfm.applyFilters();

		Map<String, Geometry> netGrid = DrtGridUtils.createGridFromNetwork(stopNetwork, gridCellSize);
		for (Geometry g : netGrid.values()) {
			Coord centroid = MGC.point2Coord(g.getCentroid());
			Link link = NetworkUtils.getNearestLink(stopNetwork, centroid);
			TransitStopFacility stop = f.createTransitStopFacility(
					Id.create(link.getId().toString() + "_stop", TransitStopFacility.class), link.getCoord(), false);
			stop.setLinkId(link.getId());
			if (!schedule.getFacilities().containsKey(stop.getId())) {
				schedule.addStopFacility(stop);
			}
			Link backLink = NetworkUtils.findLinkInOppositeDirection(link);
			if (backLink != null) {
				TransitStopFacility backstop = f.createTransitStopFacility(
						Id.create(backLink.getId().toString() + "_stop", TransitStopFacility.class),
						backLink.getCoord(), false);
				backstop.setLinkId(backLink.getId());
				if (!schedule.getFacilities().containsKey(backstop.getId())) {
					schedule.addStopFacility(backstop);
				}
			}

		}
		new TransitScheduleWriter(schedule).writeFile(transitStopsOutputFile);
		writeShape(transitStopsOutputFile.replace(".xml", ".shp"), netGrid);

	}

	private static void writeShape(String outfile, Map<String, Geometry> zones) {

		CoordinateReferenceSystem crs;

		crs = MGC.getCRS("EPSG:25832");

		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class).setCrs(crs)
				.setName("zone").create();

		List<SimpleFeature> features = new ArrayList<>();

		for (Entry<String, Geometry> z : zones.entrySet()) {
			Object[] attribs = new Object[1];

			attribs[0] = z.getKey();

			features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
		}

		ShapeFileWriter.writeGeometries(features, outfile);

	}
}
