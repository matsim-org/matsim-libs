package playground.artemc.networkTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkLengthChanger {

	public static void main(String[] args) throws IOException {

		String networkPath = args[0];
		String outputNetworkPath = args[1];

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"../roadpricingSingapore/scenarios/siouxFalls/networkLengthChanges.csv"));

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkPath);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		for (Id linkId : network.getLinks().keySet()) {
			Coord fromNode = network.getLinks().get(linkId).getFromNode().getCoord();
			Coord toNode = network.getLinks().get(linkId).getToNode().getCoord();

			Double distance = Math.sqrt((fromNode.getX() - toNode.getX()) * (fromNode.getX() - toNode.getX())
					+ (fromNode.getY() - toNode.getY()) * (fromNode.getY() - toNode.getY()));

			Double length = network.getLinks().get(linkId).getLength();

			Double change = (distance - length) / length;

			network.getLinks().get(linkId).setLength(distance);

			writer.write(linkId.toString() + "," + length + "," + distance + "," + change + "\n");
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(outputNetworkPath);

		writer.close();
	}

}
