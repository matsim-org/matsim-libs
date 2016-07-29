package playground.dziemke.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class CreateNetworkMaryland {
	final private static Logger log = Logger.getLogger(CreateNetworkMaryland.class);

	public static void main(String[] args) {
		// Input and output
		String osmFile = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/mstm_run/input_additional/osm/md_and_surroundings.osm";
		String outputBase = "../../../../SVN/shared-svn/projects/tum-with-moeckel/data/mstm_run/input_additional/network_05";
		String networkFile = outputBase + "/network.xml";
		
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		
		// Parameters
		String inputCRS = "EPSG:4326";
		String outputCRS = "EPSG:26918";
//		String outputCRS = TransformationFactory.WGS84_SA_Albers;
		// EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin; DE
		// EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, KE
		// EPSG:26918 = NAD83 / UTM zone 18N, for Maryland, US
		log.info("Input CRS is " + inputCRS + "; output CRS is " + outputBase);

		
		// Infrastructure
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		
		// "useHighwayDefaults" needs to be set to false to be able to set own values below.
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);
		
		NetworkWriter networkWriter = new NetworkWriter(network);


		// Set values
		osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 100.0/3.6, 1.0, 2000, true); // 100 instead of 120
		osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
		osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
		osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
		osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
		// minor, unclassified, residential, living_street" are left out here, whereas they are used by defaults.
			
		// additional to defaults
		osmNetworkReader.setHighwayDefaults(4, "secondary_link", 1, 60.0/3.6, 1.0, 1000); // same values as "secondary"
		osmNetworkReader.setHighwayDefaults(5, "tertiary_link", 1, 45.0/3.6, 1.0,  600); // same values as "tertiary"

		
		// Read
		osmNetworkReader.parse(osmFile); 
		new NetworkCleaner().run(network);
		networkWriter.write(networkFile);
	}
}