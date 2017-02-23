package saleem.ptoptimisation.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

/**
 * This class is to extract and return statistics,
 *  which are used as a sanity check on PT optimisation.
 *  
 * @author Mohammad Saleem
 */
public class PlausibilityStatistics {
	private final Scenario scenario;
	private static  String stats = "Iteration" + "\t" + "Avg. Utility" + "\t" + "Avg. PT Utility" + "\t" + "PT Total" + "\t" + "Avg. Car Utility"
			 + "\t" + "Car Total" + "\t" + "Number of Departures" + "\t" +"Number Of Routes" + "\t" + "Number of Lines" + "\t" + "Total Capacity" + "\n";
	private static int totaliterations=0;
	public PlausibilityStatistics(Scenario scenario){
		this.scenario=scenario;
	}
	//Accumulates plausibility statistics for one iteration.
	public void accumulateStatistics(){
		if(totaliterations>0){
			stats=stats + totaliterations + "\t";
			stats= stats + getUtilityStatistics() + "\t";
			stats= stats + getNumberOfDeparturesRoutesLines() + "\t";
			stats= stats + getTotalCapacity() + "\n";
		}
		totaliterations++;
	}
	/*Writes plausibility statistics to a text file. 
	 * Called usually every time a new decision variable is selected as the best decision variable by Optimisation algorithm.
	 */
	public void writeStatistics(){
		String path = scenario.getConfig().controler().getOutputDirectory();
		path=path.substring(0, path.lastIndexOf('/'))+"/plausibilitychecks.txt";
		try { 
			File file=new File(path);
		    FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(stats.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here.
	    }
	}
	//Returns average utility for general population, PT users and Car users
	public String getUtilityStatistics(){
		Iterator<? extends Person> personiter = scenario.getPopulation().getPersons().
				values().iterator();
		int total=0, totalPT=0,totalCar=0;
		double avgUtility = 0;
		double avgsCarUtility = 0;
		double avgPTUtility = 0;
		while(personiter.hasNext()){
			total++;
			Person person = personiter.next();
			Plan plan = person.getSelectedPlan();
			avgUtility+=person.getSelectedPlan().getScore();
			if(PopulationUtils.hasCarLeg(plan)){
				avgsCarUtility+=person.getSelectedPlan().getScore();
				totalCar++;
			}else{
				avgPTUtility+=person.getSelectedPlan().getScore();
				totalPT++;
			}
		}
		avgUtility/=total;
		avgPTUtility/=totalPT;
		avgsCarUtility/=totalCar;
		String str = avgUtility + "\t" + avgPTUtility + "\t" +  totalPT + "\t" + avgsCarUtility + "\t" + totalCar;
		return str;
	}
	//Returns total number of seats and standing room (capacity statistics) in the transit system.
	public double getTotalCapacity(){
		int totalCap=0;
		Iterator<Vehicle> vehiter = scenario.getTransitVehicles().getVehicles().values().iterator();
		while(vehiter.hasNext()){
			Vehicle vehicle = vehiter.next();
			VehicleCapacity capacity = vehicle.getType().getCapacity();
			totalCap+=(capacity.getSeats()+capacity.getStandingRoom()); 
		}
		return totalCap;	
	}
	// return the number of departures, number of routes and number of lines in the transit system
	public String getNumberOfDeparturesRoutesLines(){
		int numlines=0, numroutes=0, numdepartures=0;
		TransitSchedule schedule = scenario.getTransitSchedule();
		Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		Iterator<TransitLine> linesiterator =  lines.values().iterator();
		while(linesiterator.hasNext()){
			boolean emptyline=true;
			TransitLine tline = linesiterator.next();
			Map<Id<TransitRoute>, TransitRoute> routes = tline.getRoutes();
			Iterator<TransitRoute> routesiterator =  routes.values().iterator();
			while(routesiterator.hasNext()){
				TransitRoute troute = routesiterator.next();
				if(!(troute.getDepartures().values().iterator().next().getDepartureTime()==115200 && 
						troute.getDepartures().size()==1)){////Excluding routes with no departures between 00:00 and 30:00
					emptyline=false;
					numdepartures+=troute.getDepartures().size();
					numroutes+=1;
				}
			}
			if(!emptyline){//Only count lines with serving routes
				numlines+=1;
			}
		}
		return numdepartures + "\t" + numroutes + "\t" + numlines;
	}
}
