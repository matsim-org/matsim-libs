package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.utils.CollectionUtil;

public class TransitScheduleAdapter {
	/*With 5% chance of selecting a line, and 50% chance of randomly adding and vehicles to it 
	 * and 50% chance of randomly deleting vehicles from it, and adjusting departure times.
	 */
	public TransitSchedule updateSchedule(Vehicles vehicles, TransitSchedule schedule){
		CollectionUtil<TransitLine> cutil = new CollectionUtil<TransitLine>();
		VehicleRemover vehremover = new VehicleRemover(vehicles, schedule);
		VehicleAdder vehadder = new VehicleAdder(vehicles, schedule);
		ArrayList<TransitLine> lines = cutil.toArrayList(schedule.getTransitLines().values().iterator());
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(Math.random()<=0.05){//With 5% probability
				if(Math.random()<=0.5){//With 50% probability
					vehadder.addDeparturesToLine(tline, 0.1);//Adds 10 % departures and corresponding vehicles from tline
				}
				else {
					vehremover.removeDeparturesFromLine(tline, 0.1);//Removes  10 % departures and corresponding vehicles from tline
				}
			}
		}
		return schedule;
	}
	//With 5% chance of selecting a line, and 50% chance of randomly adding vehicles to it and adjusting departure times.
	public PTSchedule updateScheduleAdd(Scenario scenario, Vehicles vehicles, TransitSchedule schedule){
		CollectionUtil<TransitLine> cutil = new CollectionUtil<TransitLine>();
		VehicleAdder vehadder = new VehicleAdder(vehicles, schedule);
		ArrayList<TransitLine> lines = cutil.toArrayList(schedule.getTransitLines().values().iterator());
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(Math.random()<=0.1){//With 5% probability
//				if(Math.random()<=0.5){//With 50% probability
					vehadder.addDeparturesToLine(tline, 0.1);//Adds 10 % departures and corresponding vehicles from tline
//				}
			}
		}
		return new PTSchedule(scenario, schedule, vehicles);
	}
	//With 5% chance of selecting a line, and 50% chance of randomly deleting vehicles from it and adjusting departure times.
	public PTSchedule updateScheduleRemove(Scenario scenario, Vehicles vehicles, TransitSchedule schedule){
		CollectionUtil<TransitLine> cutil = new CollectionUtil<TransitLine>();
		VehicleRemover vehremover = new VehicleRemover(vehicles, schedule);
		ArrayList<TransitLine> lines = cutil.toArrayList(schedule.getTransitLines().values().iterator());
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(Math.random()<=0.1){//With 5% probability
//				if(Math.random()<=0.5){//With 50% probability
					vehremover.removeDeparturesFromLine(tline, 0.1);//Removes  10 % departures and corresponding vehicles from tline
//				}
			}
		}
		return new PTSchedule(scenario, schedule, vehicles);
	}
	//With 5% chance of selecting a line, and 50% chance of randomly adding vehicles to it and adjusting departure times.
		public PTSchedule updateScheduleDeleteRoute(Scenario scenario, TransitSchedule schedule){
			CollectionUtil<TransitLine> cutilforlines = new CollectionUtil<TransitLine>();
			CollectionUtil<TransitRoute> cutilforroutes = new CollectionUtil<TransitRoute>();
			ArrayList<TransitLine> lines = cutilforlines.toArrayList(schedule.getTransitLines().values().iterator());
			int size = lines.size();
			for(int i=0;i<size;i++) {
				TransitLine tline = lines.get(i);
				if(Math.random()<=0.1){//With 10% probability
					ArrayList<TransitRoute> routes = cutilforroutes.toArrayList(tline.getRoutes().values().iterator());
					int sizer = routes.size();
					for(int j=0;j<sizer;j++) {
						TransitRoute troute = routes.get(j);
						if(Math.random()<=0.1){
							tline.removeRoute(troute);
						}
					}
				}
			}
			writeSchedule(schedule, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\AdaptedTransitSchedule.xml");
			return new PTSchedule(scenario, schedule, scenario.getTransitVehicles());
	}
	public void writeSchedule(TransitSchedule schedule, String path){
		
		TransitScheduleWriter tw = new TransitScheduleWriter(schedule);
		tw.writeFile(path);
	}
	public void writeVehicles(Vehicles vehicles, String path){
		VehicleWriterV1 vwriter = new VehicleWriterV1(vehicles);
		vwriter.writeFile(path);
	}
	/*This is a function to create a deep copy of transit schedule. The deep copy is not fully deep and may also copy references. All those components 
	of the transit schedule which are expected to be randomised for optimisation are deep copied. 
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
		//Deep copy and add all lines
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
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
	//Just for debugging purposes, no functional importance
//	public void removeEntireScheduleAndVehicles(){
//		Vehicles vehicles = this.scenario.getTransitVehicles();
//		CollectionUtil<Id<Vehicle>> cutil = new CollectionUtil<Id<Vehicle>>();
//		 ArrayList<Id<Vehicle>> list = cutil.toArrayList(vehicles.getVehicles().keySet().iterator());
//	        int i=0;int size = list.size();
//	        while(i<size){
//	        	vehicles.removeVehicle(list.get(i));i++;
//	        }
//	        
//	        CollectionUtil<Id<TransitLine>> cutil2 = new CollectionUtil<Id<TransitLine>>();
//	        TransitSchedule schedule = this.scenario.getTransitSchedule();
//	        ArrayList<Id<TransitLine>> lines = cutil2.toArrayList(schedule.getTransitLines().keySet().iterator());
//	        int j=0;int k=lines.size();
//	        while(j<k){
//	        	TransitLine tline = schedule.getTransitLines().get(lines.get(j));
//	        	if(tline!=null)schedule.removeTransitLine(tline);j++;
//	        }
//	}
	public static void main(String[] args){
		
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
        MatsimServices controler = new Controler(config);
        // Code below is for creating updated schedules, without running any simulation
        TransitScheduleAdapter adapter = new TransitScheduleAdapter();
        TransitSchedule scheduleadded = adapter.deepCopyTransitSchedule(controler.getScenario().getTransitSchedule());
        TransitSchedule scheduledeleted = adapter.deepCopyTransitSchedule(controler.getScenario().getTransitSchedule());
        TransitSchedule scheduleboth = adapter.deepCopyTransitSchedule(controler.getScenario().getTransitSchedule());
        Vehicles vehiclesboth = adapter.deepCopyVehicles(controler.getScenario().getTransitVehicles());
        Vehicles vehiclesadded = adapter.deepCopyVehicles(controler.getScenario().getTransitVehicles());
        Vehicles vehiclesdeleted = adapter.deepCopyVehicles(controler.getScenario().getTransitVehicles());

        adapter.updateSchedule(vehiclesboth, scheduleboth);
		adapter.writeSchedule(scheduleboth, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedSchedule.xml");
		adapter.writeVehicles(vehiclesboth, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedVehicles.xml");

        adapter.updateScheduleAdd(controler.getScenario(), vehiclesadded, scheduleadded);
		adapter.writeSchedule(scheduleadded, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedScheduleAdded.xml");
		adapter.writeVehicles(vehiclesadded, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedVehiclesAdded.xml");

        adapter.updateScheduleRemove(controler.getScenario(), vehiclesdeleted, scheduledeleted);
		adapter.writeSchedule(scheduledeleted, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedScheduleDeleted.xml");
		adapter.writeVehicles(vehiclesdeleted, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedVehiclesDeleted.xml");
		
		adapter.writeSchedule(scheduleboth, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedSchedule1.xml");
		adapter.writeVehicles(vehiclesboth, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\UpdatedVehicles1.xml");


	}
}
