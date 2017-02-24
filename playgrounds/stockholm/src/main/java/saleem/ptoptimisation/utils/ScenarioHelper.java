package saleem.ptoptimisation.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * This class is an important helper class for keeping copies of different attributes of the current scenario, 
 * or updating them to variations of them.
 * It is mainly used in switching between PT MatSim states and decision variables during PT Optimisation .
 * 
 * @author Mohammad Saleem
 */
public class ScenarioHelper {
	/*This is a function to create a deep copy of transit schedule. The deep copy is not fully deep and may also copy references. 
	 * All those components of the transit schedule which are expected to be randomly adapted for PT optimisation are deep copied. 
	*/ 
	public TransitSchedule deepCopyTransitSchedule(TransitSchedule schedule){
		TransitScheduleFactoryImpl tschedulefact = new TransitScheduleFactoryImpl();
		TransitSchedule newschedule = tschedulefact.createTransitSchedule();
		//Add all stop facilities
		Map<Id<TransitStopFacility>, TransitStopFacility> stopfacilities = schedule.getFacilities();
		Iterator<Id<TransitStopFacility>> stopsiterator =  stopfacilities.keySet().iterator();
		while(stopsiterator.hasNext()){
			TransitStopFacility stopfacility = stopfacilities.get(stopsiterator.next());
			newschedule.addStopFacility(stopfacility);
		}
		//Deep copy all lines and add all them to new transit schedule
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Iterator<Id<TransitLine>> linesiterator =  lines.keySet().iterator();
		while(linesiterator.hasNext()){
			TransitLine tline = lines.get(linesiterator.next());
			TransitLine newline = tschedulefact.createTransitLine(Id.create(tline.getId().toString(), TransitLine.class));
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			Iterator<Id<TransitRoute>> routesiterator =  routes.keySet().iterator();
			while(routesiterator.hasNext()){
				TransitRoute troute = routes.get(routesiterator.next());
				TransitRoute newroute = tschedulefact.createTransitRoute
						(Id.create(troute.getId().toString(), TransitRoute.class), troute.getRoute(), troute.getStops(), troute.getTransportMode());
				Map<Id<Departure>, Departure> departures = troute.getDepartures();
				Iterator<Id<Departure>> depsiterator =  departures.keySet().iterator();
				while(depsiterator.hasNext()){
					Departure departure = departures.get(depsiterator.next());
					Departure newdeparture = tschedulefact.createDeparture(Id.create(departure.getId().toString(), Departure.class), departure.getDepartureTime());
					newdeparture.setVehicleId(departure.getVehicleId());
					newroute.addDeparture(newdeparture);
				}
				newline.addRoute(newroute);
			}
			newschedule.addTransitLine(newline);
		}
		return newschedule;
	}
	//Creates a deep copy of Vehicles object
	public Vehicles deepCopyVehicles(Vehicles vehicles){
		Vehicles newvehicles = VehicleUtils.createVehiclesContainer();
		//Add all vehicle types
		Map<Id<VehicleType>, VehicleType> vehtypes = vehicles.getVehicleTypes();
		Iterator<Id<VehicleType>> vtiterator = vehtypes.keySet().iterator();
		while(vtiterator.hasNext()){
			VehicleType vt = vehtypes.get(vtiterator.next());
			newvehicles.addVehicleType(vt);
		}
		//Add all vehicle instances
		Map<Id<Vehicle>, Vehicle> vehinstances = vehicles.getVehicles();
		Iterator<Id<Vehicle>> vehsiterator = vehinstances.keySet().iterator();
		while(vehsiterator.hasNext()){
			Vehicle veh = vehinstances.get(vehsiterator.next());
			newvehicles.addVehicle(veh);
		}
		return newvehicles;
	}
	/*Remove all nodes and links from the network. 
	 * This may create problems if called within a running simulation, 
	 * as removing links from the network may not be allowed.
	 */
	public void removeAllNodesAndLinks(Scenario scenario){
		CollectionUtil<Id<Node>> cutil = new CollectionUtil<Id<Node>>();
		Network network = scenario.getNetwork();
		Map<Id<Node>, ? extends Node> nodes = network.getNodes();
		ArrayList<Id<Node>> nodeids = cutil.toArrayList(network.getNodes().keySet().iterator());
		Iterator<Id<Node>> nodeidsiter = nodeids.iterator();
		while(nodeidsiter.hasNext()){
			nodes.remove(nodeidsiter.next());
		}
		
		CollectionUtil<Id<Link>> cutillinks = new CollectionUtil<Id<Link>>();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		ArrayList<Id<Link>> linkids = cutillinks.toArrayList(network.getLinks().keySet().iterator());
		Iterator<Id<Link>> linkidsiter = linkids.iterator();
		while(linkidsiter.hasNext()){
			links.remove(linkidsiter.next());
		}
	}
	//Add all nodes and links to the network from another new network. 
	public void addNodesAndLinks(Scenario scenario, Network newnetwork){
		Network network = scenario.getNetwork();
		Map<Id<Node>, ? extends Node> nodes = newnetwork.getNodes();
		Iterator<Id<Node>> nodeidsiter = newnetwork.getNodes().keySet().iterator();
		while(nodeidsiter.hasNext()){
			network.addNode(nodes.get(nodeidsiter.next()));
		}
		
		CollectionUtil<Id<Link>> cutillinks = new CollectionUtil<Id<Link>>();
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		ArrayList<Id<Link>> linkids = cutillinks.toArrayList(network.getLinks().keySet().iterator());
		Iterator<Id<Link>> linkidsiter = linkids.iterator();
		while(linkidsiter.hasNext()){
			links.remove(linkidsiter.next());
		}
	}
	//Calculate number of routes in transit schedule
	public int getNumberOfRoutes(TransitSchedule schedule){
		int numofroutes=0;
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Iterator<Id<TransitLine>> linesiterator =  lines.keySet().iterator();
		while(linesiterator.hasNext()){
			TransitLine tline = lines.get(linesiterator.next());
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			numofroutes+=routes.size();
		}
		return numofroutes;
	}
	//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
	public void removeEntireScheduleAndVehicles(Scenario scenario){
		
		Vehicles vehicles = scenario.getTransitVehicles();
		
		//Remove all vehicle instances and types
		CollectionUtil<Id<Vehicle>> cutil = new CollectionUtil<Id<Vehicle>>();
		 ArrayList<Id<Vehicle>> list = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());//Converted to array list in order to avoid concurrent modification issues.
	        int i=0;int size = list.size();
	        while(i<size){
	        	vehicles.removeVehicle(list.get(i));i++;
	        }
	        //Vehicles instances must be removed before removing the vehicle types
	        CollectionUtil<Id<VehicleType>> cutilt = new CollectionUtil<Id<VehicleType>>();
	        Map<Id<VehicleType>, VehicleType> vehtypes = vehicles.getVehicleTypes();
	        ArrayList<Id<VehicleType>> vtlist = cutilt.toArrayList(vehtypes.keySet().iterator());
	        i=0;size = vtlist.size();
			while(i<size){
				vehicles.removeVehicleType(vtlist.get(i));i++;
			}
	        TransitSchedule schedule = scenario.getTransitSchedule();
	        CollectionUtil<Id<TransitStopFacility>> cutilsf = new CollectionUtil<Id<TransitStopFacility>>();
	        Map<Id<TransitStopFacility>, TransitStopFacility> stopfacilities = schedule.getFacilities();
	        ArrayList<Id<TransitStopFacility>> sflist =  cutilsf.toArrayList(stopfacilities.keySet().iterator());
	        i=0;size = sflist.size();
	      //Remove all PT stops
			while(i<size){
				TransitStopFacility stopfacility = stopfacilities.get(sflist.get(i));
				if(stopfacility!=null)schedule.removeStopFacility(stopfacility);i++;
			}
			//Remove all transit lines
	        CollectionUtil<Id<TransitLine>> cutiltr = new CollectionUtil<Id<TransitLine>>();
	        ArrayList<Id<TransitLine>> lines = cutiltr.toArrayList(schedule.getTransitLines().keySet().iterator());
	        i=0;size=lines.size();
	        while(i<size){
	        	TransitLine tline = schedule.getTransitLines().get(lines.get(i));
	        	if(tline!=null)schedule.removeTransitLine(tline);i++;
	        }
	}
	/*Deep copy and add all stop facilities and transit lines from a stand alone updated transit schedule 
	 * into the current scenario's transit schedule.
	 */
	public void addTransitSchedule(Scenario scenario, TransitSchedule copiedschedule){
		TransitSchedule tschedule = scenario.getTransitSchedule();
		TransitScheduleFactoryImpl tschedulefact = new TransitScheduleFactoryImpl();
		Map<Id<TransitStopFacility>, TransitStopFacility> stopfacilities = copiedschedule.getFacilities();
		Iterator<Id<TransitStopFacility>> stopsiterator =  stopfacilities.keySet().iterator();
		while(stopsiterator.hasNext()){
			TransitStopFacility stopfacility = stopfacilities.get(stopsiterator.next());
			tschedule.addStopFacility(stopfacility);
		}
		
		//Deep copy and add all lines into current scenario
		Map<Id<TransitLine>, TransitLine> lines = copiedschedule.getTransitLines();
		Iterator<Id<TransitLine>> linesiterator =  lines.keySet().iterator();
		while(linesiterator.hasNext()){
			TransitLine tline = lines.get(linesiterator.next());
			TransitLine newline = tschedulefact.createTransitLine(Id.create(tline.getId().toString(), TransitLine.class));
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			Iterator<Id<TransitRoute>> routesiterator =  routes.keySet().iterator();
			while(routesiterator.hasNext()){
				TransitRoute troute = routes.get(routesiterator.next());
				TransitRoute newroute = tschedulefact.createTransitRoute(Id.create(troute.getId().toString(), TransitRoute.class), troute.getRoute(), troute.getStops(), troute.getTransportMode());
				Map<Id<Departure>, Departure> departures = troute.getDepartures();
				Iterator<Id<Departure>> depsiterator =  departures.keySet().iterator();
				while(depsiterator.hasNext()){
					Departure departure = departures.get(depsiterator.next());
					Departure newdeparture = tschedulefact.createDeparture(Id.create(departure.getId().toString(), Departure.class), departure.getDepartureTime());
					newdeparture.setVehicleId(departure.getVehicleId());
					newroute.addDeparture(newdeparture);
				}
				newline.addRoute(newroute);
			}
			tschedule.addTransitLine(newline);
		}
	}
	/*Add all vehicle types and vehicles from an updated stand alone vehicles object 
	 * into the current scenario's vehicles object. Vehicles not deep copied.
	 */
	
	public void addVehicles(Scenario scenario, Vehicles copiedvehicles){
		Vehicles tvehicles = scenario.getTransitVehicles();
		//Add all vehicle types
		Map<Id<VehicleType>, VehicleType> vehtypes = copiedvehicles.getVehicleTypes();
		Iterator<Id<VehicleType>> vtiterator = vehtypes.keySet().iterator();
		while(vtiterator.hasNext()){
			VehicleType vt = vehtypes.get(vtiterator.next());
			tvehicles.addVehicleType(vt);
		}
		//Add all vehicle instances
		Map<Id<Vehicle>, Vehicle> vehinstances = copiedvehicles.getVehicles();
		Iterator<Id<Vehicle>> vehsiterator = vehinstances.keySet().iterator();
		while(vehsiterator.hasNext()){
			Vehicle veh = vehinstances.get(vehsiterator.next());
			tvehicles.addVehicle(veh);
		}
	}
	/*Vehicles not serving any line, and allocated to after-simulation-end-time hypothetical departures.
	 * One hypothetical after-simulation-end-time  departure must exist in empty routes, 
	 * as totally empty route is not allowed in MatSIm by design. 
	 */
	public int getUnusedVehs(TransitSchedule schedule){//Vehicles after simulation end time
		int unusedvehs=0;
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Iterator<Id<TransitLine>> linesiterator =  lines.keySet().iterator();
		while(linesiterator.hasNext()){
			TransitLine tline = lines.get(linesiterator.next());
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			Iterator<Id<TransitRoute>> routesiterator =  routes.keySet().iterator();
			while(routesiterator.hasNext()){
				TransitRoute troute = routes.get(routesiterator.next());
				Map<Id<Departure>, Departure> departures = troute.getDepartures();
				Iterator<Id<Departure>> depsiterator =  departures.keySet().iterator();
				while(depsiterator.hasNext()){
					Departure departure = departures.get(depsiterator.next());
					if(departure.getDepartureTime()==115200){//Post simulation end time of 30:00
						unusedvehs++;
					}
				}
			}
		}
	
		return unusedvehs;
	}
	//Write relevant stats for checking that PT Optimisation is behaving plausibly.
	public void writePlausibilityStatistics(Scenario scenario, int iteration){
		PlausibilityStatistics plc = new PlausibilityStatistics(scenario);
		if(iteration==0){
			plc.accumulateStatistics();
			plc.writeStatistics();
			return;
		}
		plc.accumulateStatistics();
	}
	
	//Create an Arraylist from an iterator.
	public class CollectionUtil<Type> {
		public ArrayList<Type> toArrayList(Iterator<Type> iter){
			ArrayList<Type> arraylist = new ArrayList<Type>();
			while(iter.hasNext()){
				arraylist.add(iter.next());
			}
			return arraylist;
		}
	}
}
