package playground.dhosse.scenarios.generic.population.io.mid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income.IncomePeriod;

import playground.dhosse.scenarios.generic.Configuration;
import playground.dhosse.scenarios.generic.population.HashGenerator;
import playground.dhosse.scenarios.generic.utils.ActivityTypes;
import playground.dhosse.scenarios.generic.utils.Modes;

public class MiDParser {

	private static final Logger log = Logger.getLogger(MiDParser.class);
	
	private Map<String, List<MiDPerson>> midPersonsClassified = new HashMap<>();
	
	private Map<String, MiDHousehold> midHouseholds = new HashMap<>();
	private Map<String, MiDPerson> midPersons = new HashMap<>();
	
	public void run(Configuration configuration){
		
		try {
			
			log.info("Parsing MiD database to create a synthetic population");
			
			Class.forName("org.postgresql.Driver").newInstance();
			Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mobility_surveys",
					configuration.getDatabaseUsername(), configuration.getPassword());
		
			if(connection != null){
				
				if(configuration.isUsingHouseholds()){
					
					log.info("Creating MiD households...");
					
					parseHouseholdsDatabase(connection, configuration.getSqlQuery());
					
				}
				
				log.info("Creating MiD persons...");
				
				parsePersonsDatabase(connection, configuration.getSqlQuery(), configuration.isUsingHouseholds());
				
				log.info("Creating MiD ways...");
				
				parseWaysDatabase(connection, configuration.isOnlyUsingWorkingDays());
				
				connection.close();
				
			} else {
				
				throw new RuntimeException("Database connection could not be established! Aborting...");
				
			}
			
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			
			e.printStackTrace();
			
		}
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		for(MiDPerson person : this.getPersons().values()){
			
			Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(person.getId()));
			PersonUtils.setAge(p, person.getAge());
			PersonUtils.setCarAvail(p, Boolean.toString(person.getCarAvailable()));
			PersonUtils.setEmployed(p, person.isEmployed());
			PersonUtils.setLicence(p, Boolean.toString(person.isHasLicense()));
			PersonUtils.setSex(p, Integer.toString(person.getSex()));
			
			for(MiDPlan plan : person.getPlans()){
				
				Plan pl = scenario.getPopulation().getFactory().createPlan();
				double weight = 0.;
				
				for(MiDPlanElement element : plan.getPlanElements()){
					
					if(element instanceof MiDActivity){
						
						MiDActivity act = (MiDActivity)element;
						
						Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord(act.getActType(),
								new Coord(0.0d, 0.0d));
						activity.setStartTime(act.getStartTime());
						activity.setEndTime(act.getEndTime());
						pl.addActivity(activity);
						
					} else{
						
						MiDWay way = (MiDWay)element;
						
						Leg leg = scenario.getPopulation().getFactory().createLeg(way.getMainMode());
						leg.setDepartureTime(way.getStartTime());
						leg.setTravelTime(way.getEndTime() - way.getStartTime());
						weight += way.getWeight();
						pl.addLeg(leg);
						
					}
					
				}
				
				pl.getCustomAttributes().put("weight", weight);
				
				p.addPlan(pl);
				
			}
			
