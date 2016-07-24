package playground.artemc.networkTools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class CopyOfBuslaneGenerator_fullPriority {

	public static void main(String[] args) {

		String networkPath = args[0];
		String transitSchedulePath = args[1];
		String outputNetworkPath = args[2];

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);

		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(networkPath);
		Network network = (Network) scenario.getNetwork();

		new TransitScheduleReader(scenario).readFile(transitSchedulePath);
		TransitScheduleImpl transitSchedule = (TransitScheduleImpl) scenario.getTransitSchedule();

		Set<String> allowedModesPT = new HashSet<String>();
		allowedModesPT.add("pt");
		allowedModesPT.add("bus");
		Set<String> allowedModesPrivate = new HashSet<String>();
		allowedModesPrivate.add("car");

		for (Id line : transitSchedule.getTransitLines().keySet()) {
			System.out.print("Line: " + line.toString());
			for (Id route : transitSchedule.getTransitLines().get(line).getRoutes().keySet()) {
				System.out.println("  Route: " + route.toString());
				System.out.println("Links: ");
				Id<Node> fromNodeId = Id.create("", Node.class);
				Integer lastLinkId = Integer.parseInt(transitSchedule.getTransitLines().get(line).getRoutes().get(route)
						.getRoute().getEndLinkId().toString()) - 1;
				for (Id link : transitSchedule.getTransitLines().get(line).getRoutes().get(route).getRoute().getLinkIds()) {
					System.out.print(link.toString() + ",");
					Id<Link> newLinkId = Id.create(link.toString() + "_car", Link.class);
					if (!network.getLinks().containsKey(newLinkId)) {
						double length = network.getLinks().get(link).getLength();
						double numLanes = network.getLinks().get(link).getNumberOfLanes();
						double laneCapacity = network.getLinks().get(link).getCapacity() / numLanes;
						double freespeed = network.getLinks().get(link).getFreespeed();

						if (fromNodeId.toString().equals("")) {
							fromNodeId = network.getLinks().get(link).getFromNode().getId();
						}

						Id<Link> reverseLinkId = Id.create(network.getLinks().get(link).getId().toString() + "r", Link.class);
						network.removeLink(reverseLinkId);

						network.getLinks().get(link).setCapacity(laneCapacity);
						network.getLinks().get(link).setNumberOfLanes(1.0);
						network.getLinks().get(link).setAllowedModes(allowedModesPT);
						Id<Node> newNodeId = Id.create(network.getLinks().get(link).getToNode().getId().toString() + "c", Node.class);
						Coord newBusNodeCoord = new Coord(network.getLinks().get(link).getToNode().getCoord().getX() + 0.1, network.getLinks().get(link).getToNode().getCoord().getY() + 0.1);

						if (link.toString().equals(lastLinkId.toString())) {
							newNodeId = network.getLinks().get(link).getToNode().getId();
						} else {
							final Id<Node> id2 = newNodeId;
							final Coord coord = newBusNodeCoord;
							NetworkUtils.createAndAddNode(network, id2, coord);
						}
						final Id<Link> id = newLinkId;
						final double length1 = length;
						final double freespeed1 = freespeed;
						NetworkUtils.createAndAddLink(network,id, network.getNodes().get(fromNodeId), network.getNodes().get(newNodeId), length1, freespeed1, laneCapacity * (numLanes - 1), (numLanes - 1) );
						final Id<Link> id1 = reverseLinkId;
						final double length2 = length;
						final double freespeed2 = freespeed;
						final double numLanes1 = numLanes;
						NetworkUtils.createAndAddLink(network,id1, network.getNodes().get(newNodeId), network.getNodes().get(fromNodeId), length2, freespeed2, laneCapacity * numLanes, numLanes1 );
						network.getLinks().get(newLinkId).setAllowedModes(allowedModesPrivate);
						fromNodeId = newNodeId;
					}

				}
				System.out.println();
			}
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(outputNetworkPath);

	}

}
