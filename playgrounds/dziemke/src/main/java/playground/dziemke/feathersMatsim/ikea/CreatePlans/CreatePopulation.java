package playground.dziemke.feathersMatsim.ikea.CreatePlans;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PersonImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class CreatePopulation {

	private Scenario scenario;
	private String dataFile;

	private	ConvertTazToCoord coordTazManager = new ConvertTazToCoord();

	//set population fraction
	private double p=1;
	private Random random = new Random();
	
	private int countAgents=0;

	private ObjectAttributes homeLocations = new ObjectAttributes();
	private ObjectAttributes homeTAZ = new ObjectAttributes();

	public void run(Scenario scenario, String dataFile){
		this.dataFile=dataFile;
		this.scenario=scenario;
		this.init();
		this.populationCreation();
	}

	private void populationCreation() {
		// store population and population factory in local variable
		Population population = this.scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		try{
			coordTazManager.convertCoordinates();
			// load data-file
			BufferedReader bufferedReader = new BufferedReader(new FileReader(this.dataFile));
			// skip header
			String line = bufferedReader.readLine();

			// set indices
			int index_personId = 7;
			int index_homeLocation=1;
			int index_activityLocation=17;

			//define list of considered TAZ
			Integer[] consideredTAZ={1955,1948,1949,1950,1951,1952,1953,1954,1956,1957,1958,1959,
					1960,1961,1962,1963,1964,1965,1966,1967,1968,1969,1970,2127,2128,2129,2130};
			ArrayList<Integer> consideredTAZList=new ArrayList<Integer>(Arrays.asList(consideredTAZ));

			// read data-file and create population
			line=bufferedReader.readLine();

			String parts2[]=line.split(";");
			String currentAgentID=parts2[index_personId];

			//	Boolean ConditionTravelMode=false;
			Boolean ConditionTAZ=false;
			int homeLocation=0;
			int activityLocation=0;

			while((line=bufferedReader.readLine()) != null){
				String parts[] = line.split(";");

				if(parts[index_personId].equals(currentAgentID)){

					// check if agent is being considered for simulation
					//				if(Integer.parseInt(parts[index_travelMode])==1||Integer.parseInt(parts[index_travelMode])==3){
					//					ConditionTravelMode=true;
					//				}
					homeLocation=(Integer.parseInt(parts[index_homeLocation]));
					if
					(parts[index_activityLocation].equals("IKEA")){ConditionTAZ=true;}
					else
					{
						activityLocation=(Integer.parseInt(parts[index_activityLocation]));
						if(consideredTAZList.contains(activityLocation)){
							ConditionTAZ=true;
						}
					}
				}
				else{

					Id<Person> personId = Id.createPersonId(currentAgentID);
					if(random.nextFloat()<=p&&
							//						ConditionTravelMode==true&&
							ConditionTAZ==true){

						// Create person
						Person person = populationFactory.createPerson(personId);

						// add to population
						population.addPerson(person);
						countAgents=countAgents+1;

						// set home coordinates
						Coord homeCoord = coordTazManager.randomCoordinates(homeLocation);

						//store homeCoord as attribute
						homeLocations.putAttribute(person.getId().toString(), "home", homeCoord);
						homeTAZ.putAttribute(person.getId().toString(), "homeTAZ", homeLocation);

						//Create plan and link it to person
						Plan plan = populationFactory.createPlan();
						person.addPlan(plan);
						((PersonImpl)person).setSelectedPlan(plan);
					}
					//			ConditionTravelMode=false;
					ConditionTAZ=false;
					currentAgentID=parts[index_personId];
				}
			}
			bufferedReader.close();
			System.out.println("Number of Agents in population: "+countAgents);

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
