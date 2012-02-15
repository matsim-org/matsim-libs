package playground.tnicolai.matsim4opus.utils.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.config.ConfigUtils;

/**
 * converts a OpenStreetMap network to the MATSim format
 * @author thomas
 *
 */
public class OSM2MATSimNetwork {

	public static void main(String[] args) {
		String osm = "/Users/thomas/Downloads/merged-network.osm";
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.CH1903_LV03);
		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
		onr.parse(osm);
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write("/Users/thomas/Downloads/merged-network.xml");
	}

}
