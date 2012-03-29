/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.osm;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;

import playground.andreas.utils.net.NetworkSimplifier;

/**
 * @author droeder
 *
 */
public class Osm2Matsim {
	private static final String PATH = "C:/Users/Daniel/Desktop/network/";
	private static final String INFILE = PATH + "belgium_incl_borderArea.osm";
	private static final String OUTFILE = PATH + "belgium_incl_borderArea.xml.gz";
	
//	private static final String CRS = " GEOGCS[\"Belge 1972\", DATUM[\"Reseau_National_Belge_1972\", SPHEROID[\"International 1924\",6378388,297, AUTHORITY[\"EPSG\",\"7022\"]], TOWGS84[106.869,-52.2978,103.724,-0.33657,0.456955,-1.84218,1], AUTHORITY[\"EPSG\",\"6313\"]], PRIMEM[\"Greenwich\",0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\",0.01745329251994328, AUTHORITY[\"EPSG\",\"9122\"]], AUTHORITY[\"EPSG\",\"4313\"]], UNIT[\"metre\",1, AUTHORITY[\"EPSG\",\"9001\"]], PROJECTION[\"Lambert_Conformal_Conic_2SP_Belgium)\"], PARAMETER[\"standard_parallel_1\",49.83333333333334], PARAMETER[\"standard_parallel_2\",51.16666666666666], PARAMETER[\"latitude_of_origin\",90], PARAMETER[\"central_meridian\",4.356939722222222], PARAMETER[\"false_easting\",150000.01256], PARAMETER[\"false_northing\",5400088.4378], AUTHORITY[\"EPSG\",\"31300\"], AXIS[\"X\",EAST], AXIS[\"Y\",NORTH]]";

	public static void main(final String[] args) {

		NetworkImpl network = NetworkImpl.createNetwork();
//		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:31300");
//      String transformation = crs.getName().toString();
//		OsmNetworkReader osmReader = new OsmNetworkReader(network,
//				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
//						transformation), true);
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.DHDN_GK4), true);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		
		osmReader.setHierarchyLayer(51.671, 2.177, 49.402, 6.764, 2); //belgium and bordering areas
		osmReader.setHierarchyLayer(51.328, 3.639, 50.645, 4.888, 4); //greater brussel area
		osmReader.setHierarchyLayer(50.9515, 4.1748, 50.7312, 4.5909, 5); //city of brussel
		
		osmReader.parse(INFILE);
		new NetworkWriter(network).write(OUTFILE);
		new NetworkCleaner().run(OUTFILE, OUTFILE.split(".xml")[0] + "_clean.xml.gz");
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(OUTFILE.split(".xml")[0] + "_clean.xml.gz");
		network = (NetworkImpl) scenario.getNetwork();
		
		NetworkSimplifier simpl = new NetworkSimplifier();
		Set<Integer> nodes2merge = new HashSet<Integer>();
		nodes2merge.add(new Integer(4));
		nodes2merge.add(new Integer(5));
		simpl.setNodesToMerge(nodes2merge);
		simpl.run(network);
		new NetworkWriter(network).write(OUTFILE.split(".xml")[0] + "_clean_simple.xml.gz");
		
		OTFVis.playNetwork(OUTFILE.split(".xml")[0] + "_clean_simple.xml.gz");
	}

}
