/* *********************************************************************** *
 * project: org.matsim.*
 * PtRoute2QGIS.java
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

/**
 *
 */
package playground.yu.utils.qgis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import playground.yu.utils.container.CollectionSum;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author yu
 *
 */
public class PtRoute2QGIS extends MATSimNet2QGIS {
	public static class PtRoute2PolygonGraph extends X2GraphImpl {
		protected TransitSchedule schedule;

		public PtRoute2PolygonGraph(CoordinateReferenceSystem crs,
				TransitSchedule schedule) {
			this.schedule = schedule;

			this.geofac = new GeometryFactory();
			// this.network = network;
			this.crs = crs;
			features = new ArrayList<Feature>();

			AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
					"MultiPolygon", MultiPolygon.class, true, null, null,
					this.crs);
			AttributeType id = AttributeTypeFactory.newAttributeType("ID",
					String.class);
			AttributeType transMode = AttributeTypeFactory.newAttributeType(
					"transMode", String.class);
			AttributeType links = AttributeTypeFactory.newAttributeType(
					"links", String.class);
			AttributeType stops = AttributeTypeFactory.newAttributeType(
					"stops", String.class);

			defaultFeatureTypeFactory = new DefaultFeatureTypeFactory();
			defaultFeatureTypeFactory.setName("pt-route");
			defaultFeatureTypeFactory.addTypes(new AttributeType[] { geom, id,
					transMode, links, stops });
		}

		@Override
		public Collection<Feature> getFeatures() throws SchemaException,
				NumberFormatException, IllegalAttributeException {
			for (int i = 0; i < attrTypes.size(); i++)
				defaultFeatureTypeFactory.addType(attrTypes.get(i));
			FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();

			for (TransitLine ptLine : this.schedule.getTransitLines().values())
				for (TransitRoute ptRoute : ptLine.getRoutes().values()) {
					Object[] o = new Object[5 + parameters.size()];

					LinearRing ptRouteRing = getPtRouteRing(ptRoute);
					Polygon pg = new Polygon(ptRouteRing, null, this.geofac);

					o[0] = new MultiPolygon(new Polygon[] { pg }, this.geofac);
					Id ptRouteId = ptRoute.getId();
					o[1] = ptRouteId.toString();
					o[2] = ptRoute.getTransportMode();
					o[3] = ptRoute.getRoute().getLinkIds().toString();
					StringBuffer stops = new StringBuffer();
					for (TransitRouteStop stop : ptRoute.getStops())
						stops.append(stop.getStopFacility().getId());

					o[4] = stops.toString();

					for (int i = 0; i < parameters.size(); i++)
						o[i + 5] = parameters.get(i).get(ptRouteId);

					features.add(ftRoad.create(o, "pt-route-net"));
				}
			return features;
		}

		private LinearRing getPtRouteRing(TransitRoute ptRoute) {

			double width = getPtRouteWidth(ptRoute);

			NetworkRoute route = ptRoute.getRoute();

			List<Link> links = new ArrayList<Link>();
			Link startLink = this.network.getLinks()
					.get(route.getStartLinkId());
			links.add(startLink);
			for (Id linkId : route.getLinkIds()) {
				links.add(this.network.getLinks().get(linkId));
			}
			links.add(this.network.getLinks().get(route.getEndLinkId()));

			List<Link> links2remove = new ArrayList<Link>();
			for (Link link : links)
				if (link.getLength() <= 0.0
						|| link.getFromNode().equals(link.getToNode()))
					links2remove.add(link);

			links.removeAll(links2remove);

			return new LinearRing(new CoordinateArraySequence(getCoords(links,
					width)), this.geofac);
		}

