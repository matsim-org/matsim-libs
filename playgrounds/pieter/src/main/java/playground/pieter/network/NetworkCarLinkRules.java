package playground.pieter.network;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkCarLinkRules {

	void run(final String[] args) {
		Scenario scenario;
		MatsimRandom.reset(123);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		ArrayList<String> normalLinkLaneCountChangeList = new ArrayList<>();
		ArrayList<String> clLinkLaneCountChangeList = new ArrayList<>();
		int carLinkCount = 0;
		double originalStorageCapacity = 0d;
		double newStorageCapacity = 0d;
		int capacityperLane = Integer.parseInt(args[1]);
		for (Link l : scenario.getNetwork().getLinks().values()) {
			// only apply rules to car links
			if (l.getAllowedModes().contains("car")) {
				carLinkCount++;
				originalStorageCapacity += l.getLength() * l.getNumberOfLanes();
				// add one lane to a cl or fl link only if they have less than
				// two lanes:
				String id = l.getId().toString();
				if (id.startsWith("cl") || id.startsWith("fl")
						&& l.getNumberOfLanes() < 2) {
					clLinkLaneCountChangeList.add(id);
					// only apply one rule per link
					continue;
				}
				if ((int) (l.getCapacity() / capacityperLane) > l
						.getNumberOfLanes()) {

					normalLinkLaneCountChangeList.add(id);
				} else {
					newStorageCapacity += l.getLength() * l.getNumberOfLanes();

				}
			}

		}

		for (String id : normalLinkLaneCountChangeList) {
			Link l = scenario.getNetwork().getLinks().get(Id.createLinkId(id));
			l.setNumberOfLanes((int) (l.getCapacity() / capacityperLane));
			newStorageCapacity += l.getLength() * l.getNumberOfLanes();
		}
		for (String id : clLinkLaneCountChangeList) {
			Link l = scenario.getNetwork().getLinks().get(Id.createLinkId(id));
			l.setNumberOfLanes(l.getNumberOfLanes() + 1);
			newStorageCapacity += l.getLength() * l.getNumberOfLanes();
			// add some extra capacity if there is too few lanes for the flow
			// cap
			if ((int) (l.getCapacity() / capacityperLane) > l
					.getNumberOfLanes()) {
				double oldLaneCount = l.getNumberOfLanes();
				l.setNumberOfLanes((int) (l.getCapacity() / capacityperLane));
				newStorageCapacity += l.getLength()
						* (l.getNumberOfLanes() - oldLaneCount);
			}
		}
		System.out
				.println(String
						.format("Changed %d out of %d car links.\n(network total across all modes: %d)",
								normalLinkLaneCountChangeList.size()
										+ clLinkLaneCountChangeList.size(),
								carLinkCount, scenario.getNetwork().getLinks()
										.size()));
		System.out
				.println(String
						.format("That is an overall increase in car storage capacity from %f meters to %f meters",
								originalStorageCapacity, newStorageCapacity));
		new NetworkWriter(scenario.getNetwork()).write(args[2]);
	}

	/**
	 * @param args
	 *            - An array of String, Double, String:
	 *            <ol>
	 *            <li>The name of the network;</li>
	 *            <li>the flow capacity per lane;</li>
	 *            <li>and the name of the output network.</li>
	 *            </ol>
	 */
	public static void main(final String[] args) {
		new NetworkCarLinkRules().run(args);
	}

}
