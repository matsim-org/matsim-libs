package playground.artemc.networkTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCapacityChanger_2type {

	private static ArrayList<String> highwayLinks = new ArrayList<String>();
	private static ArrayList<String> freewayLinks = new ArrayList<String>();

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String networkPath = args[0];
		String outputNetworkPath = args[1];
		String highwayLinkList[] = new String[] { "37", "38", "7", "35", "2", "5", "3", "1", "18", "54", "56", "60" };

		Integer standardHighwayCapacity = 1800;
		// Integer standardFreewayCapacity = 1200;
		Integer standardUrbanCapacity = 900;

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"../roadpricingSingapore/scenarios/siouxFalls/networkCapacityChanges.csv"));
		Random generator = new Random();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkPath);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		network.setCapacityPeriod(3600.0);

		addHighwayLinks(highwayLinkList);

		for (Id linkId : network.getLinks().keySet()) {
			Coord fromNode = network.getLinks().get(linkId).getFromNode().getCoord();
			Coord toNode = network.getLinks().get(linkId).getToNode().getCoord();

			double capacity = 0;

			if (highwayLinks.contains(linkId.toString())) {
				capacity = (standardHighwayCapacity - 100) + generator.nextInt(201);
				network.getLinks().get(linkId).setFreespeed(25);
				network.getLinks().get(linkId).setNumberOfLanes(3);
			} else {
				capacity = (standardUrbanCapacity - 100) + generator.nextInt(201);
				network.getLinks().get(linkId).setFreespeed(13.9);
				network.getLinks().get(linkId).setNumberOfLanes(2);
			}

			network.getLinks().get(linkId).setCapacity(capacity);

			// System.out.println(linkId.toString()+": "+capacity/lanes+" x "+lanes);

			// Double distance = Math.sqrt((fromNode.getX() -
			// toNode.getX())*(fromNode.getX() - toNode.getX()) +
			// (fromNode.getY() - toNode.getY())*(fromNode.getY() -
			// toNode.getY()));
			// Double length = network.getLinks().get(linkId).getLength();
			// Double change = (distance - length) / length;
			// network.getLinks().get(linkId).setLength(distance);
			// writer.write(linkId.toString()+","+length+","+distance+","+change+"\n");
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(outputNetworkPath);

		writer.close();

	}

	private static void addHighwayLinks(String[] linkList) {
		for (int i = 0; i < linkList.length; i++) {
			highwayLinks.add(linkList[i]);
		}

	}

	private static void addFreewayLinks(String[] linkList) {
		for (int i = 0; i < linkList.length; i++) {
			freewayLinks.add(linkList[i]);
		}

	}

}
