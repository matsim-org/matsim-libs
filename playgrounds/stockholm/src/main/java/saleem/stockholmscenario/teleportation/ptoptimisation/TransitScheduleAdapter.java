package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
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
			if(Math.random()<=0.1){//With 10% probability
//				if(Math.random()<=0.5){//With 50% probability
					vehadder.addDeparturesToLine(tline, 0.2);//Adds 10 % departures and corresponding vehicles from tline
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
					vehremover.removeDeparturesFromLine(tline, 0.2);//Removes  10 % departures and corresponding vehicles from tline
//				}
			}
		}
		return new PTSchedule(scenario, schedule, vehicles);
	}
	
	//With factorline *100 % chance of selecting a line, and factorroute*100 % chance of removing each of its route for removal
	public PTSchedule updateScheduleDeleteRoute(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		RouteAdderRemover routeremover = new RouteAdderRemover();
		routeremover.deleteRandomRoutes(schedule, vehicles, factorline, factorroute);
//		writeSchedule(schedule, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\TransitScheduleRoutesRemoved.xml");
		return new PTSchedule(scenario, schedule, vehicles);
	}
	/*With factorline *100 % chance of selecting a line, and factorroute*100 % chance of adding a new route for each route. 
	 * It is called with 2X probablity as compared to removing a route.  One X for balancing the deleted routes, one X for 
	 * creating  a variation with added routes. 
	 * It should be called with vehicles and transit schedule objects that have already been varied by removing routes. 
	 * We have to work like this due to the way plans are memorised in the optimisation process.
	 */
	public PTSchedule updateScheduleAddRoute(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		RouteAdderRemover routeadder = new RouteAdderRemover();
		routeadder.addRandomRoutes(scenario, schedule, vehicles, factorline, factorroute);
		writeSchedule(schedule, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\TransitScheduleRoutesAdded.xml");
//		NetworkWriter networkWriter =  new NetworkWriter(scenario.getNetwork());
//		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\RouteAddedNetwork.xml");
		return new PTSchedule(scenario, schedule, vehicles);
	}
	//Increase capacity of vehicles in Peakhour, decrease in quiet hours
	//Make sure to change the small and large bus capacity when running with a 10% population
	public PTSchedule updateScheduleChangeCapacity(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		PTCapacityAdjuster capadj = new PTCapacityAdjuster();
		capadj.adjustCapacity(vehicles, schedule, factorline, factorroute);
//		VehicleWriterV1 vehwriter = new VehicleWriterV1(vehicles);
//		vehwriter.writeFile("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\VehiclesCapacityChanged.xml");
		return new PTSchedule(scenario, schedule, vehicles);
	}
	
	//With factorline *100 % chance of selecting a line, and factorroute*100 % chance of removing each of its route for removal
		public PTSchedule updateScheduleDeleteLines(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline){
			LineAdderRemover lar = new LineAdderRemover();
			lar.deleteRandomLines(schedule, vehicles, factorline);
//			writeSchedule(schedule, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\TransitScheduleLinesRemoved.xml");
			return new PTSchedule(scenario, schedule, vehicles);
		}
		//With factorline *100 % chance of selecting a line, and factorroute*100 % chance of removing each of its route for removal
		public PTSchedule updateScheduleAddLines(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline){
			LineAdderRemover lar = new LineAdderRemover();
			lar.addRandomLines(scenario, schedule, vehicles, factorline);
//			writeSchedule(schedule, "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\TransitScheduleLinesAdded.xml");
			return new PTSchedule(scenario, schedule, vehicles);

		}
		
	public void writeSchedule(TransitSchedule schedule, String path){
		
		TransitScheduleWriter tw = new TransitScheduleWriter(schedule);
		tw.writeFile(path);
	}
	public void writeVehicles(Vehicles vehicles, String path){
		VehicleWriterV1 vwriter = new VehicleWriterV1(vehicles);
		vwriter.writeFile(path);
	}
}
