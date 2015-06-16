package playground.dziemke.ikea;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class CreatePopulation {

	private Scenario scenario;
	private String dataFile = "C:\\Users\\jeffw_000\\Desktop\\Masterarbeit\\data\\feathers0\\prdToAscii.csv"; 
			//"./input/feathers_output.txt";
	
	private Double xMin=4.87;
	private Double xMax=5.56;
	private Double yMin=50.772;
	private Double yMax=51.014;
	
	//set population fraction
	private double p=0.05;
	private Random random = new Random();
	
	private ObjectAttributes homeLocations = new ObjectAttributes();
	
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,  "EPSG:32631");

	
public void run(Scenario scenario){
this.scenario=scenario;
this.init();
this.populationCreation();
}

private void populationCreation() {
// store population and population factory in local variable
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
		
	Id previousPerson = null;
// read data-file and create population
		while((line=bufferedReader.readLine()) != null){
			String parts[] = line.split(";");
			
			Id personId = Id.createPersonId(parts[index_personId]);
			
			
			if (!personId.equals(previousPerson)){
				if(random.nextFloat()<=p){
			// Create person
			Person person = populationFactory.createPerson(personId);
			
			// add to population
			population.addPerson(person);
			// set home coordinates
			Coord homeCoord = new CoordImpl(
					// for now: Coord generated randomly
					xMin + (double) random.nextFloat()*( xMax - xMin),
					yMin + (double) random.nextFloat()*( yMax - yMin));
			//store homeCoord as attribute
			homeLocations.putAttribute(person.getId().toString(), "home", ct.transform(homeCoord));
			
			//Create plan and link it to person
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			((PersonImpl)person).setSelectedPlan(plan);
			previousPerson = personId;
				}
			}

		}
	} // end try
	catch (IOException e) {
		e.printStackTrace();
	}
	
}

public ObjectAttributes getHomeLocations(){
	return homeLocations;
}

private void init() {
	// TODO Auto-generated method stub
	
}



}
