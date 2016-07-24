package playground.artemc.networkTools;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class BuslaneGenerator {

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

		for (Id<TransitLine> line : transitSchedule.getTransitLines().keySet()) {
			System.out.print("Line: " + line.toString());
			for (Id<TransitRoute> route : transitSchedule.getTransitLines().get(line).getRoutes().keySet()) {
				System.out.println("  Route: " + route.toString());
				System.out.println("Links: ");
				for (Id<Link> link : transitSchedule.getTransitLines().get(line).getRoutes().get(route).getRoute().getLinkIds()) {
					System.out.print(link.toString() + ",");
					Id<Link> newLinkId = Id.create(link.toString() + "_car", Link.class);
					if (!network.getLinks().containsKey(newLinkId)) {
						double length = network.getLinks().get(link).getLength();
						double numLanes = network.getLinks().get(link).getNumberOfLanes();
						double laneCapacity = network.getLinks().get(link).getCapacity() / numLanes;
						double freespeed = network.getLinks().get(link).getFreespeed();

						network.getLinks().get(link).setCapacity(laneCapacity);
						network.getLinks().get(link).setNumberOfLanes(1.0);
						network.getLinks().get(link).setAllowedModes(allowedModesPT);
						final Id<Link> id = newLinkId;
						final double length1 = length;
						final double freespeed1 = freespeed;
						NetworkUtils.createAndAddLink(network,id, network.getLinks().get(link).getFromNode(), network.getLinks().get(link).getToNode(), length1, freespeed1, laneCapacity * (numLanes - 1), (numLanes - 1) );
						network.getLinks().get(newLinkId).setAllowedModes(allowedModesPrivate);
					}

				}
				System.out.println();
			}
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(outputNetworkPath);
	}
}
