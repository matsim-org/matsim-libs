package playground.artemc.networkTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCapacityChanger {

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
		String freewayLinkList[] = new String[] { "8", "9", "11", "6", "28", "43", "50", "55", "57", "45", "25", "26" };

		Integer standardHighwayCapacity = 1800;
		Integer standardFreewayCapacity = 1200;
		Integer standardUrbanCapacity = 700;

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"../roadpricingSingapore/scenarios/siouxFalls/networkCapacityChanges.csv"));

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkPath);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		network.setCapacityPeriod(3600.0);

		addHighwayLinks(highwayLinkList);
		addFreewayLinks(freewayLinkList);

		double highwayMax = 0;
		double highwayMin = 10000;
		double freewayMax = 0;
		double freewayMin = 10000;
		double urbanMax = 0;
		double urbanMin = 10000;

		double hLanesMax = 0;
		double hLanesMin = 10000;
		double fLanesMax = 0;
		double fLanesMin = 10000;
		double uLanesMax = 0;
		double uLanesMin = 10000;

		for (Id linkId : network.getLinks().keySet()) {
			Coord fromNode = network.getLinks().get(linkId).getFromNode().getCoord();
			Coord toNode = network.getLinks().get(linkId).getToNode().getCoord();

			double capacity = network.getLinks().get(linkId).getCapacity() / 14 * 2.8;
			double lanes = 0;

			if (highwayLinks.contains(linkId.toString())) {
				lanes = (int) (Math.round(capacity / standardHighwayCapacity));
				network.getLinks().get(linkId).setFreespeed(25);

				if (lanes < 2)
					lanes = 2;
				if (capacity / lanes > highwayMax)
					highwayMax = capacity / lanes;
				if (capacity / lanes < highwayMin)
					highwayMin = capacity / lanes;
				if (lanes > hLanesMax)
					hLanesMax = lanes;
				if (lanes < hLanesMin)
					hLanesMin = lanes;
			} else if (freewayLinks.contains(linkId.toString())) {
				lanes = (int) (Math.round(capacity / standardFreewayCapacity));
				network.getLinks().get(linkId).setFreespeed(16.7);
				if (lanes < 2)
					lanes = 2;
				if (capacity / lanes > freewayMax)
					freewayMax = capacity / lanes;
				if (capacity / lanes < freewayMin)
					freewayMin = capacity / lanes;
				if (lanes > fLanesMax)
					fLanesMax = lanes;
				if (lanes < fLanesMin)
					fLanesMin = lanes;
			} else {
				lanes = (int) (Math.round(capacity / standardUrbanCapacity));
				network.getLinks().get(linkId).setFreespeed(13.9);
				if (lanes < 2)
					lanes = 2;
				if (capacity / lanes > urbanMax)
					urbanMax = capacity / lanes;
				if (capacity / lanes < highwayMin)
					urbanMin = capacity / lanes;
				if (lanes > uLanesMax)
					uLanesMax = lanes;
				if (lanes < uLanesMin)
					uLanesMin = lanes;
			}

			network.getLinks().get(linkId).setNumberOfLanes(lanes);
			network.getLinks().get(linkId).setCapacity(capacity);

			System.out.println(linkId.toString() + ": " + capacity / lanes + " x " + lanes);

			// Double distance = Math.sqrt((fromNode.getX() -
			// toNode.getX())*(fromNode.getX() - toNode.getX()) +
			// (fromNode.getY() - toNode.getY())*(fromNode.getY() -
			// toNode.getY()));
			// Double length = network.getLinks().get(linkId).getLength();
			// Double change = (distance - length) / length;
			// network.getLinks().get(linkId).setLength(distance);
			// writer.write(linkId.toString()+","+length+","+distance+","+change+"\n");
		}

		System.out.println();
		System.out.println("Highway: " + highwayMin + " - " + highwayMax + " Lanes: " + hLanesMin + " - " + hLanesMax);
		System.out.println("Major urban road: " + freewayMin + " - " + freewayMax + " Lanes: " + fLanesMin + " - " + fLanesMax);
		System.out.println("Urban street: " + urbanMin + " - " + urbanMax + " Lanes: " + uLanesMin + " - " + uLanesMax);

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
