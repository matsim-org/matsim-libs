/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.yu.utils.qgis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.RouteSummaryTest.RouteSummary;

import com.vividsolutions.jts.geom.Coordinate;

public class Route2QGIS extends SelectedPlans2ESRIShapeChanged implements X2QGIS {
	private final static Logger log = Logger.getLogger(Route2QGIS.class);
	protected Map<List<Id>, Integer> routeCounters;
	protected Network network;
	private boolean writeRoutes = true;
	private PolygonFeatureFactory factory;

	public Route2QGIS(Population population,
			final CoordinateReferenceSystem crs, final String outputDir,
			final Network network,
			final Map<List<Id>, Integer> routeCounters) {
		super(population, network, crs, outputDir);
		this.network = network;
		this.routeCounters = routeCounters;
	}

	@Override
	protected void initFeatureType() {
		this.factory = new PolygonFeatureFactory.Builder().
				setCrs(this.getCrs()).
				setName("route").
				addAttribute("ROUTE_FLOW", Double.class).
				create();
	}

	protected SimpleFeature getRouteFeature(final List<Id> routeLinkIds) {
		Integer routeFlows = routeCounters.get(routeLinkIds);
		if (routeFlows != null) {
			if (routeFlows.intValue() > 1) {
				Coordinate[] coordinates = new Coordinate[(routeLinkIds.size() + 1) * 2 + 1];
				double width = 5.0 * Math.min(250.0, routeFlows.doubleValue());
				coordinates = calculateCoordinates(coordinates, width,
						routeLinkIds);
				return this.factory.createPolygon(
						coordinates,
						new Object[] { routeFlows.doubleValue() },
						null
						);
			}
		}
		return null;
	}

	protected Coordinate[] calculateCoordinates(Coordinate[] coordinates,
			double width, List<Id> routeLinkIds) {
		for (int i = 0; i < routeLinkIds.size(); i++) {
			Link l = network.getLinks().get(routeLinkIds.get(i));
			Coord c = l.getFromNode().getCoord();
			Coordinate cdn = new Coordinate(c.getX(), c.getY());
			coordinates[i] = cdn;
			Coord toCoord = l.getToNode().getCoord();
			Coordinate to = new Coordinate(toCoord.getX(), toCoord.getY());
			double xdiff = to.x - cdn.x;
			double ydiff = to.y - cdn.y;
			double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
			coordinates[coordinates.length - 2 - i] = new Coordinate(cdn.x
					+ width * ydiff / denominator, cdn.y - width * xdiff
					/ denominator);
		}

		Coord c = network.getLinks().get(
				routeLinkIds.get(routeLinkIds.size() - 1)).getToNode()
				.getCoord();
		Coordinate cdn = new Coordinate(c.getX(), c.getY());
		coordinates[routeLinkIds.size()] = cdn;
		Coordinate from = coordinates[routeLinkIds.size() - 1];
		double xdiff = cdn.x - from.x;
		double ydiff = cdn.y - from.y;
		double denominator = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		coordinates[routeLinkIds.size() + 1] = new Coordinate(from.x + width
				* ydiff / denominator, from.y - width * xdiff / denominator);
		coordinates[coordinates.length - 1] = coordinates[0];

		return coordinates;
	}

	protected void writeRoutes() {
		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (List<Id> routeLinkIds : routeCounters.keySet()) {
			SimpleFeature ft = getRouteFeature(routeLinkIds);
			if (ft != null) {
				fts.add(ft);
			}
		}
		ShapeFileWriter.writeGeometries(fts, getOutputDir() + "/routes.shp");
	}

	@Override
	public void write() {
		if (writeRoutes) {
			writeRoutes();
		}
	}

	public static void runSelectedRoutes(final String[] args) {
		final String networkFilename = args[0];
		final String populationFilename = args[1];
		final String outputDir = args[2];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();

		RouteSummary rs = new RouteSummary(outputDir + "/routeCompare.txt.gz");

		System.out.println("-->reading plansfile: " + populationFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		rs.run(population);
		rs.write();
		rs.end();

		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(ch1903);
			Route2QGIS r2q = new Route2QGIS(population, crs, outputDir,
					network, rs.getRouteCounters());
			r2q.setOutputSample(// 0.05
					1);
			r2q.setWriteActs(false);
			r2q.setWriteRoutes(true);
			r2q.write();
		} catch (FactoryException e1) {
			e1.printStackTrace();
		}
	}

	protected void setWriteRoutes(final boolean writeRoutes) {
		this.writeRoutes = writeRoutes;
	}

	public static void main(final String[] args) {
		runSelectedRoutes(args);
	}
}
