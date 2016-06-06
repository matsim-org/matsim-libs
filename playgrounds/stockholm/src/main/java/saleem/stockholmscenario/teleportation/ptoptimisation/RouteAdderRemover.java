package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class RouteAdderRemover {
	public void deleteRandomRoutes(TransitSchedule schedule, Vehicles vehicles){//With 10 % chance of selecting a line, and 10% chance of removing each of its route. 
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
				if(Math.random()<=0.1){//With 10% probability
					ArrayList<TransitRoute> routes = cutilforroutes.toArrayList(tline.getRoutes().values().iterator());
					int sizer = routes.size();
					for(int j=0;j<sizer;j++) {
						TransitRoute troute = routes.get(j);
						if(Math.random()<=0.1){
							ArrayList<Departure> departures = cutilfordepartures.toArrayList(troute.getDepartures().values().iterator());
							Departure firstdeparture=departures.get(0);
							Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(10000000 * Math.random()), Vehicle.class);
							VehicleType vehtype = vehicleinstances.get(firstdeparture.getVehicleId()).getType();
							Vehicle veh = new VehicleImpl(vehid, vehtype);
							vehicles.addVehicle(veh);
							for(int k=0; k<departures.size();k++){
								Departure departure=departures.get(k);
								troute.removeDeparture(departure);
								vehicles.removeVehicle(departure.getVehicleId());//To remove the vehicles used in the route
							}
							double time = 115200;//Morning Peak
							Departure hypodeparture = tschedulefact.createDeparture(Id.create("DepHypo"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
							hypodeparture.setVehicleId(vehid);
							troute.addDeparture(hypodeparture);
						}
					}
				}
			}
		}
	}
	public void addRandomRoutes(Scenario scenario, TransitSchedule schedule, Vehicles vehicles){
		TransitScheduleFactory tschedulefact = schedule.getFactory();
		CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
		CollectionUtil<TransitRoute> cutilforroutes = new CollectionUtil<TransitRoute>();
		ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		Map<Id<Link>, TransitRouteStop> links2stops = mapStops2Links(schedule);
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().iterator().next().getVehicleId()).getType().getId().toString().equals("BUS")){//If a bus line
				if(Math.random()<=0.1){//With 10% probability
					ArrayList<TransitRoute> routes = cutilforroutes.toArrayList(tline.getRoutes().values().iterator());
					int sizer = routes.size();
					for(int j=0;j<sizer;j++) {
						TransitRoute troute = routes.get(j);
						if(Math.random()<=0.1){
							TransitRoute route = createTransiteRoute(scenario, tschedulefact, troute, links2stops);//Create a new Transit Route, with same origin as troute
							if(route==null)return;
							addDeparturestoRoute(tschedulefact, route, vehicles);
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
	public void addDeparturestoRoute(TransitScheduleFactory tschedulefact, TransitRoute route, Vehicles vehicles){//Creates and adds random departures to the newly created route
		
		int numpeakmorning = (int)(Math.ceil(4*Math.random()));//Peakhour departures
		int numpeakevening = (int)(Math.ceil(4*Math.random()));
		int numrandominday = (int)(Math.ceil(8*Math.random()));//Overall day departures
		Map<Id<Vehicle>, Vehicle> vehinstances = vehicles.getVehicles();
		for(int i=0;i<numpeakmorning; i++){
			Id<Vehicle> vehid= Id.create("VehAdded"+(int)Math.floor(10000000 * Math.random()), Vehicle.class);
			if(!(vehinstances.containsKey(vehid))){//If same vehicle has not been added before
				double time = 25200 + 7200*Math.random();//Morning Peak
				Departure departure = tschedulefact.createDeparture(Id.create("DepAdded"+(int)Math.floor(10000000 * Math.random()), Departure.class), time);
				departure.setVehicleId(vehid);
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
				if(!route.getDepartures().containsKey(departure.getId())){//If not already added a departure with the same id
					route.addDeparture(departure);
				}
			}
		}
	}
	public TransitRoute createTransiteRoute(Scenario scenario, TransitScheduleFactory tschedulefact, TransitRoute troute, Map<Id<Link>, TransitRouteStop> links2stops){
		Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
		TransitRouteStop origin = troute.getStops().get(0);
		List<TransitRouteStop> transitstopsfornewroute = new ArrayList<TransitRouteStop>();
		Link destinationlink = createDestination(links2stops, transitstopsfornewroute, links, origin);//Changes transitstopsfornewroute within the function
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(origin.getStopFacility().getLinkId(), destinationlink.getId());
		if(transitstopsfornewroute.size()==1){
			return null;
		}
		Id<TransitRoute> id = Id.create(origin.getStopFacility().getId().toString()+"to"+transitstopsfornewroute.get(transitstopsfornewroute.size()-1).getStopFacility().getId()+"added", TransitRoute.class);
		TransitRoute route = tschedulefact.createTransitRoute(id, networkRoute, transitstopsfornewroute, "pt");
		return route;
		
	}
	//Create Destination Link, and creates list of stops until the destination link, returns the link, changes stopsfornewroute by reference
	public Link createDestination(Map<Id<Link>, TransitRouteStop> links2stops, List<TransitRouteStop> stopsfornewroute, Map<Id<Link>, ? extends Link> links, TransitRouteStop origin){
		int numofstops = (int)(4+11*Math.random());
		Link link = links.get(origin.getStopFacility().getLinkId());
		TransitRouteStop stop = links2stops.get(link.getId());
		stopsfornewroute.add(stop);//Sequence of stops according to selected links
		Node node = link.getToNode();
		int i=0;
		while(i++<numofstops){
			Set<Id<Link>> keyset = node.getOutLinks().keySet();//Outgoing links from node
			if(keyset.size()==0){
				return link;
			}
			Iterator<Id<Link>> iterator = keyset.iterator();
			int randval = (int)Math.floor(keyset.size()*Math.random());//To select one outlink randomly
			int j=0;
			while(j++<randval){
				iterator.next();
			}
			Id<Link> linkid = iterator.next();
			link = links.get(linkid);//Select one outlink randomly
			node = link.getToNode();
			if(links2stops.get(link.getId())!=null){
				TransitRouteStop tstop = links2stops.get(link.getId());
				stopsfornewroute.add(tstop);//Sequence of stops according to selected links

			}
			else{
				System.out.println("Strange");
			}
		}
		return link;

	}	
}
