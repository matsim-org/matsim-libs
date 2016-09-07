package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;
public class RouteAdderRemover {
	private CollectionUtil<Node> cutil = new CollectionUtil<Node>(); 
//	final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
//			.getCoordinateTransformation(
//					StockholmTransformationFactory.WGS84_SWEREF99,
//					StockholmTransformationFactory.WGS84);
	public void deleteRandomRoutes(TransitSchedule schedule, Vehicles vehicles, double factorline, double factorroute){//With 10 % chance of selecting a line, and 10% chance of removing each of its route. 
		CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
		CollectionUtil<TransitRoute> cutilforroutes = new CollectionUtil<TransitRoute>();
		ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		CollectionUtil<Departure> cutilfordepartures = new CollectionUtil<Departure>();
		TransitScheduleFactory tschedulefact = schedule.getFactory();
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().iterator().next().getVehicleId()).getType().getId().toString().equals("BUS")){//If a bus line
				if(Math.random()<=factorline){//With factorline*100 percent probability
					ArrayList<TransitRoute> routes = cutilforroutes.toArrayList(tline.getRoutes().values().iterator());
					int sizer = routes.size();
					for(int j=1;j<sizer;j++) {
						TransitRoute troute = routes.get(j);
						if(Math.random()<=factorroute && !(troute.getDepartures().values()
								.iterator().next().getDepartureTime()==115200 && troute.getDepartures().size()==1)){//With factorroute*100 percent probability  
							ArrayList<Departure> departures = cutilfordepartures.toArrayList(troute.getDepartures().values().iterator());
							for(int k=0; k<departures.size();k++){
								Departure departure=departures.get(k);
								troute.removeDeparture(departure);
								if(troute.getDepartures().size()==0){
									double time = 115200;//Hypothetical departure after end time; delete all other departures; effectively a route that doesn't exist.
									Departure hypodeparture = tschedulefact.createDeparture(Id.create("DepHypo"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
									hypodeparture.setVehicleId(departure.getVehicleId());
									troute.addDeparture(hypodeparture);
								}
								else{
									vehicles.removeVehicle(departure.getVehicleId());//To remove the vehicles used in the route
								}

							}
							
//							tline.removeRoute(troute);
						}
					}
				}
			}
		}
	}
	public void addRandomRoutes(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline, double factorroute){
		TransitScheduleFactory tschedulefact = schedule.getFactory();
		CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
		CollectionUtil<TransitRoute> cutilforroutes = new CollectionUtil<TransitRoute>();
		ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		Map<Id<TransitStopFacility>, TransitRouteStop> stops = getTransitStops(schedule);
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			Vehicle vehicle = vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().
							  iterator().next().getVehicleId());
			if(vehicle.getType().getId().toString().equals("BUS")){//If a bus line
				VehicleType type = vehicle.getType();
				if(Math.random()<=factorline){//With factorline*100 percent probability
					ArrayList<TransitRoute> routes = cutilforroutes.toArrayList(tline.getRoutes().values().iterator());
					int sizer = routes.size();
					for(int j=0;j<sizer;j++) {
						TransitRoute troute = routes.get(j);
						if(Math.random()<=factorroute && !(troute.getDepartures().values()
								.iterator().next().getDepartureTime()==115200 && troute.getDepartures().size()==1)){//With factorroute*100 percent probability
							TransitRoute route = createTransiteRoute(scenario, schedule, tschedulefact, troute, stops);//Create a new Transit Route, with same origin as troute
							if(route==null)return;
							addDeparturestoRoute(tschedulefact, route, vehicles, type);
							tline.addRoute(route);
						}
					}
				}
			}
		}
	}
	public Map<Id<Link>, TransitRouteStop> mapStops2Links(TransitSchedule schedule){//Maps links to transit stops
		Map<Id<Link>, TransitRouteStop> link2stop = new LinkedHashMap<Id<Link>, TransitRouteStop>();
		Iterator<TransitLine> lines = schedule.getTransitLines().values().iterator();
		while(lines.hasNext()) {
			TransitLine tline = lines.next();
			Iterator<TransitRoute> routes = tline.getRoutes().values().iterator();
			while(routes.hasNext()) {
				TransitRoute troute = routes.next();
				Iterator<TransitRouteStop> stops =  troute.getStops().iterator();
				while(stops.hasNext()){
					TransitRouteStop stop = stops.next();
					link2stop.put(stop.getStopFacility().getLinkId(), stop);
				}
			}
		}
		return link2stop;
	}
	public Map<Id<TransitStopFacility>, TransitRouteStop> getTransitStops(TransitSchedule schedule){//returns a map of all transit route stops maped against id of the facility they represent
		Map<Id<TransitStopFacility>, TransitRouteStop> stops = new LinkedHashMap<Id<TransitStopFacility>, TransitRouteStop>();
		Iterator<TransitLine> lines = schedule.getTransitLines().values().iterator();
		while(lines.hasNext()) {
			TransitLine tline = lines.next();
			Iterator<TransitRoute> routes = tline.getRoutes().values().iterator();
			while(routes.hasNext()) {
				TransitRoute troute = routes.next();
				Iterator<TransitRouteStop> routestops =  troute.getStops().iterator();
				while(routestops.hasNext()){
					TransitRouteStop stop = routestops.next();
					stops.put(stop.getStopFacility().getId(), stop);
				}
			}
		}
		return stops;
	}
	public void addDeparturestoRoute(TransitScheduleFactory tschedulefact, TransitRoute route, Vehicles vehicles, VehicleType vtype ){//Creates and adds 
																																	 //random departures to the newly created route
		
		int numpeakmorning = (int)(Math.ceil(2*Math.random()));//Peakhour departures
		int numpeakevening = (int)(Math.ceil(2*Math.random()));
		int numrandominday = (int)(Math.ceil(4*Math.random()));//Overall day departures
		Map<Id<Vehicle>, Vehicle> vehinstances = vehicles.getVehicles();
		for(int i=0;i<numpeakmorning; i++){
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(10000000 * Math.random()), Vehicle.class);
			if(!(vehinstances.containsKey(vehid))){//If same vehicle has not been added before
				double time = 25200 + 7200*Math.random();//Morning Peak
				Departure departure = tschedulefact.createDeparture(Id.create("DepAdded"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
				departure.setVehicleId(vehid);
				Vehicle veh = new VehicleImpl(vehid, vtype);
				vehicles.addVehicle(veh);
				if(!route.getDepartures().containsKey(departure.getId())){//If not already added a departure with the same id
					route.addDeparture(departure);
				}
			}
		}
		for(int i=0;i<numpeakevening; i++){
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(10000000 * Math.random()), Vehicle.class);
			if(!(vehinstances.containsKey(vehid))){//If same vehicle has not been added before
				double time = 59400 + 7200*Math.random();//Evening Peak
				Departure departure = tschedulefact.createDeparture(Id.create("DepAdded"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
				departure.setVehicleId(vehid);
				Vehicle veh = new VehicleImpl(vehid, vtype);
				vehicles.addVehicle(veh);
				if(!route.getDepartures().containsKey(departure.getId())){//If not already added a departure with the same id
					route.addDeparture(departure);
				}
			}
		}
		for(int i=0;i<numrandominday; i++){
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(10000000 * Math.random()), Vehicle.class);
			if(!(vehinstances.containsKey(vehid))){//If same vehicle has not been added before
				double time = 86400*Math.random();//Anytime within day
				Departure departure = tschedulefact.createDeparture(Id.create("DepAdded"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
				departure.setVehicleId(vehid);
				Vehicle veh = new VehicleImpl(vehid, vtype);
				vehicles.addVehicle(veh);
				if(!route.getDepartures().containsKey(departure.getId())){//If not already added a departure with the same id
					route.addDeparture(departure);
				}
			}
		}
	}
	public TransitRoute createTransiteRoute(Scenario scenario, TransitSchedule schedule, TransitScheduleFactory tschedulefact, TransitRoute troute, Map<Id<TransitStopFacility>, TransitRouteStop> stops){
		TransitRouteStop origin = troute.getStops().get(0);
		List<TransitRouteStop> transitstopsfornewroute = new ArrayList<TransitRouteStop>();
		List<Id<Link>> linksfornewroute = new ArrayList<Id<Link>>();
		createDestination(scenario, schedule, stops, transitstopsfornewroute,linksfornewroute, origin);//Changes transitstopsfornewroute 
																											//and networkroutelinks within the function
		if(transitstopsfornewroute.size()<2){
			return null;
		}
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(linksfornewroute.get(0),linksfornewroute.subList(1, linksfornewroute.size()-1), linksfornewroute.get(linksfornewroute.size()-1));
		Id<TransitRoute> id = Id.create(origin.getStopFacility().getId().toString()+"to"+transitstopsfornewroute.get
				(transitstopsfornewroute.size()-1).getStopFacility().getId()+"added"+(100000*Math.random()), TransitRoute.class);
		TransitRoute route = tschedulefact.createTransitRoute(id, networkRoute, transitstopsfornewroute, "pt");
		return route;
		
	}
	public TransitRoute createReverseTransiteRoute(Scenario scenario, TransitSchedule schedule, TransitScheduleFactory tschedulefact, TransitRoute forward){
		List<TransitRouteStop> transitstopsfornewroute = new ArrayList<TransitRouteStop>();
		NetworkFactory factory = scenario.getNetwork().getFactory();
		Network network = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Set<String> modes = new HashSet<String>();
		modes.add("pt");
		List<TransitRouteStop> transitstopsforward = forward.getStops();
		if(transitstopsforward.size()<2){
			return null;
		}
		int i=transitstopsforward.size()-1;
		while(i>=0){
			transitstopsfornewroute.add(transitstopsforward.get(i));
			i--;
		}
		List<Id<Link>> linksfornewroute = new ArrayList<Id<Link>>();
		
		Id<Link> originlink = transitstopsfornewroute.get(0).getStopFacility().getLinkId();
		Id<Link> previouslink = transitstopsfornewroute.get(0).getStopFacility().getLinkId();
		Id<Link> destlink = transitstopsfornewroute.get(transitstopsfornewroute.size()-1).getStopFacility().getLinkId();
		
		for(int j=1;j<transitstopsfornewroute.size();j++){
			Node from = links.get(previouslink).getToNode();
			Node to = links.get(transitstopsfornewroute.get(j).getStopFacility().getLinkId()).getFromNode();
			Link link = factory.createLink(Id.createLinkId("LinkAdded"+from.getId().toString()+"to"+to.getId().toString()), from, to);
			link.setFreespeed(8.33);
			link.setCapacity(500.0);
			link.setAllowedModes(modes);
			link.setLength(NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord()));//Length from from node to to node
			link.setNumberOfLanes(1.0);
			if(!network.getLinks().containsKey(link.getId())){
				network.addLink(link);
			}
			linksfornewroute.add(link.getId());
			Id<Link> linkid = transitstopsfornewroute.get(j).getStopFacility().getLinkId();
			if(j!=transitstopsfornewroute.size()-1){
				linksfornewroute.add(linkid);
			}
			previouslink=linkid;
			
		}
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(originlink,linksfornewroute, destlink);
		Id<TransitRoute> id = Id.create(transitstopsfornewroute.get(0).getStopFacility().getId().toString()+"to"+transitstopsfornewroute.get
				(transitstopsfornewroute.size()-1).getStopFacility().getId()+"addedrev"+(100000*Math.random()), TransitRoute.class);
		TransitRoute route = tschedulefact.createTransitRoute(id, networkRoute, transitstopsfornewroute, "pt");
		return route;
		
	}
	public Node getNextNode(double distancefromorigin, Coord origincords, Collection<Node> nodes){
		double distance = -1;
		ArrayList<Node> neighboringnodes = this.cutil.toArrayList(nodes.iterator());
		removeNonPTNodes(neighboringnodes);
		Node selected = null;
		while(distance<distancefromorigin){
			selected = neighboringnodes.get((int)Math.floor((neighboringnodes.size()*Math.random())));
			distance = NetworkUtils.getEuclideanDistance(origincords, selected.getCoord());
		}
		return selected;
	}
	public void removeNonPTNodes(ArrayList<Node> neighboringnodes){
		int i = 0;
		while(i<neighboringnodes.size()){
			if(neighboringnodes.get(i).getId().toString().contains("tr_")) {
				i++;
			}else{
				neighboringnodes.remove(i);
			}
		}
	}
	//Create Destination Link, and creates list of stops until the destination link, returns the link, changes stopsfornewroute by reference
	public void createDestination(Scenario scenario, TransitSchedule schedule,  Map<Id<TransitStopFacility>, TransitRouteStop> stops, List<TransitRouteStop> stopsfornewroute,
								List<Id<Link>> linksfornewroute, TransitRouteStop origin){
		List<Id<TransitStopFacility>> stopsfornewrouteids = new ArrayList<Id<TransitStopFacility>>();
		NetworkFactory factory = scenario.getNetwork().getFactory();
		Network network = scenario.getNetwork();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		int numofstops = 1 + (int)Math.ceil((24*Math.random()));
		double distancefromorigin = 0;//Euclidean distance from origin must keep increasing
		Map<Id<Node>, ? extends Node>  allnodes = network.getNodes();
		String idorigstr = "tr_"+origin.getStopFacility().getId().toString();
		 if(idorigstr.indexOf('.')!=-1){
			 idorigstr = idorigstr.substring(0,idorigstr.indexOf('.'));
		 };
		Node fromnode = allnodes.get(Id.create(idorigstr, Node.class));
		if(fromnode==null){
			return;
		}
		TransitStopFacility facility = origin.getStopFacility();
		Set<String> modes = new HashSet<String>();
		modes.add("pt");
		Coord origincords = fromnode.getCoord();
		stopsfornewroute.add(origin);
		linksfornewroute.add(facility.getLinkId());
		Node tonode = null;
		int i=0;
		while(i++<numofstops){
			Collection<Node> nodes = NetworkUtils.getNearestNodes(network, fromnode.getCoord(), 2000);//Nodes within a 1000 meters
			tonode = getNextNode(distancefromorigin, origincords, nodes);//Get next node to the current node out of the neighboring nodes
																	  //such that the overall distance to the node from origin keeps increasing to avoid weird detours
			distancefromorigin = NetworkUtils.getEuclideanDistance(origincords, tonode.getCoord());
			String idstr = tonode.getId().toString().substring(3, tonode.getId().toString().length());//Removing the tr_ prefix
			TransitRouteStop tstop = stops.get(Id.create(idstr, TransitStopFacility.class));
			if(!stopsfornewrouteids.contains(tstop.getStopFacility().getId())){
				stopsfornewroute.add(tstop);
				stopsfornewrouteids.add(tstop.getStopFacility().getId());
				if(linksfornewroute.size()>0){//For linking the stop link to next stop link, if not already linked
					Node from = links.get(linksfornewroute.get(linksfornewroute.size()-1)).getToNode();
					Node to = links.get(tstop.getStopFacility().getLinkId()).getFromNode();
					Link link = factory.createLink(Id.createLinkId("LinkAdded"+from.getId().toString()+"to"+to.getId().toString()), from, to);
					link.setFreespeed(8.33);
					link.setCapacity(500.0);
					link.setAllowedModes(modes);
					link.setLength(NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord()));//Length from from node to to node
					link.setNumberOfLanes(1.0);
					if(!network.getLinks().containsKey(link.getId())){
						network.addLink(link);
					}
					linksfornewroute.add(link.getId());
				}
				
				linksfornewroute.add(tstop.getStopFacility().getLinkId());
				fromnode = allnodes.get(tonode.getId());//next search from this node
			}
		}
	}	
}
