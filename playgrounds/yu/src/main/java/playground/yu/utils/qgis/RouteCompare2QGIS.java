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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.RouteSummaryTest.RouteSummary;

import com.vividsolutions.jts.geom.Coordinate;

public class RouteCompare2QGIS extends Route2QGIS {
	private final static Logger log = Logger.getLogger(RouteCompare2QGIS.class);
	private final Map<List<Id>, Integer> routeCountersB;
	private PolygonFeatureFactory factory;

	public RouteCompare2QGIS(Population population,
			final CoordinateReferenceSystem crs, final String outputDir,
			final Network network,
			final Map<List<Id>, Integer> routeCountersA,
			final Map<List<Id>, Integer> routeCountersB) {
		super(population, crs, outputDir, network, routeCountersA);
		this.routeCountersB = routeCountersB;
	}

	@Override
	protected void initFeatureType() {
		this.factory = new PolygonFeatureFactory.Builder().
				setCrs(this.getCrs()).
				setName("route").
				addAttribute("DIFF_B-A", Double.class).
				addAttribute("DIFF_SIGN", Double.class).
				addAttribute("ROUTEFLOWA", Double.class).
				addAttribute("ROUTEFLOWB", Double.class).
				addAttribute("(B-A)/A", Double.class).
				create();
	}

	@Override
	protected SimpleFeature getRouteFeature(final List<Id> routeLinkIds) {
		Integer routeFlowsA = routeCounters.get(routeLinkIds);
		Integer routeFlowsB = routeCountersB.get(routeLinkIds);
		if (routeFlowsA != null || routeFlowsB != null) {
			if (routeFlowsA == null) {
				routeFlowsA = 0;
			}
			if (routeFlowsB == null) {
				routeFlowsB = 0;
			}
			if ((routeFlowsA.intValue() > 1 || routeFlowsB.intValue() > 1)
					&& routeFlowsA.intValue() != routeFlowsB.intValue()) {
				Coordinate[] coordinates = new Coordinate[(routeLinkIds.size() + 1) * 2 + 1];
				Double diff = routeFlowsB.doubleValue() - routeFlowsA.doubleValue();
				Double absDiff = Math.abs(diff);
				double width = 100.0 * Math.min(250.0, routeFlowsA.intValue() == 0 ? 10.0 : absDiff
								/ routeFlowsA.doubleValue());
				coordinates = calculateCoordinates(coordinates, width, routeLinkIds);
				return factory.createPolygon(
						coordinates,
						new Object[] {absDiff,
								routeFlowsA.intValue() == 0 ? 0 : diff / absDiff,
								routeFlowsA,
								routeFlowsB,
								routeFlowsA.intValue() == 0 ? 10 : absDiff / routeFlowsA.doubleValue() },
						null);
			}
		}
		return null;
	}

	@Override
	protected void writeRoutes() {
		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		Set<List<Id>> totalKeys = new HashSet<List<Id>>();
		totalKeys.addAll(routeCounters.keySet());
		totalKeys.addAll(routeCountersB.keySet());
		for (List<Id> routeLinkIds : totalKeys) {
			SimpleFeature ft = getRouteFeature(routeLinkIds);
			if (ft != null) {
				fts.add(ft);
			}
		}
		ShapeFileWriter.writeGeometries(fts, getOutputDir() + "/routes.shp");
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String networkFilename = args[0];
		final String populationFilenameA = args[1];
		final String populationFilenameB = args[2];
		final String outputDir = args[3];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		// ------------------------RouteSummaryA--------------------------------
		ScenarioImpl scenarioA = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenarioA.setNetwork(network);
		Population populationA = scenarioA.getPopulation();

		RouteSummary rsA = new RouteSummary(outputDir + "/routeCompareA.txt.gz");

		System.out.println("-->reading plansfile: " + populationFilenameA);
		new MatsimPopulationReader(scenarioA).readFile(populationFilenameA);

		rsA.run(populationA);
		rsA.write();
		rsA.end();
		// ------------------------RouteSummaryB---------------------------------
		ScenarioImpl scenarioB = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenarioB.setNetwork(network);
		Population populationB = scenarioB.getPopulation();

		RouteSummary rsB = new RouteSummary(outputDir + "/routeCompareB.txt.gz");

		System.out.println("-->reading plansfile: " + populationFilenameB);
		new MatsimPopulationReader(scenarioB).readFile(populationFilenameB);

		rsB.run(populationB);
		rsB.write();
		rsB.end();
		// ----------------------------------------------------------------------
		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(ch1903);
			RouteCompare2QGIS r2q = new RouteCompare2QGIS(null, crs, outputDir,
					network, rsA.getRouteCounters(), rsB.getRouteCounters());
			r2q.setWriteActs(false);
			r2q.setWriteLegs(false);
			r2q.setWriteRoutes(true);
			r2q.write();
		} catch (FactoryException e1) {
			e1.printStackTrace();
		}
	}

}
