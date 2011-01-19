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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.analysis.DiverseRoutesSummary;
import playground.yu.analysis.DiverseRoutesSummary.LegRoute;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape of Mr.
 * Laemmeland can convert a MATSim-population to a QGIS .shp-file (acts or legs)
 * 
 * @author ychen
 * 
 */
public class SingleRoute2QGIS extends SelectedPlans2ESRIShapeChanged implements
		X2QGIS {
	private final static Logger log = Logger.getLogger(SingleRoute2QGIS.class);
	protected Map<Id, List<LegRoute>> dailyRoutes;
	protected NetworkImpl network;
	private FeatureType featureTypeRoute;
	private boolean writeRoutes = true;

	public SingleRoute2QGIS(Population population,
			final CoordinateReferenceSystem crs, final String outputDir,
			final NetworkImpl network, final Map<Id, List<LegRoute>> dailyRoutes) {
		super(population, network, crs, outputDir);
		this.network = network;
		this.dailyRoutes = dailyRoutes;
	}

	@Override
	protected void initFeatureType() {
		AttributeType[] attrRoute = new AttributeType[4];
		attrRoute[0] = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, getCrs());
		attrRoute[1] = AttributeTypeFactory.newAttributeType("PERSON_ID",
				String.class);
		attrRoute[2] = AttributeTypeFactory.newAttributeType("PLAN_INDEX",
				Integer.class);
		attrRoute[3] = AttributeTypeFactory.newAttributeType("LEG_INDEX",
				Integer.class);
		try {
			setFeatureTypeRoute(FeatureTypeBuilder.newFeatureType(attrRoute,
					"route"));
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	public void setFeatureTypeRoute(final FeatureType featureTypeRoute) {
		this.featureTypeRoute = featureTypeRoute;
	}

	protected Feature getRouteFeature(Id personId, LegRoute dailyRoutes) {
		List<Id> routeLinkIds = dailyRoutes.getRouteLinkIds();
		Coordinate[] coordinates = new Coordinate[routeLinkIds.size() + 1];

		coordinates = calculateCoordinates(coordinates, routeLinkIds);
		try {
			return getFeatureTypeRoute().create(
					new Object[] {
							new LineString(new CoordinateArraySequence(
									coordinates), getGeofac()),
							personId.toString()/* 1. element */,
							dailyRoutes.getPlanIndex()/* 2. element */,
							dailyRoutes.getLegIndex() /* 3. element */});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		return null;
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

	protected FeatureType getFeatureTypeRoute() {
		return featureTypeRoute;
	}

	protected void writeRoutes() throws IOException {

		for (Entry<Id, List<LegRoute>> personDailyRoutes : dailyRoutes
				.entrySet()) {
			ArrayList<Feature> fts = new ArrayList<Feature>();

			Id personId = personDailyRoutes.getKey();
			for (LegRoute legRoute : personDailyRoutes.getValue()) {
				Feature ft = getRouteFeature(personId, legRoute);
				if (ft != null) {
					fts.add(ft);
				}
			}
			ShapeFileWriter.writeGeometries(fts, getOutputDir() + "_"
					+ personId + "_routes.shp");// TODO for one person?
		}

	}

	@Override
	public void write() throws IOException {
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
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();

		DiverseRoutesSummary drs = new DiverseRoutesSummary(sample, outputDir
				+ "DiverseRoute.txt");

		System.out.println("-->reading plansfile: " + populationFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		drs.run(population);
		drs.write();

		CoordinateReferenceSystem crs;
		try {
			crs = MGC.getCRS(gk4);
			SingleRoute2QGIS r2q = new SingleRoute2QGIS(population, crs,
					outputDir, network, drs.getDailyRouteList());
			r2q.setOutputSample(sample);
			r2q.setWriteActs(false);
			r2q.setWriteRoutes(true);
			r2q.write();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	protected void setWriteRoutes(final boolean writeRoutes) {
		this.writeRoutes = writeRoutes;
	}

	public static void main(final String[] args) {
		runAllRoutes(args);
	}
}
