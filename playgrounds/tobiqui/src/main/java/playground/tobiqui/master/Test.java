package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */

/**
 * @author tquick
 *
 */
public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String configFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/config_default.xml";
		String populationInput = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestFullSorted.xml";
		String vehiclesInput = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_vehicles.xml";
		String transitScheduleInput = "E:/MA/workspace/matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule.xml";
		String output = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestFullSumo.xml";
		String populationOutput = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestSorted.xml"; //if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
		
		Map<Id<Person>, Person> persons = new HashMap<Id<Person>, Person>(); 
		Map<Id<Person>, Person> personsSorted = new LinkedHashMap<Id<Person>, Person>(); //id's sorted by end_times of first activity of selectedPlans 
		Map<Id<Vehicle>, Vehicle> vehicles = new HashMap<Id<Vehicle>, Vehicle>();
		Map<Id<VehicleType>, VehicleType> vehicleTypes = new HashMap<Id<VehicleType>, VehicleType>();
		
		
		TqMatsimVehiclesReader vr = new TqMatsimVehiclesReader(vehiclesInput);
		vehicles = vr.getVehicles();
		vehicleTypes = vr.getVehicleTypes();
		
		TqMatsimTransitScheduleReader tsr = new TqMatsimTransitScheduleReader(transitScheduleInput, configFileName);
		TransitSchedule transitSchedule = tsr.getTransitSchedule();
		
		TqMatsimPlansReader pr = new TqMatsimPlansReader(populationInput);
		persons = pr.getPlans();
		System.out.println("getPlans completed");
		
	//if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
//		personsSorted = pr.sortPlans(persons);
//		System.out.println("sortPlans completed");
//		pr.writeSortedPopulation(pr.getSortedPopulation(), populationOutput);
//		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(personsSorted, vehicleTypes, vehicles, transitSchedule, output);
		
	//else if inputPopulation (input) already sorted by end_times of activity of selectedPlans:
		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(persons, vehicleTypes, vehicles, transitSchedule, output); 
		
		routesWriter.writeFile();
	}

}

