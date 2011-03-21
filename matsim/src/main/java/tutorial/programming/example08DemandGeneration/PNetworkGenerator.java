package tutorial.programming.example08DemandGeneration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;


public class PNetworkGenerator {
	
	public static void main(String[] args) {
		String osm = "./input/my-map.osm";
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = 
		 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		OsmNetworkReader onr = new OsmNetworkReader(net,ct);
		onr.parse(osm); 
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write("./input/network.xml");
	}

}
