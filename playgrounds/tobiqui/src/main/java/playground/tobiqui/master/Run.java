package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
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
		/*String configFileName = "../matsim/examples/siouxfalls-2014/config_default.xml";
		String populationInput = "../matsim/output/siouxfalls-2014/output_plans.xml.gz";
		String vehiclesInput = "../matsim/examples/siouxfalls-2014/Siouxfalls_vehicles.xml";
		String transitScheduleInput = "../matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule.xml";
		String outputRoutes = "../matsim/output/siouxfalls-2014/TestFullSumo_.rou.xml";
		String outputAdditional = "../matsim/output/siouxfalls-2014/input_additional.add.xml"; //additional data like busstops
*/
		String configFileName = "E:/MA/workspaceMars/matsim/examples/siouxfalls-2014/config_renamed.xml";
		String populationInput = "../../matsim/output/siouxfalls-2014/output_plans.xml.gz";
		String vehiclesInput = "../../matsim/examples/siouxfalls-2014/Siouxfalls_vehicles.xml";
		String transitScheduleInput = "../../matsim/examples/siouxfalls-2014/Siouxfalls_transitSchedule_renamed.xml";
		String outputRoutes = "../../matsim/output/siouxfalls-2014/TestFullSumo_.rou.xml";
		String outputAdditional = "../../matsim/output/siouxfalls-2014/input_additional.add.xml"; //additional data like busstops
		
		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.createScenario(config);


		Vehicles v = VehicleUtils.createVehiclesContainer();
		new VehicleReaderV1(v).readFile(vehiclesInput);

		Map<Id<Vehicle>, Vehicle> vehicles = v.getVehicles();
		Collection<VehicleType> vehicleTypes = v.getVehicleTypes().values();


		new TransitScheduleReader(scenario).readFile(transitScheduleInput);

		new MatsimPopulationReader(scenario).readFile(populationInput);

		int i=0;
		Iterator<? extends Person> iterator = scenario.getPopulation().getPersons().values().iterator();
		while (iterator.hasNext()) {
			iterator.next();
			if (i % 5 != 0) {
				iterator.remove();
			}
			i++;
		}
	
		List<Person> personsSorted = new ArrayList<Person>(scenario.getPopulation().getPersons().values()); //id's sorted by end_times of first activity of selectedPlans
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (Departure departure : transitRoute.getDepartures().values()) {
					Id<Person> vehId = Id.createPersonId(departure.getVehicleId().toString());
					Person person = scenario.getPopulation().getFactory().createPerson(vehId);
					PlanImpl plan = new PlanImpl();
					plan.createAndAddActivity("").setEndTime(departure.getDepartureTime());
					person.addPlan(plan);
					personsSorted.add(person);
				}
			}
		}
	
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

