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

import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import playground.dhosse.scenarios.generic.Configuration;
import playground.dhosse.scenarios.generic.population.HashGenerator;
import playground.dhosse.scenarios.generic.utils.ActivityTypes;

public class MiDParser {

	private Map<String, List<MiDPerson>> midPersonsClassified = new HashMap<>();
	
	private Map<String, MiDHousehold> midHouseholds = new HashMap<>();
	private Map<String, MiDPerson> midPersons = new HashMap<>();
	
	public void run(Configuration configuration){
		
		if(configuration.isUsingHouseholds()){
			
			parseHouseholdsDatabase(new String[]{configuration.getHouseholdDatabaseUrl(),
					configuration.getDatabaseUsername(), configuration.getPassword()});
			
		}
		
		parsePersonsDatabase(new String[]{configuration.getPersonDatabaseUrl(),
				configuration.getDatabaseUsername(), configuration.getPassword()});
		
		parseWaysDatabase(new String[]{configuration.getWayDatabaseUrl(),
				configuration.getDatabaseUsername(), configuration.getPassword()});
		
	}
	
	private void parseHouseholdsDatabase(String args[]) throws RuntimeException{
		
		try {
			
			Class.forName("org.postgresql.Driver").newInstance();
			Connection connection = DriverManager.getConnection(args[0],
					args[1], args[2]);
			
			if(connection != null){

				Statement statement = connection.createStatement();

				ResultSet set = statement.executeQuery("select * from households"
						+ " where [condition]");
				
				while(set.next()){
					
					String hhId = set.getString(MiDConstants.HOUSEHOLD_ID);
					MiDHousehold hh = new MiDHousehold(hhId);
					
					this.midHouseholds.put(hhId, hh);
					
				}
				
				set.close();
				statement.close();
				connection.close();
				
			} else{
				
				throw new RuntimeException("Database connection could not be established! Aborting...");
				
			}
			
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
				SQLException e) {
			
			e.printStackTrace();
			
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
	 * 
	 * @throws RunimeException 
	 */
	private void parsePersonsDatabase(String args[]) throws RuntimeException{
		
		try {
			
			Class.forName("org.postgresql.Driver").newInstance();
			Connection connection = DriverManager.getConnection(args[0],
					args[1], args[2]);
			
			if(connection != null){

				Statement statement = connection.createStatement();

				ResultSet set = statement.executeQuery("select * from persons"
						+ " where [condition]");
				
				while(set.next()){
					
					String personId = set.getString(MiDConstants.PERSON_ID);
					double personWeight = set.getDouble(MiDConstants.PERSON_WEIGHT);
					String carAvail = set.getString(MiDConstants.PERSON_CAR_AVAIL);
					String license = set.getString(MiDConstants.PERSON_LICENSE);
					String sex = set.getString(MiDConstants.PERSON_SEX);
					String age = set.getString(MiDConstants.PERSON_AGE);
					String employed = set.getString(MiDConstants.PERSON_EMPLOYED);
					
					MiDPerson person = new MiDPerson(personId, sex, age, carAvail, license, employed);
					person.setWeight(personWeight);
					
					if(!this.midPersons.containsKey(personId)){
					
						this.midPersons.put(personId, person);
						
					}
					
					//generate person hash in order to classify the current person
					String hash = HashGenerator.generateMiDPersonHash(person);
					
					if(!this.midPersonsClassified.containsKey(hash)){
						
						this.midPersonsClassified.put(hash, new ArrayList<MiDPerson>());
						
					}
					
					this.midPersonsClassified.get(hash).add(person);
					
				}
				
				set.close();
				statement.close();
				connection.close();
				
			} else{
				
				throw new RuntimeException("Database connection could not be established! Aborting...");
				
			}
			
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
				SQLException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	private void parseWaysDatabase(String args[]) throws RuntimeException{
		
		try {
			
			Class.forName("org.postgresql.Driver").newInstance();
			Connection connection = DriverManager.getConnection(args[0],
					args[1], args[2]);
			
			if(connection != null){

				Statement statement = connection.createStatement();

				ResultSet set = statement.executeQuery("select * from ways"
						+ " where [condition]");
				
				int lastWayIdx = 0;
				
				while(set.next()){
					
					String personId = set.getString(MiDConstants.PERSON_ID);
					MiDPerson person = this.midPersons.get(personId);
					
					MiDPlan plan = null;
					
					int id = set.getInt(MiDConstants.WAY_ID_SORTED);
					
					//if the index of the current way is lower than the previous index
					//it's probably a new plan...
					if(id < lastWayIdx){
						
						plan = new MiDPlan();
						person.getPlans().add(plan);
						
					} else {
						
						plan = person.getPlans().get(person.getPlans().size() - 1);
						
					}
					
					if(person != null){
						
						//the act type index at the destination
						int purpose = set.getInt(MiDConstants.PURPOSE);
						
						//the main mode of the leg and the mode combination
						String mainMode = set.getString(MiDConstants.MAIN_MODE);
						Set<String> modes = CollectionUtils.stringToSet(set.getString(MiDConstants
								.MODE_COMBINATION));
						
						double startTime = Time.parseTime(set.getString(MiDConstants.ST_TIME));
						double endTime = Time.parseTime(set.getString(MiDConstants.EN_TIME));
						
						int startDate = set.getInt(MiDConstants.ST_DAT);
						int endDate = set.getInt(MiDConstants.EN_DAT);
						
						//if the way ends at the next day, add 24 hrs to the departure / arrival time
						if(startDate != 0){
							startTime += 24 * 3600;
						}
						if(endDate != 0){
							endTime += 24 * 3600;
						}
						
						double weight = set.getDouble(MiDConstants.WAY_WEIGHT);

						//create a new way and set the main mode, mode combination
						//and departure / arrival time
						MiDWay way = new MiDWay(id);
						way.setMainMode(mainMode);
						way.setModes(modes);
						way.setStartTime(startTime);
						way.setEndTime(endTime);
						way.setWeight(weight);
						
						String actType = handleActType(purpose);
						
						//if it's a return leg, set the act type according to the act type
						// before the last activity
						if(actType.equals("return")){
						
							actType = ((MiDActivity)plan.getPlanElements().get(plan.getPlanElements()
									.size() - 3)).getActType();
							
						}
						
						//if it's a round-based trip, the act types at origin and destination equal
						if(set.getInt(MiDConstants.END_POINT) == 9){
							
							actType = ((MiDActivity)plan.getPlanElements().get(plan.getPlanElements()
									.size() - 1)).getActType();
							
						}
						
						MiDActivity activity = new MiDActivity(actType);
						
						if(plan.getPlanElements().size() < 1){
							
							//add the source activity
							int firstActType = set.getInt(MiDConstants.START_POINT);
							MiDActivity firstAct = new MiDActivity(handleActTypeAtStart(firstActType));
							firstAct.setStartTime(0);
							firstAct.setEndTime(startTime);
							plan.getPlanElements().add(firstAct);
							
						} else {

							//set end time of last activity in plan to
							//the departure time of the current way
							((MiDActivity)plan.getPlanElements().get(plan.getPlanElements().size()-1))
								.setEndTime(startTime);
							
							//set start time of current activity to
							//the arrival time of the current way
							activity.setStartTime(endTime);
							
						}
						
						//add current way and activity
						plan.getPlanElements().add(way);
						plan.getPlanElements().add(activity);
						
					}
					
					lastWayIdx = id;
					
				}
				
				set.close();
				statement.close();
				connection.close();
				
			} else{
				
				throw new RuntimeException("Database connection could not be established! Aborting...");
				
			}
			
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException |
				SQLException e) {
			
			e.printStackTrace();
			
		}
		
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
	
	private String handleActTypeAtStart(int idx){
		
		switch(idx){
		
			case 1: return ActivityTypes.HOME;
			case 2: return ActivityTypes.WORK;
			default: return ActivityTypes.OTHER;
			
		}
		
	}
	
}
