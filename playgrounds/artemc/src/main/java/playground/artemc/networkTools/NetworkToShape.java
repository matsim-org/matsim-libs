package playground.artemc.networkTools;

import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

public class NetworkToShape {

	/**
	 * @param args
	 */

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage:");
			System.exit(0);
		}

		String networkPath = args[0];
		String outputPath = args[1];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new NetworkReaderMatsimV1(scenario.getNetwork()).parse(networkPath);
		Network net = scenario.getNetwork();

		Map<Id<Node>, ? extends Node> nodes = net.getNodes();

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(net, "WGS84_UTM48N");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(net, outputPath + "/network.shp", builder).write();
	}
}