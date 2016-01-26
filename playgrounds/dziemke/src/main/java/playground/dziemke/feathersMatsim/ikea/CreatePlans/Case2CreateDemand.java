package playground.dziemke.feathersMatsim.ikea.CreatePlans;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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

public class Case2CreateDemand {

	private double beelineFactor=1.3;

	private Scenario scenario;
	private String dataFile;

	private	ConvertTazToCoord coordTazManager = new ConvertTazToCoord();

	//IKEA Coordinates
	private Double xIKEA = 662741.5;
	private Double yIKEA = 5643343.5366;

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

			String originTAZ=null;

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
			double sum=0.0;
			double max=0.0;
			double maxSlowMode=0.0;
			String stringMAX = null;
			String stringMAXSlowMode=null;
			int counter=0;
			Coord coordOrigin=new Coord(0,0);
			BufferedWriter bw=new BufferedWriter(new FileWriter("C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2Distances.csv", true));
			BufferedWriter bw2=new BufferedWriter(new FileWriter("C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2DistancesCars.csv", true));


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
		//		double journeyDuration = (Double.parseDouble(parts[index_journeyDuration]))*60;

				//Add first activity
				if(!personId.equals(previousPerson)){
					// Check if activity is a home-activity -> home Coord
					if (parts[index_activityType].equals("0")){
						coordOrigin = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");
						originTAZ=parts[index_homeLocation];
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
			//		leg.setTravelTime(journeyDuration);
					plan.addLeg(leg);

					//Add (random or home) destination Coord
					if (parts[index_activityType].equals("0")){
						coordDestination = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");}
// If agent is an IKEA visitor: set destination to IKEA-coordinates
					else if(parts[index_personId].contains("IKEA")&&parts[index_activityLocation].equals("IKEA")&&parts[index_activityType].equals("4")&&parts[index_mode].equals("1"))

					{coordDestination = new Coord(xIKEA,yIKEA);}


					else{
						System.out.println("Looking for best suited  Coord...Agent: "+personId+" Activity ID: "+Integer.parseInt(parts[index_activityId])+" Distance: "+Double.parseDouble(parts[index_journeyDistance])*1000);

						coordDestination=coordTazManager.findBestRandomCoordinates(Integer.parseInt(parts[index_activityLocation]), coordOrigin, Double.parseDouble(parts[index_journeyDistance])*1000, Integer.parseInt(parts[index_mode]));

						double distance=Math.sqrt(
								Math.pow(coordDestination.getX()-coordOrigin.getX(),2)
								+Math.pow(coordDestination.getY()-coordOrigin.getY(), 2)
								);

						// Slow mode (bike or walking): route distance close to beeline;
						// for car legs however: multiply distance with beelineFactor
						if(Integer.parseInt(parts[index_mode])==1||Integer.parseInt(parts[index_mode])==6){
							distance=distance*beelineFactor;
						}

						// 	Difference between calculated OD-distance and FEATHERS-distance
						double diff=Math.abs(distance-((Double.parseDouble(parts[index_journeyDistance])*1000)));

						// find MAX for car trips:
						if((Integer.parseInt(parts[index_mode])==1)&&diff>max){
							max=diff;
							stringMAX="Agent: "+personId+" Activity ID: "+Integer.parseInt(parts[index_activityId])+" Travel Mode: "+parts[index_mode]+" Distance: "+distance+" difference: "+diff+" Journey_distance: "+Double.parseDouble(parts[index_journeyDistance])+" TAZ: "+Integer.parseInt(parts[index_activityLocation])+" OriginTAZ: "+originTAZ+" CoordOrigin: "+coordOrigin+" CoordDestination: "+coordDestination;
						}
						
						// MAX for slowMode:
						if((Integer.parseInt(parts[index_mode])==3)&&(diff>maxSlowMode)){
							maxSlowMode=diff;
							stringMAXSlowMode="Agent: "+personId+" Activity ID: "+Integer.parseInt(parts[index_activityId])+" Travel Mode: "+parts[index_mode]+" Distance: "+distance+" difference: "+diff+" Journey_distance: "+Double.parseDouble(parts[index_journeyDistance])+" TAZ: "+Integer.parseInt(parts[index_activityLocation])+" OriginTAZ: "+originTAZ+" CoordOrigin: "+coordOrigin+" CoordDestination: "+coordDestination;
						}

						if(Integer.parseInt(parts[index_mode])==1||Integer.parseInt(parts[index_mode])==6
								||Integer.parseInt(parts[index_mode])==3
								||Integer.parseInt(parts[index_mode])==4

								){
							bw.write(parts[index_activityId]+";"+(diff/distance)*100+" %; "+(int) diff+";"+distance+";"+parts[index_mode]+";");
							sum=sum+diff;
							counter=counter+1;
							bw.newLine();
						}
						System.out.println("-------------------------------------------------- Distance: "+distance+" Difference: "+diff+" margin: "+(diff/distance)*100+"% mode: "+parts[index_mode]);


					}
					// Add activity
					Activity activity = populationFactory.createActivityFromCoord(activityTypeStrings[Integer.parseInt(parts[index_activityType])], coordDestination);
					activity.setEndTime(activityStartTime+activityDuration);
					plan.addActivity(activity);
					departureTimeBuffer=activityStartTime+activityDuration;
					coordOrigin=coordDestination;
					originTAZ=parts[index_activityLocation];
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
