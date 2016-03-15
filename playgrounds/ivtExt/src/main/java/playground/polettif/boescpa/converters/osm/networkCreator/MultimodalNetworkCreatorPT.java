/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.converters.osm.networkCreator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.polettif.boescpa.converters.osm.networkCreator.osmWithPT.OsmNetworkReaderWithPT;

/**
 * A version of MultimodalNetworkCreator that retains the pt tags in the mode tag of the network.
 *
 * @author boescpa
 */
public class MultimodalNetworkCreatorPT extends MultimodalNetworkCreator {

	public MultimodalNetworkCreatorPT(Network network) {
		super(network);
	}

	@Override
	public void createMultimodalNetwork(String osmFile) {
		OsmNetworkReaderWithPT osmReader =
				new OsmNetworkReaderWithPT(this.network, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"), true);

		osmReader.setKeepPaths(false);

		/*// Set street-defaults (and with it the filter...)
		osmReader.setHighwayDefaults("motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults("motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		osmReader.setHighwayDefaults("trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults("trunk_link",    1,  50.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults("primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults("primary_link",  1,  60.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults("secondary",     1,  60.0/3.6, 1.0, 1000);
		osmReader.setHighwayDefaults("tertiary",      1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults("minor",         1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults("unclassified",  1,  45.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults("residential",   1,  30.0/3.6, 1.0,  600);
		osmReader.setHighwayDefaults("living_street", 1,  15.0/3.6, 1.0,  300);
		// Set railway-defaults (and with it the filter...)
		osmReader.setRailwayDefaults("rail", 		  1, 120.0/3.6, 1.0,  100);
		osmReader.setRailwayDefaults("tram", 		  1,  80.0/3.6, 1.0,  100, true);
		osmReader.setRailwayDefaults("funicular",	  1,  40.0/3.6, 1.0,  100);
		osmReader.setRailwayDefaults("light_rail",	  1,  80.0/3.6, 1.0,  100);*/

		osmReader.parse(osmFile);

		// TODO-boescpa NetworkCleaner interferes with the network creation!
		//new NetworkCleaner().run(this.network);
	}
}
