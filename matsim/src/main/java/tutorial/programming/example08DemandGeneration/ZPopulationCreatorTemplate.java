package tutorial.programming.example08DemandGeneration;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.run.XY2Links;

public class ZPopulationCreatorTemplate {

	private Scenario scenario;
	
	public static void main(String args[])
	{
		ZPopulationCreator populationCreator = new ZPopulationCreator();
		
		/*
		 * Create population from sample input data.  
		 */
		populationCreator.createPopulation("../matsimExamples/tutorial/demandgeneration_zurich/input_sample_zurich.txt");
		
		/*
		 * Write population to file - mapping of the coordinates to links is not done yet.
		 */
		populationCreator.writePopulation("./input/population.xml");
		
		/*
		 * Map coordinates to links and write another population file.
		 */
//		populationCreator.createXY2LinkMapping("examples/tutorial/demandgeneration/config_demandgeneration.xml");
	}
	
	public ZPopulationCreatorTemplate()
	{
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void createPopulation(String censusFile)
	{
		/*
		 * Use Parser to read the census sample file.
		 */
		List<ZCensusEntry> censusEntries = new ZCensusParser().readFile(censusFile);
		
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
		Map<Integer, List<ZCensusEntry>> personEntryMapping = new TreeMap<Integer, List<ZCensusEntry>>();
		for (ZCensusEntry censusEntry : censusEntries)
		{

		}
				
		/*
		 * Now create a plan for each person - iterate over all entries in the map.
		 */
		for (List<ZCensusEntry> personEntries : personEntryMapping.values())
		{
			/*
			 * Get the first entry from the list - it will never be null.
			 */
			ZCensusEntry entry = personEntries.get(0);
			
			/*
			 * Get id of the person from the censusEntry.
			 */
			int id_person = entry.id_person;
			
			/*
			 * Create new person and add it to the population.
			 * Use scenario.createId(String id) to create the Person's Id.
			 */

			
			
			/*
			 * Set some demographic values for the person.
			 * Cast the Person object to PersonImpl here!
			 */

			
			
			/*
			 *  Create new plan and add it to the person. Use the
			 *  PopulationFactory to create the Plan.
			 */
			Plan plan = null;
			
			
			/*
			 * Every Agent has at least one activity which is being at home.
			 * - set the activity type to "home"
			 * - set the start time to 0.0
			 * - add the Activity to the plan.
			 */	
			Coord homeCoord =  scenario.createCoord(entry.h_x, entry.h_y);
			Activity homeActivity = populationFactory.createActivityFromCoord("home", homeCoord);
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
			TransportMode transportMode = null;
			Leg leg = null;
			Activity activity = null;
			Activity previousActivity = homeActivity;
			
			/*
			 *  Create person's Trips and add them to the Plan.
			 */
			for (ZCensusEntry personEntry : personEntries)
			{
				/*
				 * Use the Helper Methods to:
				 * - Create a new Coordinate at the destination of the Trip using
				 *   scenario.createCoord(double x, double y)
				 * - Create a new Transport Mode Object.
				 * - Create a new Activity Type String. 
				 */

				
				
				/*
				 * Create a new Leg using the PopulationFactory and set its parameters.
				 * Mind that MATSim uses seconds as time unit whereas the census uses minutes.
				 */

				
				
				/*
				 * Create a new Activity using the Population Factory and set its parameters.
				 */

				
				
				/*
				 * Add the Leg and the Activity to the plan.
				 */
				
				
				
				/*
				 * Do not forget to update the pointer to the previousActivity.
				 */

				
				
			}
			
			/*
			 * ... and finally: If the last Activity takes place at the Home Coordinates
			 * we assume that the Agent is performing a "home" Activity.
			 */
			if (activity.getCoord().equals(homeCoord))
			{
				activity.setType("home");
			}
		}
		
	}
	
	public void createXY2LinkMapping(String configFile)
	{
		new XY2Links().run(new String[]{configFile});
	}
	
	public void writePopulation(String populationFile)
	{
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(populationFile);
	}

	
	/*
	 * Helper methods that converts the entries from the census file.
	 */
	
	private String getTransportMode(int mode)
	{
		switch (mode)
		{
			case 1: return TransportMode.walk;
			case 2: return TransportMode.bike;
			case 3: return TransportMode.car;
			case 4: return TransportMode.pt;
			case 5: return "undefined";
			
			default: return "undefined";
		}
	}
	
	private String getGender(int gender)
	{
		if (gender == 0) return "f";
		else return "m";
	}
	
	private String getLicense(int license)
	{
		if (license == 1) return "yes";
		else return "no";
	}
	
	private String getCarAvailability(int availability)
	{
		if (availability == 1) return "always";
		else if (availability == 2) return "sometimes";
		else return "never";
	}
	
	private String getActivityType(int activityType)
	{
		switch (activityType)
		{
			case 1: return "work";
			case 2: return "education";
			case 3: return "shop";
			case 4: return "leisure";
			case 5: return "other";
			
			default: return "undefined";
		}
	}

}
