package playground.dziemke.ikea;

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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class CreateDemand {
private Scenario scenario;
private String dataFile = "./input/feathers_output.txt";

private Double xMin=4.87;
private Double xMax=5.56;
private Double yMin=50.772;
private Double yMax=51.014;

private Random random = new Random();
private ObjectAttributes homeLocations = new ObjectAttributes();

private String[] activityTypeStrings = {"home","work","n.a.","brinGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"} ;
private String[] modeStrings = {"n.a.","car","n.a.","walk","pt","n.a.","ride"};
//----------------------------------------------------------------------------"walk" -> "slow" etc.

private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,  "EPSG:32631");



public void run(Scenario scenario, ObjectAttributes homeLocations){
	this.scenario = scenario;
	this.homeLocations = homeLocations;
	this.createPlans();
}

private void createPlans() {
Population population = this.scenario.getPopulation();
PopulationFactory populationFactory = population.getFactory();

try{
	// load data-file
BufferedReader bufferedReader = new BufferedReader(new FileReader(this.dataFile));
// skip header
String line = bufferedReader.readLine();

// set indices
int index_personId = 7;
int index_activityId = 12;
int index_activityType = 14;
int index_age = 8;
int index_homeLocation=1;
int index_activityLocation=17;
int index_beginningTime = 15;
int index_activityDuration = 16;
int index_journeyDuration = 18;
int index_mode=19;

Id previousPerson = null;
double departureTimeBuffer = 0.0;

	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");
		
		Id personId;
		personId=Id.createPersonId(parts[index_personId]);
		Person person = population.getPersons().get(personId);
		Plan plan = person.getSelectedPlan();
		
		// activity start time in [s]:
		double activityStartTime = (((Integer.parseInt(parts[index_beginningTime]))/100)*60
										+Integer.parseInt(parts[index_beginningTime])%100)
										*60;
		// activity duration in [s]:
		double activityDuration = (Double.parseDouble(parts[index_activityDuration]))*60;
		
		// journey duration in [s]:
				double journeyDuration = (Double.parseDouble(parts[index_journeyDuration]))*60;
		
		//Set Coordinates for activity
		Coord coordOrigin;

			//Add first activity
if(!personId.equals(previousPerson)){
				// Check if activity is a home-activity -> home Coord
	if (parts[index_activityType].equals("0")){
		coordOrigin = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");
		Activity activity = populationFactory.createActivityFromCoord("home", coordOrigin);
	//	activity.setStartTime(activityStartTime);
	//	activity.setMaximumDuration(activityDuration);
		activity.setEndTime(activityStartTime+activityDuration);
		plan.addActivity(activity);
			
	}
	// else -> add activity with random Coord
	else {
		coordOrigin = new CoordImpl(
				// for now: Coord generated randomly
				xMin + (double) random.nextFloat()*( xMax - xMin),
				yMin + (double) random.nextFloat()*( yMax - yMin));
		Activity activity = populationFactory.createActivityFromCoord(activityTypeStrings[Integer.parseInt(parts[index_activityType])], ct.transform(coordOrigin));
//		activity.setStartTime(activityStartTime);
//		activity.setMaximumDuration(activityDuration);
		activity.setEndTime(activityStartTime+activityDuration);
		plan.addActivity(activity);

	}
departureTimeBuffer=activityStartTime+activityDuration;
}
//Add a leg to next activity 
	else {
		String mode = parts[index_mode];
		Leg leg = populationFactory.createLeg(modeStrings[Integer.parseInt(mode)]);
		leg.setDepartureTime(departureTimeBuffer);
		leg.setTravelTime(journeyDuration);
		plan.addLeg(leg);
		
		//Add (random) destination Coord
		
		Coord coordDestination;
		
		if (parts[index_activityType].equals("0")){
			coordDestination = (Coord) homeLocations.getAttribute(String.valueOf(personId),"home");}
		else{
		coordDestination = new CoordImpl(
				// for now: Coord generated randomly
				xMin + (double) random.nextFloat()*( xMax - xMin),
				yMin + (double) random.nextFloat()*( yMax - yMin));
		coordDestination = ct.transform(coordDestination);
		}
		// Add activity
		Activity activity = populationFactory.createActivityFromCoord(activityTypeStrings[Integer.parseInt(parts[index_activityType])], coordDestination);
//		activity.setStartTime(activityStartTime);
//		activity.setMaximumDuration(activityDuration);
		activity.setEndTime(activityStartTime+activityDuration);
		plan.addActivity(activity);
		departureTimeBuffer=activityStartTime+activityDuration;

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
