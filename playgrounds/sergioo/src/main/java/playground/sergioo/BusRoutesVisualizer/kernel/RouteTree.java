package playground.sergioo.BusRoutesVisualizer.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.GTFS.Stop;

public class RouteTree {
	//Attributes
	private Map<String, Stop>[] activeStops;
	private Set<Link>[] links;
	private StopRoutes root;
	private final Network network;
	private final String stop;
	private final int numTransfers;
	
	//Methods
	public RouteTree(Network network, String code, int numTransfers, Map<String, String[]> finishedTrips, Map<String, Stop> stops) {
		super();
		this.network = network;
		this.stop = code;
		this.numTransfers = numTransfers;
		activeStops = new Map[numTransfers+2];
		links = new Set[numTransfers+2];
		for(int i=0;i<activeStops.length;i++) {
			activeStops[i]=new HashMap<String, Stop>();
			links[i]=new HashSet<Link>();
		}
		links[0].add(network.getLinks().get(new IdImpl(stops.get(stop).getLinkId())));
		this.root = new StopRoutes(network, null, stop, numTransfers,finishedTrips,stops,activeStops,links);
		System.out.println();
	}
	public Set<Link>[] getLinks() {
		return links;
	}
	public Collection<Link> getStopLinks() {
		return new ArrayList<Link>();
	}
	public Collection<Coord>[] getStopPoints() {
		Collection<Coord>[] res = new Collection[activeStops.length]; 
		for(int i=0;i<activeStops.length;i++) {
			res[i]=new ArrayList<Coord>();
			for(Stop stop:activeStops[i].values())
				res[i].add(stop.getPoint());
		}
		return res;
	}
	public String getStopId(int pos) {
		return "";
	}
	public Link getLink(String selectedLinkId) {
		return null;
	}
	public Collection<Link> getNetworkLinks(double xMin, double yMin, double xMax, double yMax) {
		Collection<Link> links =  new HashSet<Link>();
		for(Link link:network.getLinks().values()) {
			Coord linkCenter = link.getCoord();
			if(xMin<linkCenter.getX()&&yMin<linkCenter.getY()&&xMax>linkCenter.getX()&&yMax>linkCenter.getY())
				links.add(link);
		}
		return links;
	}
	public Coord getStop(String selectedStopId) {
		for(Map<String, Stop> activeStopsP:activeStops) {
			Stop stop = activeStopsP.get(selectedStopId);
			if(stop!=null)
				return stop.getPoint();
		}
		return null;
	}
	public String getIndexNearestLink(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		String nearest = "";
		return nearest;
	}
	public String getIdNearestStop(double x, double y) {
		Coord coord = new CoordImpl(x, y);
		String nearest = "";
		return nearest;
	}
	public Node getNearestNode(double x, double y) {
		Coord point = new CoordImpl(x, y);
		Node nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(Link link:network.getLinks().values()) {
			double distance = CoordUtils.calcDistance(point,link.getToNode().getCoord());
			if(distance<nearestDistance) {
				nearestDistance = distance;
				nearest = link.getToNode();
			}
		}
		return nearest;
	}
	public String getLinkIdStop(String selectedStopId) {
		for(Map<String, Stop> activeStopsP:activeStops) {
			Stop stop = activeStopsP.get(selectedStopId);
			if(stop!=null)
				return stop.getLinkId();
		}
		return "";
	}
	
}
