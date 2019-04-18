package org.matsim.codeexamples.network;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RunCreateNetworkFromOSM {

	private static String GK4asEpsg = "EPSG:31468";
	private static Path input = Paths.get("path-to-osm-xml");

	public static void main(String[] args) {
		new RunCreateNetworkFromOSM().create();
	}

	private void create() {

		Network network = NetworkUtils.createNetwork();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, GK4asEpsg
		);
		OsmNetworkReader reader = new OsmNetworkReader(network, transformation, true, true);
		reader.addOsmFilter((coord, hierarchyLevel) -> {
			// hierachy levels 1 - 3 are motorways and primary roads, as well as their trunks
			return hierarchyLevel <= 4;
		});
		reader.parse(input.toString());

		new NetworkWriter(network).write("output path");
	}
}
