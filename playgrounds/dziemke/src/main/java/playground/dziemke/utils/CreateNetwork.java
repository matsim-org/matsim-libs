package playground.dziemke.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class CreateNetwork {

	public static void main(String[] args) {
		// input and output
//		String osmFile = "/Users/dominik/Accessibility/Data/OSM/2015-10-15_nairobi.osm.xml";
		String osmFile = "/Users/dominik/Accessibility/Data/OSM/2015-11-05_kibera.osm.xml";
		
//		String networkFile = "/Users/dominik/Accessibility/Data/Networks/Kenya/2015-10-15_nairobi_paths.xml";
		String networkFile = "/Users/dominik/Accessibility/Data/Networks/Kenya/2015-11-05_kibera_paths_detailed.xml";
		
		
		// parameters
		String inputCRS = "EPSG:4326";
		String outputCRS = "EPSG:21037";
		// EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin
		// EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
//		boolean includeLowHierarchyWays = false;
		boolean includeLowHierarchyWays = true;
//		boolean keepPaths = false;
		boolean keepPaths = true;
		boolean writeNetwortFile = true;

		createNetwork(osmFile, networkFile, inputCRS, outputCRS, includeLowHierarchyWays, keepPaths, writeNetwortFile);
	}

	public static Network createNetwork(String osmFile, String networkFile, String inputCRS, String outputCRS,
			boolean includeLowHierarchyWays, boolean keepPaths, boolean writeNetworkFile) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation);
			
		// keeping the path makes the file (for the Nairobi case) three times as big (22.4MB vs. 8.7MB)
		if (keepPaths == true) {
			osmNetworkReader.setKeepPaths(true);
		}
		
		// this block is for the low hierarchy roads
		if (includeLowHierarchyWays == true) {
			// defaults already set for motorway, motorway_link, trunk, trunk_link, primary,
			// primary_link, secondary, tertiary, minor, unclassified, residential, living_street
			// minor does not seem to exist on the website anymore
			//
			// other types in osm, see: http://wiki.openstreetmap.org/wiki/Key:highway
			// secondary_link, tertiary_link, pedestrian, track, bus_guideway, raceway, road,
			// footway, bridleway, steps, path
			//
			// onr.setHighwayDefaults(hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour);
			//
			osmNetworkReader.setHighwayDefaults(4, "secondary_link", 1, 60.0/3.6, 1.0, 1000); // same values as "secondary"
			osmNetworkReader.setHighwayDefaults(5, "tertiary_link", 1, 45.0/3.6, 1.0,  600); // same values as "tertiary"
			//
			// lowest hierarchy contained in defaults: 6, "living_street", 1,  15.0/3.6, 1.0,  300);
			//
			osmNetworkReader.setHighwayDefaults(7, "pedestrian", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "track", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "road", 1, 15/3.6, 1.0, 300); // like "living_street"
			osmNetworkReader.setHighwayDefaults(7, "footway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "bridleway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "steps", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "path", 1, 15/3.6, 1.0, 0);
		}
		// end of low hierarchy roads block
		

		osmNetworkReader.parse(osmFile); 
		new NetworkCleaner().run(network);
		
		if (writeNetworkFile == true) {
			new NetworkWriter(network).write(networkFile);
		}
		
		return network;
	}
}