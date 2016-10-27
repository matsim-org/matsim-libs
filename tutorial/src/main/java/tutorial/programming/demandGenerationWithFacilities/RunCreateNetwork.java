package tutorial.programming.demandGenerationWithFacilities;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class RunCreateNetwork {

	private static final String osm = "examples/tutorial/programming/demandGenerationWithFacilities/zrh-center-bigroads.osm.gz";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);
		OsmNetworkReader onr = new OsmNetworkReader(net,ct);
		onr.parse(osm); 
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write("./output/network.xml");
	}
}


