package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
//import org.matsim.contrib.osm.networkReader.OsmNetworkParser;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//import org.matsim.core.utils.io.OsmNetworkReader;

public class RunSimpleNetworkReader {

//	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/sample_illmensee.osm";
	private static final String inputFile = "C://Users/metz_so/IdeaProjects/data/saarland-260122.osm.pbf";
	private static final String outputFile = "C://Users/metz_so/IdeaProjects/data/matsim-network2.xml.gz";
	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	public static void main(String[] args) {

		//Network network = new SupersonicOsmNetworkReader.Builder()
		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.build()
				.read(inputFile);

		new NetworkWriter(network).write(outputFile);
	}

}
