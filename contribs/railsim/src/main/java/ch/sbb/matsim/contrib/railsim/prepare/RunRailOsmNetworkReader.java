package ch.sbb.matsim.contrib.railsim.prepare;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.OsmRailwayReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class RunRailOsmNetworkReader {

	private static final String baseDirectory = "path/to/directory/";
	
	private static final String inputFile = baseDirectory + "switzerland-latest.osm.pbf";
	private static final String outputFile1 = baseDirectory + "switzerland_network.xml.gz";
	private static final String outputFile2 = baseDirectory + "switzerland_network_cleaned.xml.gz";

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:2056");

	public static void main(String[] args) {
		
		Network network = new OsmRailwayReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setPreserveNodeWithId(id -> true) // this filter keeps the detailed geometries, only required for cosmetic reasons
				.build()
				.read(inputFile);
		
		network.getAttributes().putAttribute("data_origin", "OSM");
		
		new NetworkWriter(network).write(outputFile1);
		
		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(outputFile2);
	}
}
