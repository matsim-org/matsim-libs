package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;
import java.util.*;

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
		String populationInput = "E:/MA/workspace.bak/master/output/siouxfalls-2014/output_plans.xml";
		String vehiclesInput = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_vehicles.xml";
		String transitScheduleInput = "E:/MA/workspace/matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule.xml";
		String output = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestFullSumo_.xml";
		String populationOutput = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestSorted.xml"; //if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
		
		List<Person> persons = new ArrayList<>();
		Map<Id<Vehicle>, Vehicle> vehicles = new HashMap<>();
		List<VehicleType> vehicleTypes = new ArrayList<>();
		
		
		TqMatsimVehiclesReader vr = new TqMatsimVehiclesReader(vehiclesInput);
		vehicles = vr.getVehicles();
		vehicleTypes = vr.getVehicleTypes();
		
		TqMatsimTransitScheduleReader tsr = new TqMatsimTransitScheduleReader(transitScheduleInput, configFileName);
		TransitSchedule transitSchedule = tsr.getTransitSchedule();
		
		TqMatsimPlansReader pr = new TqMatsimPlansReader(populationInput);
		persons = pr.getPlans();
		System.out.println("getPlans completed");

        List<Person> personsSorted = new ArrayList<Person>(persons); //id's sorted by end_times of first activity of selectedPlans
        Collections.sort(personsSorted, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                return Double.compare(firstActivityEndTime(o1), firstActivityEndTime(o2));
            }

            private double firstActivityEndTime(Person o1) {
                return ((Activity) o1.getSelectedPlan().getPlanElements().get(0)).getEndTime();
            }
        });


        //if inputPopulation (input) NOT already sorted by end_times of first activity of selectedPlans:
//		personsSorted = pr.sortPlans(persons);
//		System.out.println("sortPlans completed");
//		pr.writeSortedPopulation(pr.getSortedPopulation(), populationOutput);
//		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(personsSorted, vehicleTypes, vehicles, transitSchedule, output);
		
	//else if inputPopulation (input) already sorted by end_times of activity of selectedPlans:
		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(personsSorted, vehicleTypes, vehicles, transitSchedule, output);
		
		routesWriter.writeFile();
	}

}

