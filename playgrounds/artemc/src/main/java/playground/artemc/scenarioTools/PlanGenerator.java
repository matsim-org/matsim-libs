package playground.artemc.scenarioTools;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class PlanGenerator {

	private static String facilitiesPath = "C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/Siouxalls_facilities.xml";
	private static String populationPath = "C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/Siouxalls_population_v3.xml";
	//private static Double noCarPercentage = 0.1;
	//private static Double noCarPercentageSecondary = 0.2;


	public static void main(String[] args) throws SQLException, NoConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/SiouxFalls_Dempo.properties"));
		Random generator = new Random();	


		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesPath);

		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
		HashMap<String, Integer> ageMap = new HashMap<String, Integer>();
		HashMap<String, String> sexMap = new HashMap<String, String>();
		HashMap<String,Integer> carTracker = new HashMap<String, Integer>();

		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		population.setIsStreaming(true);
		PopulationWriter popWriter = new PopulationWriter(population, scenario.getNetwork());
		popWriter.startStreaming(populationPath);
		
		/*Get Population*/
		ResultSet persons = dba.executeQuery("SELECT * FROM u_artemc.sf_population");
		while(persons.next()){
			ageMap.put(persons.getString("synth_person_id"), persons.getInt("AGE"));
			Integer sexCode = persons.getInt("SEX");
			String sex = "";
			if(sexCode==1){
				sex = "m";
			}
			else{
				sex = "f";
			}
			sexMap.put(persons.getString("synth_person_id"), sex);
		}

		/*Get car ownership*/
		ResultSet cars = dba.executeQuery("SELECT * FROM u_artemc.sf_cars");
		while(cars.next()){
			carTracker.put(cars.getString("synth_hh_id"), cars.getInt("cars"));
		}

		/*Write worker plans*/
		ResultSet workers = dba.executeQuery("SELECT * FROM u_artemc.sf_home_work");
		while(workers.next()){
			String personId = workers.getString("synth_person_id");
			Id<ActivityFacility> homeFacilityId = Id.create(workers.getString("homeFacility"), ActivityFacility.class);
			Id<ActivityFacility> workFacilityId = Id.create(workers.getString("workFacility"), ActivityFacility.class);

			Person person = pf.createPerson(Id.create(personId, Person.class));
			Plan plan = pf.createPlan();

			String mode = "";
			
			String[] parts = personId.split("_");
			String hh_id = parts[0];
			
			if(carTracker.get(hh_id)>0){
				PersonUtils.setCarAvail(person, "always");
				carTracker.put(hh_id, carTracker.get(hh_id)-1);
				mode = "car";
				dba.executeStatement(String.format("UPDATE %s SET car = 1 WHERE synth_person_id = '%s';",
						"u_artemc.sf_home_work", personId));
			}
			else{
				PersonUtils.setCarAvail(person, "never");
				mode = "pt";
				dba.executeStatement(String.format("UPDATE %s SET car = 0 WHERE synth_person_id = '%s';",
						"u_artemc.sf_home_work", personId));
			}
			
	


			PersonUtils.setAge(person, ageMap.get(personId));
			PersonUtils.setSex(person, sexMap.get(personId));
			PersonUtils.setEmployed(person, true);

			//Add home location to the plan
			ActivityImpl actHome = (ActivityImpl) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			ActivityImpl actWork = (ActivityImpl) pf.createActivityFromCoord("work", facilities.get(workFacilityId).getCoord());

			LegImpl leg = (LegImpl) pf.createLeg(mode);
			actHome.setFacilityId(homeFacilityId);
			actHome.setEndTime(3600.00*7.5 + generator.nextGaussian()*900.0);
			plan.addActivity(actHome);
			plan.addLeg(leg);
			actWork.setFacilityId(workFacilityId);
			actWork.setStartTime(actHome.getEndTime() + 900.0);
			actWork.setEndTime(3600.00*17.5 + generator.nextGaussian()*900.0);
			plan.addActivity(actWork);
			plan.addLeg(leg);

			ActivityImpl actHome2 = (ActivityImpl) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			actHome2.setFacilityId(homeFacilityId);
			plan.addActivity(actHome2);		

			person.addPlan(plan);
			popWriter.writePerson(person);

		}

		/*Write secondary activity plans*/
		ResultSet housewives = dba.executeQuery("SELECT * FROM u_artemc.sf_home_secondary");
		while(housewives.next()){
			String personId = housewives.getString("synth_person_id");
			Id<ActivityFacility> homeFacilityId = Id.create(housewives.getString("homeFacility"), ActivityFacility.class);
			Id<ActivityFacility> secondaryFacilityId = Id.create(housewives.getString("secondaryFacility"), ActivityFacility.class);

			Person person = pf.createPerson(Id.create(personId, Person.class));
			Plan plan = pf.createPlan();

			String mode="";
			
			String[] parts = personId.split("_");
			String hh_id = parts[0];
			
			if(carTracker.get(hh_id)>0){
				PersonUtils.setCarAvail(person, "always");
				carTracker.put(hh_id, carTracker.get(hh_id)-1);
				mode = "car";
				dba.executeStatement(String.format("UPDATE %s SET car = 1 WHERE synth_person_id = '%s';",
						"u_artemc.sf_home_secondary", personId));
			}
			else{
				PersonUtils.setCarAvail(person, "never");
				mode = "pt";
				dba.executeStatement(String.format("UPDATE %s SET car = 0 WHERE synth_person_id = '%s';",
						"u_artemc.sf_home_secondary", personId));
			}

			PersonUtils.setAge(person, ageMap.get(personId));
			PersonUtils.setSex(person, sexMap.get(personId));
			PersonUtils.setEmployed(person, false);

			//Add home location to the plan
			ActivityImpl actHome = (ActivityImpl) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			ActivityImpl actSecondary = (ActivityImpl) pf.createActivityFromCoord("secondary", facilities.get(secondaryFacilityId).getCoord());

			
			LegImpl leg = (LegImpl) pf.createLeg(mode);
			actHome.setFacilityId(homeFacilityId);
			actHome.setEndTime(3600.00*7.75 + generator.nextDouble()*3600.0*12);
			plan.addActivity(actHome);
			plan.addLeg(leg);
			actSecondary.setFacilityId(secondaryFacilityId);
			actSecondary.setStartTime(actHome.getEndTime() + 900.00);
			actSecondary.setEndTime(actHome.getEndTime() + 900.00 + 3600.00);
			plan.addActivity(actSecondary);
			plan.addLeg(leg);

			ActivityImpl actHome2 = (ActivityImpl) pf.createActivityFromCoord("home", facilities.get(homeFacilityId).getCoord());
			actHome2.setFacilityId(homeFacilityId);
			plan.addActivity(actHome2);		

			person.addPlan(plan);
			popWriter.writePerson(person);
		}

		popWriter.closeStreaming();
	}
}
