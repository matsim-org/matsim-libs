package playground.sergioo.BusRoutesVisualizer.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.sergioo.GTFS2PTSchedule.Stop;

public class PartialRoute {

	private List<String> path;
	private Collection<StopRoutes> nextRoutes;
	/**
	 * @param network 
	 * @param path
	 * @param links 
	 */
	public PartialRoute(Network network, String routeId, List<String> path, int numTransfers, Map<String, String[]> finishedTrips, Map<String, Stop> stops, Map<String, Stop>[] activeStops, Set<Link>[] links) {
		super();
		this.path = path;
		nextRoutes = new ArrayList<StopRoutes>();
		numTransfers--;
		for(String link:path) {
			String stopId = getStopIdLink(link,stops);
			if(stopId!=null)
				nextRoutes.add(new StopRoutes(network, routeId, stopId, numTransfers, finishedTrips, stops, activeStops, links));
		}
	}
	private String getStopIdLink(String link, Map<String, Stop> stops) {
		for(Entry<String,Stop> stopE:stops.entrySet())
			if(stopE.getValue().getLinkId()!=null && stopE.getValue().getLinkId().equals(link))
				return stopE.getKey();
		return null;
	}
	
	public LinkSequenceTree getLinkSequenceTree(Network network) {
		List<Link> links = new ArrayList<Link>();
		for(String link:path)
			links.add(network.getLinks().get(new IdImpl(link)));
		LinkSequenceTree linkSequenceTree = new LinkSequenceTree(links);
		for(StopRoutes stopRoute:nextRoutes)
			linkSequenceTree.getNextSequences().addAll(stopRoute.getLinkSequenceTree(network));
		return linkSequenceTree;
	}
}