			if(!p.getPlans().isEmpty()){
				scenario.getPopulation().addPerson(p);
			}
			
		}
		
		new PopulationWriter(scenario.getPopulation()).write("/home/dhosse/plansFromMiD.xml.gz");
		
		if(configuration.isUsingHouseholds()){

			for(MiDHousehold household : this.getHouseholds().values()){
			
				Household hh = scenario.getHouseholds().getFactory().createHousehold(Id.create(household.getId(), Household.class));
				
				for(String pid : household.getMemberIds()){
					
					Id<Person> personId = Id.createPersonId(pid);
					
					if(scenario.getPopulation().getPersons().containsKey(personId)){
					
						((HouseholdImpl)hh).getMemberIds().add(personId);
						
					}
					
				}
				
				hh.setIncome(scenario.getHouseholds().getFactory().createIncome(household.getIncome(),
						IncomePeriod.month));
				
				if(!hh.getMemberIds().isEmpty()){
					scenario.getHouseholds().getHouseholds().put(hh.getId(), hh);
				}
				
			}
			
			new HouseholdsWriterV10(scenario.getHouseholds()).writeFile("/home/dhosse/hhFromMiD.xml.gz");
			
		}
		
	}
	
	private void parseHouseholdsDatabase(Connection connection, String query) throws RuntimeException, SQLException{
		
		Statement statement = connection.createStatement();
	
		ResultSet set = statement.executeQuery(query);
		
		while(set.next()){
			
			String hhId = set.getString(MiDConstants.HOUSEHOLD_ID);
			MiDHousehold hh = new MiDHousehold(hhId);
			
			double income = set.getDouble(MiDConstants.HOUSEHOLD_INCOME);
			hh.setIncome(handleHouseholdIncome(income));
			
			this.midHouseholds.put(hhId, hh);
			
		}
		
		set.close();
		statement.close();
		
		if(this.midHouseholds.isEmpty()){
			
			log.warn("The selected query \"" + query + "\" yielded no results...");
			log.warn("This eventually results in no population.");
			log.warn("Continuing anyway");
			
		} else {
			
			log.info("Created " + this.midHouseholds.size() + " households from MiD database.");
			
		}
		
	}
	
	/**
	 * 
	 * Parses the mid persons database
	 * 
	 * @param args
	 * 0: url</br>
	 * 1: username</br>
	 * 2: password</br>
	 * @throws SQLException 
	 * 
	 */
	private void parsePersonsDatabase(Connection connection, String query, boolean isUsingHouseholds) throws SQLException{
		
		Statement statement = connection.createStatement();

		ResultSet set = null;
		
		if(isUsingHouseholds){
			
			set = statement.executeQuery("select * from mid2008.persons_raw");
			
		} else {
			
			set = statement.executeQuery(query);
			
		}
		
		while(set.next()){
			
			String hhId = set.getString(MiDConstants.HOUSEHOLD_ID);
			String personId = set.getString(MiDConstants.PERSON_ID);
			double personWeight = set.getDouble(MiDConstants.PERSON_WEIGHT);
			String carAvail = set.getString(MiDConstants.PERSON_CAR_AVAIL);
			String license = set.getString(MiDConstants.PERSON_LICENSE);
			String sex = set.getString(MiDConstants.PERSON_SEX);
			String age = set.getString(MiDConstants.PERSON_AGE);
			String employed = set.getString(MiDConstants.PERSON_EMPLOYED);
			
			int personGroup = set.getInt(MiDConstants.PERSON_GROUP_12);
			int phase = set.getInt(MiDConstants.PERSON_LIFE_PHASE);
			
			MiDPerson person = new MiDPerson(hhId + personId, sex, age, carAvail, license, employed);
			person.setWeight(personWeight);
			person.setPersonGroup(personGroup);
			person.setLifePhase(phase);
			
			if(isUsingHouseholds){
				
				if(!this.midHouseholds.containsKey(hhId)){
					
					continue;
					
				} else {
					
					this.midHouseholds.get(hhId).getMemberIds().add(person.getId());
					
				}
				
			}
			
			if(!this.midPersons.containsKey(person.getId())){
			
				this.midPersons.put(person.getId(), person);
				
			}
			
			//generate person hash in order to classify the current person
			String hash = HashGenerator.generateAgeGroupHash(person);
			
			if(!this.midPersonsClassified.containsKey(hash)){
				
				this.midPersonsClassified.put(hash, new ArrayList<MiDPerson>());
				
			}
			
			this.midPersonsClassified.get(hash).add(person);
			
		}
		
		set.close();
		statement.close();
		
		if(this.midPersons.isEmpty()){

			log.warn("The selected query \"" + query + "\" yielded no results...");
			log.warn("This eventually results in no population.");
			log.warn("Continuing anyway");
			
		} else {
			
			log.info("Created " + this.midPersons.size() + " persons from MiD database.");
			
		}
		
	}
	
	private void parseWaysDatabase(Connection connection, boolean onlyWorkingDays) throws SQLException {
		
		Statement statement = connection.createStatement();

		String query = "select * from mid2008.ways_raw";
		
		if(onlyWorkingDays){
			query.concat(" where stichtag < 6");
		}
		
		ResultSet set = statement.executeQuery(query);
		
		int lastWayIdx = 100;
		int counter = 0;
		String lastPersonId = "";
		
		while(set.next()){
			
			String hhId = set.getString(MiDConstants.HOUSEHOLD_ID);
			String personId = set.getString(MiDConstants.PERSON_ID);
			MiDPerson person = this.midPersons.get(hhId + personId);
			
			if(person != null){
			
			MiDPlan plan = null;
			
			int currentWayIdx = set.getInt(MiDConstants.WAY_ID_SORTED);
			
			//if the index of the current way is lower than the previous index
			//or the currently processed way is a rbw
			//it's probably a new plan...
			if(currentWayIdx < lastWayIdx || currentWayIdx >= 100 ||
					!lastPersonId.equals(person.getId())){
				
				plan = new MiDPlan();
				person.getPlans().add(plan);
				
			} else {
				
				plan = person.getPlans().get(person.getPlans().size() - 1);
				
			}
			
			if(person != null){
				
				//the act type index at the destination
				int purpose = set.getInt(MiDConstants.PURPOSE);
				
				//the main mode of the leg and the mode combination
				String mainMode = set.getString(MiDConstants.MAIN_MODE_DIFF);
				Set<String> modes = CollectionUtils.stringToSet(set.getString(MiDConstants
						.MODE_COMBINATION));
				
				double startTime =set.getDouble(MiDConstants.ST_TIME);
				double endTime = set.getDouble(MiDConstants.EN_TIME);
				
				int startDate = set.getInt(MiDConstants.ST_DAT);
				int endDate = set.getInt(MiDConstants.EN_DAT);
				
				//if the way ends on the next day, add 24 hrs to the departure / arrival time
				if(startDate != 0){
					startTime += 24 * 3600;
				}
				if(endDate != 0){
					endTime += 24 * 3600;
				}
				
				double weight = set.getDouble(MiDConstants.WAY_WEIGHT);

				//create a new way and set the main mode, mode combination
				//and departure / arrival time
				MiDWay way = new MiDWay(currentWayIdx);
				way.setMainMode(handleMainMode(mainMode));
				way.setModes(modes);
				way.setStartTime(startTime);
				way.setEndTime(endTime);
				way.setWeight(weight);
				
				if(plan.getPlanElements().size() < 1){
					
					//add the source activity
					double firstActType = set.getDouble(MiDConstants.START_POINT);
					MiDActivity firstAct = new MiDActivity(handleActTypeAtStart(firstActType));
					firstAct.setStartTime(0);
					firstAct.setEndTime(way.getStartTime());
					plan.getPlanElements().add(firstAct);
					
				}
				
				String actType = handleActType(purpose);
				
				//if it's a return leg, set the act type according to the act type
				// before the last activity
				if(actType.equals("return")){
				
					if(plan.getPlanElements().size() > 2){
						
						actType = ((MiDActivity)plan.getPlanElements().get(plan.getPlanElements()
								.size() - 3)).getActType();
						
					} else{
						
						actType = ActivityTypes.HOME;
						
					}
					
				}
				
				//if it's a round-based trip, the act types at origin and destination equal
				if(set.getDouble(MiDConstants.END_POINT) == 5){

					addRoundBasedWayAndActivity(plan, way, currentWayIdx, actType);
					
				} else {
					
					addWayAndActivity(set, plan, way, actType);
					
				}
				
				counter++;
				
			}
			
			lastWayIdx = currentWayIdx;
			lastPersonId = person.getId();
			
			}
			
		}
		
		set.close();
		statement.close();
		
		log.info("Created " + counter + " ways from MiD database.");
		
	}
	
	private void addRoundBasedWayAndActivity(MiDPlan plan, MiDWay way, int currentWayIdx, String actType){
		
		double startTime = way.getStartTime();
		double endTime = way.getEndTime();
		
		//insert intermediate act and return leg
		way.setStartTime((endTime + startTime)/2);
		
		MiDWay firstWay = new MiDWay(currentWayIdx);
		firstWay.setMainMode(handleMainMode(way.getMainMode()));
		firstWay.setModes(way.getModes());
		firstWay.setStartTime(startTime);
		firstWay.setEndTime((endTime + startTime)/2);
		
		MiDActivity intermediateAct = new MiDActivity(actType);
		intermediateAct.setStartTime(firstWay.getEndTime());
		intermediateAct.setEndTime(firstWay.getEndTime());
		
		actType = ((MiDActivity)plan.getPlanElements().get(plan.getPlanElements()
				.size() - 1)).getActType();
		
		MiDActivity activity = new MiDActivity(actType);
		
		//set end time of last activity in plan to
		//the departure time of the current way
		((MiDActivity)plan.getPlanElements().get(plan.getPlanElements().size()-1))
			.setEndTime(way.getStartTime());
		
		//set start time of current activity to
		//the arrival time of the current way
		activity.setStartTime(way.getEndTime());
		
		plan.getPlanElements().add(firstWay);
		plan.getPlanElements().add(intermediateAct);
		plan.getPlanElements().add(way);
		plan.getPlanElements().add(activity);
		
	}
	
	private void addWayAndActivity(ResultSet set, MiDPlan plan, MiDWay way, String actType) throws SQLException{
		
		MiDActivity activity = new MiDActivity(actType);
		
		//set end time of last activity in plan to
		//the departure time of the current way
		((MiDActivity)plan.getPlanElements().get(plan.getPlanElements().size()-1))
			.setEndTime(way.getStartTime());
		
		//set start time of current activity to
		//the arrival time of the current way
		activity.setStartTime(way.getEndTime());
		
		//add current way and activity
		plan.getPlanElements().add(way);
		plan.getPlanElements().add(activity);
		
	}
	
	private String handleActType(int idx){
		
		switch(idx){
		
			case 1: return ActivityTypes.WORK;
			case 2: return ActivityTypes.BUSINESS;
			case 3: return ActivityTypes.EDUCATION;
			case 4: return ActivityTypes.SHOPPING;
			case 5: return ActivityTypes.PRIVATE;
			case 6: return ActivityTypes.PICK_DROP;
			case 7: return ActivityTypes.LEISURE;
			case 8: return ActivityTypes.HOME;
			case 9: return "return";
			case 31: return ActivityTypes.KINDERGARTEN;
			case 10:
			case 11: 
			default: return ActivityTypes.OTHER;
		
		}
		
	}
	
	private String handleActTypeAtStart(double idx){
		
		switch((int)idx){
		
			case 1: return ActivityTypes.HOME;
			case 2: return ActivityTypes.WORK;
			default: return ActivityTypes.OTHER;
			
		}
		
	}
	
	private String handleMainMode(String modeIdx){
		
		switch(modeIdx){
		
		case "1": return TransportMode.walk;
		case "2": return TransportMode.bike;
		case "3": return Modes.SCOOTER;
		case "4": return Modes.MOTORCYCLE;
		case "5": return TransportMode.ride;
		case "6": return TransportMode.car;
		case "8": return TransportMode.pt;
		default: return TransportMode.other;
		
		}
		
	}
	
	private double handleHouseholdIncome(double incomeIdx){
		
		switch((int)incomeIdx){
		
		case 1: return 250;
		case 2: return 750;
		case 3: return 1200;
		case 4: return 1750;
		case 5: return 2300;
		case 6: return 2800;
		case 7: return 3300;
		case 8: return 3800;
		case 9: return 4300;
		case 10: return 4800;
		case 11: return 5300;
		case 12: return 5800;
		case 13: return 6300;
		case 14: return 6800;
		case 15: return 7300;
		default: return 0;
		
		}
		
	}
	
	public Map<String, MiDHousehold> getHouseholds(){
		return this.midHouseholds;
	}
	
	public Map<String, MiDPerson> getPersons(){
		return this.midPersons;
	}
	
	public Map<String, List<MiDPerson>> getClassifiedPersons(){
		return this.midPersonsClassified;
	}
	
}
