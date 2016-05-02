package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.MutableScenario;
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

import saleem.stockholmscenario.utils.CollectionUtil;
import floetteroed.opdyts.DecisionVariable;

public class PTSchedule implements DecisionVariable{
	TransitSchedule schedule;
	Vehicles vehicles;
	Scenario scenario;
	public PTSchedule(Scenario scenario, TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.vehicles=vehicles;
		this.scenario=scenario;
		
	}
	//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
	public void removeEntireScheduleAndVehicles(){
		
		Vehicles vehicles = this.scenario.getTransitVehicles();
		//Add all vehicle types
		
		CollectionUtil<Id<Vehicle>> cutil = new CollectionUtil<Id<Vehicle>>();
		 ArrayList<Id<Vehicle>> list = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());//Converted to array list in order to avoid concurrent modification issues.
	        int i=0;int size = list.size();
	        while(i<size){
	        	vehicles.removeVehicle(list.get(i));i++;
	        }
	        //Vehicles must be removed before removing the vehicle types
	        CollectionUtil<Id<VehicleType>> cutilt = new CollectionUtil<Id<VehicleType>>();
	        Map<Id<VehicleType>, VehicleType> vehtypes = vehicles.getVehicleTypes();
	        ArrayList<Id<VehicleType>> vtlist = cutilt.toArrayList(vehtypes.keySet().iterator());
	        i=0;size = vtlist.size();
			while(i<size){
				vehicles.removeVehicleType(vtlist.get(i));i++;
			}
	        TransitSchedule schedule = this.scenario.getTransitSchedule();
	        CollectionUtil<Id<TransitStopFacility>> cutilsf = new CollectionUtil<Id<TransitStopFacility>>();
	        Map<Id<TransitStopFacility>, TransitStopFacility> stopfacilities = schedule.getFacilities();
	        ArrayList<Id<TransitStopFacility>> sflist =  cutilsf.toArrayList(stopfacilities.keySet().iterator());
	        i=0;size = sflist.size();
			while(i<size){
				TransitStopFacility stopfacility = stopfacilities.get(sflist.get(i));
				if(stopfacility!=null)schedule.removeStopFacility(stopfacility);i++;
			}
	        CollectionUtil<Id<TransitLine>> cutiltr = new CollectionUtil<Id<TransitLine>>();
	        ArrayList<Id<TransitLine>> lines = cutiltr.toArrayList(schedule.getTransitLines().keySet().iterator());
	        i=0;size=lines.size();
	        while(i<size){
	        	TransitLine tline = schedule.getTransitLines().get(lines.get(i));
	        	if(tline!=null)schedule.removeTransitLine(tline);i++;
	        }
	}
	//Adds all stop facilities and transit lines from a stand alone updated transit schedule into the current scenario transit schedule
	public void addTransitSchedule(TransitSchedule copiedschedule){
		TransitSchedule tschedule = this.scenario.getTransitSchedule();
		TransitScheduleFactoryImpl tschedulefact = new TransitScheduleFactoryImpl();
		Map<Id<TransitStopFacility>, TransitStopFacility> stopfacilities = copiedschedule.getFacilities();
		Iterator<Id<TransitStopFacility>> stopsiterator =  stopfacilities.keySet().iterator();
		while(stopsiterator.hasNext()){
			TransitStopFacility stopfacility = stopfacilities.get(stopsiterator.next());
			tschedule.addStopFacility(stopfacility);
		}
		
		//Deep copy and add all lines
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
	//Adds all vehicle types and vehicles from an updated stand alone vehicles object into the current scenario vehicles object
	
	public void addVehicles(Vehicles copiedvehicles){
		Vehicles tvehicles = this.scenario.getTransitVehicles();
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
	/*
	 * The implementInSimulation function updates the transit schedule and vehicles objects associated with the current main scenario. 
	 * Empty the transit schedule and vehicles objects by removing all existing vehicle types, vehicles, stop facilities and transit lines, 
	 * and add back from an updated vehicles object and transit schedule object into the transit schedule and vehicles object associated with the scenario.
	 * The updated vehicles object and transit schedule object are the ones that are produced by the PTScheduleRandomiser by adding and deleting 
	 * vehicles and departures to the selected transit schedule.
	*/
	@Override
	public void implementInSimulation() {
		
		TransitScheduleAdapter adapter = new TransitScheduleAdapter();
		TransitSchedule copiedschedule = adapter.deepCopyTransitSchedule(schedule);
		Vehicles copiedvehicles = adapter.deepCopyVehicles(vehicles);
		removeEntireScheduleAndVehicles();//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
		addVehicles(copiedvehicles);//Adds all vehicle types and vehicles from an updated stand alone vehicles object into the current scenario vehicles object
		addTransitSchedule(copiedschedule);//Add all stop facilities and transit lines from a stand alone updated transit schedule into the current scenario transit schedule
		
	}

}
