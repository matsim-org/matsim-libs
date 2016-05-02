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

package playground.polettif.multiModalMap.validation;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.*;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Schedule2ShapeFileConverter {

	private static final Logger log = Logger.getLogger(Schedule2ShapeFileConverter.class);

	private Map<Id<Link>, ? extends Link> links;
	private final TransitSchedule schedule;
	private final Network network;
	private Collection<SimpleFeature> features;

	public Schedule2ShapeFileConverter(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
		features = new ArrayList<>();

	}

	public static void main(final String[] arg) {
		String[] args = new String[4];
		args[0] = "C:/Users/polettif/Desktop/output/PublicTransportMap/zurich_gtfs_schedule.xml";
		args[1] = "C:/Users/polettif/Desktop/output/PublicTransportMap/zurich_gtfs_network.xml";
		args[2] = "C:/Users/polettif/Desktop/output/shp/lines.shp";
		args[3] = "C:/Users/polettif/Desktop/output/shp/stops.shp";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

		Schedule2ShapeFileConverter s2s = new Schedule2ShapeFileConverter(schedule, network);

		s2s.routes2Polyline(args[2]);
		s2s.stopFaclities2Points(args[3]);
	}

	private void stopFaclities2Points(String outFile) {

	}


	public void routes2Polyline(String outFile) {
		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("schedule_zurich")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("Line", String.class)
				.addAttribute("Route", String.class)
				.create();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				Coordinate[] coordinates = getCoordinatesFromRoute(transitRoute);
//				SimpleFeature f = SimpleFeatureBuilder.build();

				if(coordinates == null) {
					log.error("No links found for route " + transitRoute.getId() + " on line " + transitLine.getId());
				} else {
					SimpleFeature f = ff.createPolyline(coordinates);
					f.setAttribute("Line", transitLine.getId().toString());
					f.setAttribute("Route", transitRoute.getId().toString());
					features.add(f);
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, outFile);
	}

	private Coordinate[] getCoordinatesFromRoute(TransitRoute transitRoute) {
		List<Coordinate> coordList = new ArrayList<>();
		List<Id<Link>> linkList = new ArrayList<>();

		NetworkRoute networkRoute = transitRoute.getRoute();
		linkList.add(networkRoute.getStartLinkId());
		linkList.addAll(networkRoute.getLinkIds());

		for(Id<Link> linkId : linkList) {
			try {
				Coord coord = network.getLinks().get(linkId).getFromNode().getCoord();
				coordList.add(new Coordinate(coord.getX(), coord.getY()));
			} catch (Exception e) {
				return null;
			}
		}

		try {
			Coord lastCoord = network.getLinks().get(networkRoute.getEndLinkId()).getFromNode().getCoord();
			coordList.add(new Coordinate(lastCoord.getX(), lastCoord.getY()));
		} catch (Exception e) {
			return null;
		}

		Coordinate[] returnArray = new Coordinate[coordList.size()];

		return coordList.toArray(returnArray);
	}

	/*
	public Network convert(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {

		NetworkFilterManager filterManager = new NetworkFilterManager(network);
		filterManager.addLinkFilter(new TransitRouteLinkFilter(transitLineId, transitRouteId));

		return filterManager.applyFilters();
	}


	private class TransitRouteLinkFilter implements NetworkLinkFilter {

		List<Id<Link>> linkList = new ArrayList<>();

		public TransitRouteLinkFilter(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
			for(TransitRoute transitRoute : schedule.getTransitLines().get(transitLineId).getRoutes().values()) {
				if(transitRoute.getId().equals(transitRouteId)) {
					NetworkRoute networkRoute = transitRoute.getRoute();
					linkList.add(networkRoute.getStartLinkId());
					linkList.addAll(networkRoute.getLinkIds());
					linkList.add(networkRoute.getEndLinkId());
				}
			}
		}

		@Override
		public boolean judgeLink(Link l) {
			return linkList.contains(l.getId());
		}
	}

	public static void old() {
		String[] args = new String[3];
		args[0] = "C:/Users/polettif/Desktop/output/PublicTransportMap/zurich_gtfs_schedule.xml";
		args[1] = "C:/Users/polettif/Desktop/output/PublicTransportMap/zurich_gtfs_network.xml";
		args[2] = "C:/Users/polettif/Desktop/output/shp/test.shp";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		new TransitScheduleReader(sc).readFile(args[0]);
		new MatsimNetworkReader(network).readFile(args[1]);
		TransitSchedule schedule = sc.getTransitSchedule();

		Schedule2ShapeFileConverter s2s = new Schedule2ShapeFileConverter(schedule, network);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "EPSG:2056");
		builder.createFeatureGenerator();
		builder.setWidthCoefficient(0.1);

		Id<TransitLine> transitLineId =Id.create("2-33-P-j16-1", TransitLine.class);
		Id<TransitRoute> transitRouteId = Id.create("607.T0.2-33-P-j16-1.21.H", TransitRoute.class);

		new Links2ESRIShape(s2s.convert(transitLineId, transitRouteId), args[2], builder).write();
	}
	*/
}