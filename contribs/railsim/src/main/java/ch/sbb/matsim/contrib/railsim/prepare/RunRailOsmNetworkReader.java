package ch.sbb.matsim.contrib.railsim.prepare;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.contrib.osm.networkReader.OsmRailwayReader;
import org.matsim.contrib.osm.networkReader.OsmTags;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class RunRailOsmNetworkReader {

	private static final String inputFile = "path/to/file.osm.pbf";
	private static final String outputFile = "path/to/network.xml.gz";

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:2056");

	public static void main(String[] args) {

		ConcurrentMap<String, LinkProperties> linkProperties = new ConcurrentHashMap<>();
		linkProperties.put(OsmTags.RAIL, new LinkProperties(1, 1, 30., 1000., false));
		linkProperties.put(OsmTags.NARROW_GAUGE, new LinkProperties(2, 1, 30., 1000., false));
		linkProperties.put(OsmTags.LIGHT_RAIL, new LinkProperties(3, 1, 30., 1000., false));
		linkProperties.put(OsmTags.SUBWAY, new LinkProperties(4, 1, 30., 1000., false));
		linkProperties.put(OsmTags.MONORAIL, new LinkProperties(5, 1, 30., 1000., false));
		
		Network network = new OsmRailwayReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setLinkProperties(linkProperties)
				.setPreserveNodeWithId(id -> true) // this filter keeps the detailed geometries
				.build()
				.read(inputFile);
		
		network.getAttributes().putAttribute("data_origin", "OSM");
		
		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(outputFile);
	}
}
