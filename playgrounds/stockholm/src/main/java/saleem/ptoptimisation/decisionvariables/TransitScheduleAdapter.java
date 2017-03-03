package saleem.ptoptimisation.decisionvariables;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import saleem.ptoptimisation.optimisationintegration.PTSchedule;
import saleem.stockholmmodel.utils.CollectionUtil;
/**
 * A helper class to create variations of transit schedule.
 * 
 * @author Mohammad Saleem
 *
 */
public class TransitScheduleAdapter {
	/*With factorline*100 % probability of selecting a line, 
	 * and factordeparture*100 % probability for selecting each departure serving the line for addition
	 * For each selected departure, a new independent departure is added to corresponding route of the line.
	 */
	public PTSchedule updateScheduleAddDepartures(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factordeparture){
		CollectionUtil<TransitLine> cutil = new CollectionUtil<TransitLine>();
		VehicleAdder vehadder = new VehicleAdder(vehicles, schedule);
		ArrayList<TransitLine> lines = cutil.toArrayList(schedule.getTransitLines().values().iterator());
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(Math.random()<=factorline){//With 10% probability
			vehadder.addDeparturesToLine(tline, factordeparture);//Adds 10 % departures and corresponding vehicles from tline
			}
		}
		return new PTSchedule(scenario, schedule, vehicles);
	}
	/*With factorline*100 % probability of selecting a line, 
	 * and factordeparture*100 % probability for selecting each departure serving the line for deletion
	 * selected departures are deleted from corresponding routes of the line.
	 */
	public PTSchedule updateScheduleRemoveDepartures(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factordeparture){
		CollectionUtil<TransitLine> cutil = new CollectionUtil<TransitLine>();
		VehicleRemover vehremover = new VehicleRemover(vehicles, schedule);
		ArrayList<TransitLine> lines = cutil.toArrayList(schedule.getTransitLines().values().iterator());
		int size = lines.size();
		for(int i=0;i<size;i++) {
			TransitLine tline = lines.get(i);
			if(Math.random()<=factorline){//With 5% probability
				vehremover.removeDeparturesFromLine(tline, factordeparture);//Removes  10 % departures and corresponding vehicles from tline
			}
		}
		return new PTSchedule(scenario, schedule, vehicles);
	}
	
	//With factorline *100 % probability of selecting a line, and factorroute*100 % probability of removing each route of a selected line
	public PTSchedule updateScheduleDeleteRoute(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		RouteAdderRemover routeremover = new RouteAdderRemover();
		routeremover.deleteRandomRoutes(schedule, vehicles, factorline, factorroute);
		return new PTSchedule(scenario, schedule, vehicles);
	}
	/*With factorline *100 % chance of selecting a line, and factorroute*100 % chance of adding a new route for each route in the selected line. */
	public PTSchedule updateScheduleAddRoute(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		RouteAdderRemover routeadder = new RouteAdderRemover();
		routeadder.addRandomRoutes(scenario, schedule, vehicles, factorline, factorroute);
		return new PTSchedule(scenario, schedule, vehicles);
	}
	/*With factorline*100 % probability of selecting a line, 
	 * and factorroute*100 % probability for selecting each of its routes
	 * Increase capacity of vehicles in Peakhour, decrease in quiet hours, in the selected routes.
	 */
	public PTSchedule updateScheduleChangeCapacity(Scenario scenario, Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		PTCapacityAdjuster capadj = new PTCapacityAdjuster();
		capadj.adjustCapacity(vehicles, schedule, factorline, factorroute);
		return new PTSchedule(scenario, schedule, vehicles);
	}
	
	//Each line is selected with factorline *100 % probability of deletion
	public PTSchedule updateScheduleDeleteLines(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline){
		LineAdderRemover lar = new LineAdderRemover();
		lar.deleteRandomLines(schedule, vehicles, factorline);
		return new PTSchedule(scenario, schedule, vehicles);
	}
	/*Each line is selected with factorline *100 % probability for addition
	*   For each selected line, a new independent line is added
	**/
	public PTSchedule updateScheduleAddLines(Scenario scenario, TransitSchedule schedule, Vehicles vehicles, double factorline){
		LineAdderRemover lar = new LineAdderRemover();
		lar.addRandomLines(scenario, schedule, vehicles, factorline);
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
