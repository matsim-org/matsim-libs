package playground.sergioo.ptsim2013;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;

public class TransitSheduleToNetwork {

	public static final String SEPARATOR = "--";

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario)).readFile(args[0]);
		scenario.getConfig().transit().setUseTransit(true);
		(new TransitScheduleReader(scenario)).readFile(args[1]);
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory =  new NetworkFactoryImpl(network);
		Set<String> modes = new HashSet<String>(Arrays.asList("pt"));
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
			Node sS = factory.createNode(Id.create("s-"+stop.getId(), Node.class), stop.getCoord());
			network.addNode(sS);
			Node eS = factory.createNode(Id.create("e-"+stop.getId(), Node.class), stop.getCoord());
			network.addNode(eS);
			Link link = factory.createLink(Id.create(stop.getId(), Link.class), sS, eS);
			link.setLength(30);
			link.setFreespeed(20);
			link.setCapacity(10000);
			link.setAllowedModes(modes);
			link.setNumberOfLanes(1);
			network.addLink(link);
		}
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				Id<Link> sId = null; 
				Id<Link> id;
				List<Id<Link>> ids = new ArrayList<Id<Link>>();
				double length = scenario.getNetwork().getLinks().get(route.getRoute().getStartLinkId()).getLength();
				int linkPos = 0;
				for(int s=0; s<route.getStops().size()-1; s++) {
					Id<TransitStopFacility> idO = route.getStops().get(s).getStopFacility().getId();
					Id<TransitStopFacility> idD = route.getStops().get(s+1).getStopFacility().getId();
					Id<Link> linkId = route.getRoute().getLinkIds().get(linkPos);
					id = Id.createLinkId(route.getStops().get(s).getStopFacility().getId()+SEPARATOR+route.getStops().get(s+1).getStopFacility().getId());
					if(sId==null)
						sId = Id.createLinkId(idO.toString());
					else
						ids.add(Id.createLinkId(idO.toString()));
					ids.add(id);
					while(!linkId.equals(route.getStops().get(s+1).getStopFacility().getLinkId())) {
						length += scenario.getNetwork().getLinks().get(linkId).getLength();
						if(++linkPos == route.getRoute().getLinkIds().size())
							linkId = route.getRoute().getEndLinkId();
						else
							linkId = route.getRoute().getLinkIds().get(linkPos);
					}
					Link link = network.getLinks().get(id);
					if(link==null) {
						link = factory.createLink(id, network.getNodes().get(Id.createNodeId("e-"+idO)), network.getNodes().get(Id.createNodeId("s-"+idD)));
						link.setLength(length);
						link.setFreespeed(20);
						link.setCapacity(10000);
						link.setAllowedModes(modes);
						link.setNumberOfLanes(1);
						network.addLink(link);
					}
					else
						link.setNumberOfLanes(link.getNumberOfLanes()+1);
					length = 0;
				}
				route.setRoute(new LinkNetworkRouteImpl(sId, ids, Id.create(route.getStops().get(route.getStops().size()-1).getStopFacility().getId(), Link.class)));
			}
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values())
			stop.setLinkId(Id.create(stop.getId().toString(), Link.class));
		TransitScheduleValidator.printResult(TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), network));
		(new NetworkWriter(network)).write(args[2]);
		(new TransitScheduleWriter(scenario.getTransitSchedule())).writeFile(args[3]);
	}
	
}
