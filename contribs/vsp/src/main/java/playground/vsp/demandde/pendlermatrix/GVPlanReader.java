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

package playground.vsp.demandde.pendlermatrix;

import java.util.Collection;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;

import playground.vsp.pipeline.PopulationReaderTask;
import playground.vsp.pipeline.PopulationWriterTask;

public class GVPlanReader {


	private static final String GV_NETWORK_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/network_cleaned_wgs84.xml.gz";

	private static final String NETWORK_FILENAME = "/Users/michaelzilske/osm/motorway_germany.xml";

	private static final String GV_PLANS = "/Users/michaelzilske/workspace/run1061/1061.output_plans.xml.gz";

	private static final String FILTER_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/filter.shp";

	private static final String LANDKREISE = "/Users/michaelzilske/workspace/prognose_2025/osm_zellen/landkreise.shp";

	private static boolean isCoordInShape(Coord linkCoord, Collection<SimpleFeature> features, GeometryFactory factory) {
		boolean found = false;
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (SimpleFeature ft : features) {
			if (((Geometry) ft.getDefaultGeometry()).contains(geo)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static void main(String[] args) {
		Scenario gvNetwork = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(gvNetwork.getNetwork()).readFile(GV_NETWORK_FILENAME);
		Scenario osmNetwork = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osmNetwork.getNetwork()).readFile(NETWORK_FILENAME);
		Collection<SimpleFeature> featuresInShape;
		featuresInShape = new GeoFileReader().readFileAndInitialize(FILTER_FILENAME);

		PopulationReaderTask populationReaderTask = new PopulationReaderTask(GV_PLANS, gvNetwork.getNetwork());

		PersonDereferencerTask personDereferencerTask = new PersonDereferencerTask();

		PersonGeoTransformatorTask personGeoTransformatorTask = new PersonGeoTransformatorTask(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);

		PersonRouterFilter personRouterFilter = new PersonRouterFilter(osmNetwork.getNetwork());
		GeometryFactory factory = new GeometryFactory();
		for (Node node : osmNetwork.getNetwork().getNodes().values()) {
			if (isCoordInShape(node.getCoord(), featuresInShape, factory)) {
				personRouterFilter.getInterestingNodeIds().add(node.getId());
			}
		}

		PersonVerschmiererTask personVerschmiererTask = new PersonVerschmiererTask(LANDKREISE);

		PopulationWriterTask populationWriterTask = new PopulationWriterTask("/Users/michaelzilske/workspace/prognose_2025/demand/naechster_versuch_gv.xml", gvNetwork.getNetwork());

		populationReaderTask.setSink(personDereferencerTask);
		personDereferencerTask.setSink(personGeoTransformatorTask);
		personGeoTransformatorTask.setSink(personRouterFilter);
		personRouterFilter.setSink(personVerschmiererTask);
		personVerschmiererTask.setSink(populationWriterTask);

		populationReaderTask.run();
	}

}
