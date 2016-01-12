package playground.dziemke.feathersMatsim.ikea.CreatePlans;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class Case0CreateDemand {

	private double beelineFactor=1.3;

	private Scenario scenario;
	private String dataFile;

	private	ConvertTazToCoord coordTazManager = new ConvertTazToCoord();

	private Random random = new Random();
	private ObjectAttributes homeLocations = new ObjectAttributes();

	private String[] activityTypeStrings = {"home","work","n.a.","brinGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"} ;
	private String[] modeStrings = {"n.a.","car","n.a.","slow","pt","n.a.","ride"};

	public void run(Scenario scenario, ObjectAttributes homeLocations, String dataFile){
		this.scenario = scenario;
		this.homeLocations = homeLocations;
		this.dataFile=dataFile;
		this.createPlans();
	}

	private void createPlans() {
		Population population = this.scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		try{
			coordTazManager.convertCoordinates();
			// load data-file
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.dataFile));
			// skip header
			String line = bufferedReader.readLine();

			int originTAZ=0;

			// set indices
			int index_personId = 7;
			int index_activityId = 12;
			int index_activityType = 14;
			int index_homeLocation=1;
			int index_activityLocation=17;
			int index_beginningTime = 15;
			int index_activityDuration = 16;
			int index_journeyDuration = 18;
			int index_mode=19;
			int index_journeyDistance=20;

			Id previousPerson = null;
			double departureTimeBuffer = 0.0;
			Coord coordOrigin=new Coord(0,0);

			while((line=bufferedReader.readLine()) != null){
				String parts[] = line.split(";");

				Id personId=Id.createPersonId(parts[index_personId]);
				Person person = population.getPersons().get(personId);

				// check if person exists in population
				if(!population.getPersons().containsKey(personId)){
					continue;
				}

				Plan plan = person.getSelectedPlan();

				// activity start time in [s]:
				double activityStartTime = (((Integer.parseInt(parts[index_beginningTime]))/100)*60
						+Integer.parseInt(parts[index_beginningTime])%100)
						*60;
				// activity duration in [s]:
				double activityDuration = (Double.parseDouble(parts[index_activityDuration]))*60;

				// journey duration in [s]:
				double journeyDuration = (Double.parseDouble(parts[index_journeyDuration]))*60;

				//Add first activity
				if(!personId.equals(previousPerson)){
					// Check if activity is a home-activity -> home Coord
					if (parts[index_activityType].equals("0")){
						coordOrigin = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");
						originTAZ=Integer.parseInt(parts[index_homeLocation]);
						Activity activity = populationFactory.createActivityFromCoord("home", coordOrigin);
						activity.setEndTime(activityStartTime+activityDuration);
						plan.addActivity(activity);
					}

					// else -> add activity with random Coord
					else {
						coordOrigin = coordTazManager.randomCoordinates(Integer.parseInt(parts[index_activityLocation]));
						Activity activity = populationFactory.createActivityFromCoord(activityTypeStrings[Integer.parseInt(parts[index_activityType])], coordOrigin);
						activity.setEndTime(activityStartTime+activityDuration);
						plan.addActivity(activity);
					}
					departureTimeBuffer=activityStartTime+activityDuration;
				}

				//Add a leg to next activity
				else {
					Coord coordDestination;
					String mode = parts[index_mode];
					Leg leg = populationFactory.createLeg(modeStrings[Integer.parseInt(mode)]);
					leg.setDepartureTime(departureTimeBuffer);
					leg.setTravelTime(journeyDuration);
					plan.addLeg(leg);

					//Add (random or home) destination Coord
					if (parts[index_activityType].equals("0")){
						coordDestination = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");}

					else{
						coordDestination = coordTazManager.randomCoordinates(Integer.parseInt(parts[index_activityLocation]));

						double distance=Math.sqrt(
								Math.pow(coordDestination.getX()-coordOrigin.getX(),2)
								+Math.pow(coordDestination.getY()-coordOrigin.getY(), 2)
								);


						// Set allowed margin depending on travel mode and route distance
						double margin=999999;
						// Slow mode (bike or walking): route distance close to beeline
						if(Integer.parseInt(parts[index_mode])==3){margin=3000;}
						//car or Passenger
						if(Integer.parseInt(parts[index_mode])==1||Integer.parseInt(parts[index_mode])==6){
							// set margin to 20% of travel distance with a min of 25000
							distance=distance*beelineFactor;
							margin=0.2*Double.parseDouble(parts[index_journeyDistance])*1000;
							if(margin<25000){margin=25000;}
						}

						double diff=Math.abs(distance-((Double.parseDouble(parts[index_journeyDistance])*1000)));

						System.out.println("Agent: "+personId+" Activity ID: "+Integer.parseInt(parts[index_activityId])+" Distance: "+diff+" TAZ: "+parts[index_activityLocation]);


						while (diff>=margin
								){
							coordDestination = coordTazManager.randomCoordinates(Integer.parseInt(parts[index_activityLocation]));
							distance=Math.sqrt(
									Math.pow(coordDestination.getX()-coordOrigin.getX(),2)
									+Math.pow(coordDestination.getY()-coordOrigin.getY(), 2)
									);

							if(Integer.parseInt(parts[index_mode])==1||Integer.parseInt(parts[index_mode])==6){
								distance=distance*beelineFactor;
							}	

							diff=Math.abs(distance-((Double.parseDouble(parts[index_journeyDistance])*1000)));	
							System.out.println("Agent: "+personId+" Activity ID: "+Integer.parseInt(parts[index_activityId])+" Travel Mode: "+parts[index_mode]+" Distance: "+distance+" difference: "+diff+" allowed margin: "+margin+" Journey_distance: "+Double.parseDouble(parts[index_journeyDistance])+" TAZ: "+Integer.parseInt(parts[index_activityLocation])+" OriginTAZ: "+originTAZ+" CoordOrigin: "+coordOrigin+" CoordDestination: "+coordDestination);
						}

					}
					// Add activity
					Activity activity = populationFactory.createActivityFromCoord(activityTypeStrings[Integer.parseInt(parts[index_activityType])], coordDestination);
					activity.setEndTime(activityStartTime+activityDuration);
					plan.addActivity(activity);
					departureTimeBuffer=activityStartTime+activityDuration;
					coordOrigin=coordDestination;
					originTAZ=Integer.parseInt(parts[index_activityLocation]);
				}
				previousPerson = personId;
			}
			bufferedReader.close();

		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
