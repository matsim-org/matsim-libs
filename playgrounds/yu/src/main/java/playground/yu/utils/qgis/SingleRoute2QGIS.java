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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.DiverseRoutesSummary;
import playground.yu.analysis.DiverseRoutesSummary.LegRoute;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape of Mr.
 * Laemmeland can convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 * NO, it's not (at least no longer) a copy!
 * 
 */
public class SingleRoute2QGIS extends SelectedPlans2ESRIShapeChanged implements
		X2QGIS {
	private final static Logger log = Logger.getLogger(SingleRoute2QGIS.class);
	protected Map<String, List<LegRoute>> dailyRoutes;
	protected Network network;
	private boolean writeRoutes = true;
	private PolylineFeatureFactory factory;

	public SingleRoute2QGIS(Population population,
			final CoordinateReferenceSystem crs, final String outputDir,
			final Network network, final Map<String, List<LegRoute>> map) {
		super(population, network, crs, outputDir);
		this.network = network;
		dailyRoutes = map;
	}

	@Override
	protected void initFeatureType() {
		this.factory = new PolylineFeatureFactory.Builder().
				setCrs(getCrs()).
				setName("route").
				addAttribute("PERSON_ID", String.class).
				addAttribute("PLAN_INDEX", Integer.class).
				create();
	}

	protected SimpleFeature getRouteFeature(String personLegId, LegRoute dailyRoutes) {
		List<Id> routeLinkIds = dailyRoutes.getRouteLinkIds();
		Coordinate[] coordinates = new Coordinate[routeLinkIds.size() + 1];

		coordinates = calculateCoordinates(coordinates, routeLinkIds);
		return this.factory.createPolyline(
				coordinates,
				new Object[] {personLegId.toString(), dailyRoutes.getPlanIndex()},
				null);
	}

	protected Coordinate[] calculateCoordinates(Coordinate[] coordinates,
			List<Id> routeLinkIds) {
		for (int i = 0; i < routeLinkIds.size(); i++) {
			Link l = network.getLinks().get(routeLinkIds.get(i));
			coordinates[i] = MGC.coord2Coordinate(l.getFromNode().getCoord());
		}

		coordinates[routeLinkIds.size()] = MGC.coord2Coordinate(network
				.getLinks().get(routeLinkIds.get(routeLinkIds.size() - 1))
				.getToNode().getCoord());

		return coordinates;
	}

	protected void writeRoutes() {

		for (Entry<String, List<LegRoute>> personDailyRoutes : dailyRoutes.entrySet()) {
			ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();

			String personLegId = personDailyRoutes.getKey();
			for (LegRoute legRoute : personDailyRoutes.getValue()) {
				SimpleFeature ft = getRouteFeature(personLegId, legRoute);
				if (ft != null) {
					fts.add(ft);
				}
			}
			ShapeFileWriter.writeGeometries(fts, getOutputDir() + "_"
					+ personLegId + "_routes.shp");
		}

	}

	@Override
	public void write() {
		if (writeRoutes) {
			writeRoutes();
		}
	}

	public static void runAllRoutes(final String[] args) {
		final String networkFilename, populationFilename, outputDir;
		final double sample;
		if (args.length >= 4) {
			networkFilename = args[0];
			populationFilename = args[1];
			outputDir = args[2];
			sample = Double.parseDouble(args[3]);
		} else {
			networkFilename = "../matsimTests/ParamCalibration/network.xml";
			populationFilename = "../matsimTests/ParamCalibration/40.plans.xml.gz";
			outputDir = "../matsimTests/dailyJourney_Route2QGIS/";
			sample = 0.1;
			// networkFilename =
			// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
			// populationFilename =
			// "../matsimTests/dailyJourney_Route2QGIS/output_plans.xml.gz";
			// outputDir = "../matsimTests/dailyJourney_Route2QGIS/Berlin";
			// sample = 0.1;
		}
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();

		DiverseRoutesSummary drs = new DiverseRoutesSummary(sample, outputDir
				+ "DiverseRoute.txt");

		System.out.println("-->reading plansfile: " + populationFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		drs.run(population);
		drs.write();

		CoordinateReferenceSystem crs;
		crs = MGC.getCRS(gk4);
		SingleRoute2QGIS r2q = new SingleRoute2QGIS(population, crs,
				outputDir, network, drs.getDailyRouteList());
		r2q.setOutputSample(sample);
		r2q.setWriteActs(false);
		r2q.setWriteRoutes(true);
		r2q.write();
	}

	protected void setWriteRoutes(final boolean writeRoutes) {
		this.writeRoutes = writeRoutes;
	}

	public static void main(final String[] args) {
		runAllRoutes(args);
	}
}
