package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Arrays;
import java.util.HashSet;

/**
 * 1. It is possible to apply a link filter. The below example includes all links which are of hierarchy level "motorway".
 * For simplicity filtering for a coordinate is skipped but this is the place to test whether a coordinate is within a
 * certain area. This method is called for the from- and to nodes of a link.
 * 2. The reader generally tries to simplify the network as much as possible. If it is necessary to preserve certain nodes
 * for e.g. implementing counts it is possible to omit the simplification for certain node ids. The below example
 * prevents the reader to remove the node with id: 2.
 * 3. It is possible to override the default properties wich are assigned to a link of a certain hierarchy level. E.g. one could change the freespeed of highways by adding a new LinkProperties object for the 'highway' tag. The example below adds LinkProperties for residential links, which are otherwise ignored.
 * 4. After creating a link the reader will call the 'afterLinkCreated' hook with the newly created link, the original osm
 * tags, and a flag whether it is the forward or reverse direction of an osm-way. The below example sets the allowed
 * transport mode on all links to 'car' and 'bike'.
 */
public class RunFullyConfiguredNetworkReader {

	private static final String inputFile = "/path/to/your/file.osm.pbf";
	private static final String outputFile = "/path/to/your/matsim-network.xml.gz";
	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

	public static void main(String[] args) {

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setIncludeLinkAtCoordWithHierarchy((coord, hierachyLevel) -> hierachyLevel == LinkProperties.LEVEL_MOTORWAY)
				.setPreserveNodeWithId(id -> id == 2)
				.addOverridingLinkProperties("residential", new LinkProperties(9, 1, 30.0 / 3.6, 1500, false))
				.setAfterLinkCreated((link, osmTags, isReverse) -> link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike))))
				.build()
				.read(inputFile);

		new NetworkWriter(network).write(outputFile);
	}
}