		private Coordinate[] getCoords(List<Link> links, double width) {
			System.out.println("links size\t" + links.size());

			Coordinate[] coords = new Coordinate[2 * (links.size() + 1) + 1];// to
			// return
			Coord firstFromCoord = links.get(0).getFromNode().getCoord();
			coords[0] = new Coordinate(firstFromCoord.getX(), firstFromCoord
					.getY());// first from Node Coordinate

			System.out.println("coords[0]\tis saved with\t" + coords[0]);

			List<Coordinate> coordinates = new ArrayList<Coordinate>();
			// tmp location of Coordinates (b,c)

			for (int i = 0; i < links.size(); i++) {
				Link link = links.get(i);
				Coord toCoord = link.getToNode().getCoord();
				coords[i + 1] = new Coordinate(toCoord.getX(), toCoord.getY());

				System.out.println("coords[" + (i + 1) + "]\tis saved with\t"
						+ coords[i + 1]);

				Tuple<Coordinate, Coordinate> coordinateTuple = getOffsetedCoordinatePair(
						link, width);
				coordinates.add(coordinateTuple.getFirst());
				coordinates.add(coordinateTuple.getSecond());
			}

			System.out.println("coordinates size\t" + coordinates.size());

			coords[coords.length - 2] = coordinates.get(0);
			coords[coords.length - 1] = coords[0];
			coords[links.size() + 1] = coordinates.get(coordinates.size() - 1);

			System.out.println("coords[" + (coords.length - 2)
					+ "]\tis saved with\t" + coords[coords.length - 2]);
			System.out.println("coords[" + (coords.length - 1)
					+ "]\tis saved with\t" + coords[coords.length - 1]);
			System.out.println("coords[" + (links.size() + 1)
					+ "]\tis saved with\t" + coords[links.size() + 1]);

			for (int i = 1; i < links.size(); i++) {
				Coordinate a = coords[i], b = coordinates.get(2 * i - 1), c = coordinates
						.get(2 * i);
				coords[coords.length - 2 - i] = getIntersectionCoord(a, b, c,
						width);
				System.out.println("coords[" + (coords.length - 2 - i)
						+ "]\tis saved with\t" + coords[coords.length - 2 - i]);
			}
			return coords;
		}

		/**
		 * @param a
		 *            original node
		 * @param b
		 *            node after offset
		 * @param c
		 *            node after offset
		 * @return
		 */
		private Coordinate getIntersectionCoord(Coordinate a, Coordinate b,
				Coordinate c, double width) {
			double x_bc = c.x - b.x, y_bc = c.y - b.y;
			double length_bc = Math.sqrt(x_bc * x_bc + y_bc * y_bc);

			double x_d = (b.x + c.x) / 2.0, y_d = (b.y + c.y) / 2.0;

			double beta = width * width
					/ (width * width - length_bc * length_bc / 4.0);

			double x_e = beta * (x_d - a.x) + a.x, y_e = beta * (y_d - a.y)
					+ a.y;

			return new Coordinate(x_e, y_e);
		}

		private Tuple<Coordinate, Coordinate> getOffsetedCoordinatePair(
				Link link, double width) {
			Coordinate from = getCoordinate(link.getFromNode().getCoord());
			Coordinate to = getCoordinate(link.getToNode().getCoord());

			double xdiff = to.x - from.x;
			double ydiff = to.y - from.y;
			double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
			double xwidth = width * ydiff / denominator;
			double ywidth = -width * xdiff / denominator;

			Coordinate fromB = new Coordinate(from.x + xwidth, from.y + ywidth);
			Coordinate toB = new Coordinate(to.x + xwidth, to.y + ywidth);

			return new Tuple<Coordinate, Coordinate>(fromB, toB);
		}

		private double getPtRouteWidth(TransitRoute ptRoute) {
			NetworkRoute route = ptRoute.getRoute();
			List<Id> linkIds = route.getLinkIds();
			int size = linkIds.size();

			double[] widths = new double[size + 2], lengths = new double[size + 2];

			Link startLink = this.network.getLinks()
					.get(route.getStartLinkId());
			widths[0] = 20;
			lengths[0] = startLink.getLength();

			int n = 0;
			for (Id linkId : linkIds) {
				widths[++n] = 20;
				lengths[n] = this.network.getLinks().get(linkId).getLength();
			}

			Link endLink = this.network.getLinks().get(route.getEndLinkId());
			widths[widths.length - 1] = 20;
			lengths[lengths.length - 1] = endLink.getLength();

			for (int i = 0; i < widths.length; i++)
				widths[i] *= lengths[i];

			return CollectionSum.getSum(widths) / CollectionSum.getSum(lengths);
		}
	}

	protected PtRoute2PolygonGraph p2g;

	/**
	 * @param netFilename
	 * @param coordRefSys
	 */
	public PtRoute2QGIS(String netFilename, String coordRefSys,
			String scheduleFilename) {
		super(netFilename, coordRefSys);
		this.scenario.getConfig().scenario().setUseTransit(true);
		try {
			new TransitScheduleReader(this.scenario).readFile(scheduleFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		p2g = new PtRoute2PolygonGraph(crs, ((ScenarioImpl) this.scenario)
				.getTransitSchedule());
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	@Override
	public void writeShapeFile(final String ShapeFilename) {
		try {
			ShapeFileWriter2.writeGeometries(p2g.getFeatures(), ShapeFilename);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../berlin-bvg09/pt/nullfall_alles/network.xml";
		String scheduleFilename = "../berlin-bvg09/pt/nullfall_alles/transitSchedule.xml";

		PtRoute2QGIS pr2q = new PtRoute2QGIS(netFilename, gk4, scheduleFilename);
		pr2q.writeShapeFile("../berlin-bvg09/pt/nullfall_alles/schedule.shp");
	}
}
