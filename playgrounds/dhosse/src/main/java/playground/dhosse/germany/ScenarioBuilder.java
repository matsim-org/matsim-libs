package playground.dhosse.germany;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class ScenarioBuilder {
	
	public static void main(String args[]){

		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32632");
		
		OsmNetworkReader onr = new OsmNetworkReader(network, ct);
		onr.parse("C:/Users/Daniel/Documents/work/germany/raw_data/osm/hamburg-highways.osm");
		
		new NetworkCleaner().run(network);
		
		new NetworkWriter(network).write("C:/Users/Daniel/Documents/work/germany/matsim/hamburg-roads.xml");
		
	}

}