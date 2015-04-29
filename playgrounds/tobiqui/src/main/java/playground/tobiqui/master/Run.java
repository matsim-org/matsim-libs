package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.*;

import java.io.IOException;
import java.util.*;

/**
 * 
 */

/**
 * @author tquick
 *
 */
public class Run {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String configFileName = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/config_renamed.xml";
		String populationInput = "E:/MA/workspace.bak/matsim/output/siouxfalls-2014_renamed/output_plans.xml.gz";
		String vehiclesInput = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_vehicles.xml";
		String transitScheduleInput = "E:/MA/workspace.bak/matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule_renamed.xml";
		String outputRoutes = "E:/MA/workspace.bak/master/output/siouxfalls-2014/TestFullSumo_.rou.xml";
		String outputAdditional = "E:/MA/workspace.bak/master/output/siouxfalls-2014/input_additional.add.xml"; //additional data like busstops

		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.createScenario(config);


		Vehicles v = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(v).readFile(vehiclesInput);

		Map<Id<Vehicle>, Vehicle> vehicles = v.getVehicles();
		Collection<VehicleType> vehicleTypes = v.getVehicleTypes().values();


		new TransitScheduleReader(scenario).readFile(transitScheduleInput);

		new MatsimPopulationReader(scenario).readFile(populationInput);


        List<Person> personsSorted = new ArrayList<Person>(scenario.getPopulation().getPersons().values()); //id's sorted by end_times of first activity of selectedPlans
        Collections.sort(personsSorted, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                return Double.compare(firstActivityEndTime(o1), firstActivityEndTime(o2));
            }

            private double firstActivityEndTime(Person o1) {
                return ((Activity) o1.getSelectedPlan().getPlanElements().get(0)).getEndTime();
            }
        });


		TqSumoRoutesWriter routesWriter = new TqSumoRoutesWriter(personsSorted, vehicleTypes, vehicles, scenario.getTransitSchedule(), outputRoutes, outputAdditional);
		
		routesWriter.writeFiles();
	}

}

