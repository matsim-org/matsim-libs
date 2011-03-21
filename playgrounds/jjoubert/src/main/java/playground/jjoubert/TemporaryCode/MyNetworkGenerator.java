/* *********************************************************************** *
 * project: org.matsim.*
 * MyNetworkConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.TemporaryCode;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;

public class MyNetworkGenerator {
	public static final String UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";


	public static void main(String [] args) {
		String osm = "/Users/johanwjoubert/Downloads/map.osm";

		Network net = NetworkImpl.createNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, UTM35S);

		OsmNetworkReader onr = new OsmNetworkReader(net,ct);
		onr.setHierarchyLayer(-25.8875, 28.1204, -25.9366, 28.1612, 6);
		onr.parse(osm);
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write("/Users/johanwjoubert/Desktop/Temp/network.xml");

		Config c = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getConfig();
		c.global().setCoordinateSystem(UTM35S);

		// TODO Sort out these errors: Cannot instatiate FeatureGeneratorBuilder.

//		FeatureGeneratorBuilder fgb = new FeatureGeneratorBuilderImpl(net, UTM35S);
//		FeatureGenerator builder = fgb.createFeatureGenerator();
//
//		builder.setWidthCoefficient(0.01);
//		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
//		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
//		new Links2ESRIShape(net,"/Users/johanwjoubert/Desktop/Temp/network.shp", builder).write();

	}

}