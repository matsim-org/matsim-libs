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

public class StopRoutes {
	
	private Collection<PartialRoute> routes;
	/**
	 * @param stopId
	 * @param stops 
	 * @param finishedTrips 
	 * @param numTransfers 
	 * @param activeStops 
	 * @param links 
	 */
	public StopRoutes(Network network, String prevRoute, String stopId, int numTransfers, Map<String, String[]> finishedTrips, Map<String, Stop> stops, Map<String, Stop>[] activeStops, Set<Link>[] links) {
		super();
		activeStops[activeStops.length-numTransfers-2].put(stopId,stops.get(stopId));
		if(numTransfers>=0) {
			routes = new ArrayList<PartialRoute>();
			for(Entry<String, String[]> route:finishedTrips.entrySet()) {
				if(isTripOk(route.getKey())) {
					boolean is = false;
					int i=0;
					for(;!is && i<route.getValue().length;i++)
						if(route.getValue()[i].equals(stops.get(stopId).getLinkId())&&(prevRoute==null||!prevRoute.equals(route.getKey())))
							is=true;
					if(is) {
						List<String> path = new ArrayList<String>();
						for(;i<route.getValue().length;i++) {
							path.add(route.getValue()[i]);
							links[activeStops.length-numTransfers-1].add(network.getLinks().get(new IdImpl(route.getValue()[i])));
						}
						routes.add(new PartialRoute(network, route.getKey(), path, numTransfers, finishedTrips, stops, activeStops, links));
					}
				}		
			}
		}
	}
	private boolean isTripOk(String key) {
		return key.contains("weekday")&&!key.contains("-p");
	}
	public Collection<LinkSequenceTree> getLinkSequenceTree(Network network) {
		Collection<LinkSequenceTree> res = new ArrayList<LinkSequenceTree>();
		for(PartialRoute partialRoute:routes)
			res.add(partialRoute.getLinkSequenceTree(network));
		return res;
	}
	
}
