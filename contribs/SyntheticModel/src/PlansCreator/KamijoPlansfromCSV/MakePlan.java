package PlansCreator.KamijoPlansfromCSV;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MakePlan {

	/*
	 * Creates as subclass of the abstract class Id<Person> to generate IDs
	 */
	public class genID extends Id<Person> {
		@Override
		public int index() {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	public static void main(String args[]) {

		Scenario scenario = createPopulationFromCensusFile("scenarios/equil/NYCTaxi1_137.csv");
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("scenarios/equil/NYCTaxi1_137.xml");

	}


	private static Scenario createPopulationFromCensusFile(String censusFile)	{
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		/*
		 * Use Parser to read the census sample file.
		 */
		List<MakePlanEntry> censusEntries = new MakePlanParser().readFile(censusFile);
		/*
		 * Get Population and PopulationFactory objects.
		 * The Population contains all created Agents and their plans,
		 * the PopulationFactory should be used to create Plans,
		 * Activities, Legs and so on.
		 */
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		/*
		 * The census file contains one line per trip, meaning a typical person
		 * is represented by multiple lines / trips. Therefore in a first step
		 * we have to sort the trips based on the person who executes them.
		 */

		/*
		 * Create a Map with the PersonIds as key and a list of CensusEntry as values.
		 */
		Map<Integer, List<MakePlanEntry>> personEntryMapping = new TreeMap<Integer, List<MakePlanEntry>>();
		for (MakePlanEntry censusEntry : censusEntries) {
			/*
			 * If the Map already contains an entry for the current person
			 * the list will not be null.
			 */
			List<MakePlanEntry> entries = personEntryMapping.get(censusEntry.id_person);
			/*
			 * If no mapping exists -> create a new one
			 */
			if (entries == null) {
				entries = new ArrayList<MakePlanEntry>();
				personEntryMapping.put(censusEntry.id_person, entries);
			}
			/*
			 *  Add currently processed entry to the list
			 */
			entries.add(censusEntry);
		}

		/*
		 * Now create a plan for each person - iterate over all entries in the map.
		 */
		for (List<MakePlanEntry> personEntries : personEntryMapping.values()) {
			/*
			 * Get the first entry from the list - it will never be null.
			 */
			MakePlanEntry entry = personEntries.get(0);
			/*
			 * Get id of the person from the censusEntry.
			 */
			int id_person = entry.id_person;
			/*
			 * Create new person and add it to the population.
			 * Use scenario.createId(String id) to create the Person's Id.
			 */
			Person person = populationFactory.createPerson(genID.createPersonId(id_person));
			population.addPerson(person);
			/*
			 *  Create new plan and add it to the person.
			 */
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			/*
			 * Every Agent has at least one activity which is being at home.
			 * - set the activity type to "home"
			 * - set the start time to 0.0
			 * - add the Activity to the plan.
			 */
			Coord homeCoord =  new Coord(entry.s_x, entry.s_y);
			Activity homeActivity = populationFactory.createActivityFromCoord("dummy", homeCoord);
			homeActivity.setStartTime(0.0);
			plan.addActivity(homeActivity);
			/*
			 * Create objects that are needed when creating the other
			 * Activities and Legs of the Plan.
			 *
			 * Mind that we have to set a start and end time for each Activity
			 * (except the last one - it will last until the end of the simulated
			 * period). The end time of an Activity equals the departure time of
			 * the next Trip. We set the end time of an Activity when we process
			 * the next Trip by using a point to the last previously created
			 * Activity (initially this is the Home Activity).
			 */
			Coord endCoord = null;
			String transportMode = null;
			Leg leg = null;
			Activity activity = null;
			Activity previousActivity = homeActivity;
			/*
			 *  adding license issues
			 */
			/*
			if(entry.car == 0) {
				person.getAttributes().putAttribute("carAvail","never");
			}
			if(entry.bike == 0) {
				person.getAttributes().putAttribute("bicycAvail","never");
			}
			*/
			/*
			 *  Create person's Trips and add them to the Plan.
			 */
			for (MakePlanEntry personEntry : personEntries) {
				endCoord = new Coord(personEntry.d_x, personEntry.d_y);
				transportMode = "drt";
				String activityType = "dummy";

				/*
				 * Create a new Leg using the PopulationFactory and set its parameters.
				 * Mind that MATSim uses seconds as time unit whereas the census uses minutes.
				 */
				leg = populationFactory.createLeg(transportMode);
				leg.setDepartureTime(personEntry.starttime);
				leg.setTravelTime(10 * 60);// デフォルトの移動時間を10分固定とした
				previousActivity.setEndTime(personEntry.starttime);

				/*
				 * Create a new Activity using the Population Factory and set its parameters.
				 */
				activity = populationFactory.createActivityFromCoord(activityType, endCoord);
				activity.setStartTime(personEntry.starttime+ 10 * 60);

				/*
				 * Add the Leg and the Activity to the plan.
				 */
				plan.addLeg(leg);
				plan.addActivity(activity);

				/*
				 * Do not forget to update the pointer to the previousActivity.
				 */
				//previousActivity = activity;
			}

			/*
			 * ... and finally: If the last Activity takes place at the Home Coordinates
			 * we assume that the Agent is performing a "home" Activity.
			 */
			/*
			if (activity.getCoord().equals(homeCoord)) {
				activity.setType("home");
			}
			*/
		}
		return scenario;

	}

	/*
	 * Helper methods that convert the entries from the census file.
	 */

}
