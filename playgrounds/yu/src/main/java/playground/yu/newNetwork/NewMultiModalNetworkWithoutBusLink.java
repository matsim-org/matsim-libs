package playground.yu.newNetwork;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

public class NewMultiModalNetworkWithoutBusLink {
	private static Set<Link> busLinks2delete = new HashSet<Link>();

	public static NetworkLayer excludeBusLinks(NetworkLayer mmNet,
			TransitSchedule schedule) {
		for (TransitLine tl : schedule.getTransitLines().values()) {
			for (TransitRoute tr : tl.getRoutes().values()) {
				handleRoute(tr, mmNet);
			}
		}
		checkLinks2delete();
		for (Link link : busLinks2delete)
			mmNet.removeLink(link);
		return mmNet;
	}

	private static void handleBusLink(Link link) {
		if (link != null) {
			// System.out.println("linkId:\t" + link.getId());
			System.out.println("link prior modes:\t" + link.getAllowedModes());
			Set<TransportMode> modes = link.getAllowedModes();
			modes.add(TransportMode.bus);
			link.setAllowedModes(modes);
			System.out.println("link posterior modes:\t"
					+ link.getAllowedModes());
			busLinks2delete.add(link);
		}
	}

	private static void handleOtherPtLink(Link link, TransportMode mode) {
		if (link != null) {
			System.out.println("link prior modes:\t" + link.getAllowedModes());
			Set<TransportMode> modes = link.getAllowedModes();
			modes.add(mode);
			link.setAllowedModes(modes);
			System.out.println("link posterior modes:\t"
					+ link.getAllowedModes());
		}
	}

	private static void handleRoute(TransitRoute route, Network network) {
		TransportMode tm = route.getTransportMode();
		NetworkRouteWRefs nrwr = route.getRoute();
		if (tm.equals(TransportMode.bus)) {
			handleBusLink(network.getLinks().get(nrwr.getStartLinkId()));
			for (Id linkId : nrwr.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				handleBusLink(link);
			}
			handleBusLink(network.getLinks().get(nrwr.getEndLinkId()));
		} else {
			System.out.println(tm);
			handleOtherPtLink(network.getLinks().get(nrwr.getStartLinkId()), tm);
			for (Id linkId : nrwr.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				handleOtherPtLink(link, tm);
			}
			handleOtherPtLink(network.getLinks().get(nrwr.getEndLinkId()), tm);
		}
	}

	private static void checkLinks2delete() {
		Set<TransportMode> modes = new HashSet<TransportMode>();
		modes.add(TransportMode.pt);
		modes.add(TransportMode.bus);
		Set<Link> tmpSet = new HashSet<Link>();
		tmpSet.addAll(busLinks2delete);
		for (Link link : tmpSet) {
			for (TransportMode mode : link.getAllowedModes()) {
				if (!modes.contains(mode)) {
					busLinks2delete.remove(link.getId());
					break;
				}
			}
		}
	}

	public static void main(final String[] args) {
		String multiModalNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
		String transitScheduleFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		String newNetworkFile = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/newMultiModalNetBiggerWithoutBusLinkTest.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);

		NetworkLayer multiModalNetwork = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(multiModalNetworkFile);

		TransitSchedule schedule = scenario.getTransitSchedule();
		try {
			new TransitScheduleReader(scenario).readFile(transitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		new NetworkWriter(excludeBusLinks(multiModalNetwork, schedule))
				.write(newNetworkFile);
	}
}
