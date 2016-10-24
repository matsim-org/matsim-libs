package playground.artemc.networkTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCapacityMultiplier {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String networkPath = args[0];
		String outputNetworkPath = args[1];

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"../roadpricingSingapore/scenarios/siouxFalls/networkCapacityChanges.csv"));

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(networkPath);
		Network network = (Network) scenario.getNetwork();

		network.setCapacityPeriod(3600.0);

		for (Id linkId : network.getLinks().keySet()) {
			double capacity = network.getLinks().get(linkId).getCapacity() * network.getLinks().get(linkId).getNumberOfLanes();
			network.getLinks().get(linkId).setCapacity(capacity);
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(outputNetworkPath);
		writer.close();
	}

}
