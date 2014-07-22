package playground.artemc.scenarioTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;
import playground.artemc.utils.SortEntriesByValueDesc;

import com.mysql.jdbc.Statement;


public class SecondaryActivities {

	
	private final static String networkPath = "C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/Siouxfalls_network.xml";
	
	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/SiouxFalls_Database.properties"));
		Random generator = new Random();	

		HashMap<Integer,String> hh_HomeFacilities = new HashMap<Integer, String>();
		HashMap<Integer,Integer> hh_HomeZones = new HashMap<Integer, Integer>();
		HashMap<Integer, ArrayList<String>> secondaryFacilities = new HashMap<Integer, ArrayList<String>>();
		HashMap<Integer, ArrayList<Double>> placeAttractivities = new HashMap<Integer, ArrayList<Double>>();
		HashMap<String,String> person_HomeFacilities = new HashMap<String,String>();		
		HashMap<String,String> secondaryActivities = new HashMap<String, String>();

		NodeDistances nodeDistances = new NodeDistances(networkPath);

		/*Get secondary places for each zone*/
		ResultSet facilities = dba.executeQuery("SELECT * FROM sf_facilities");
		while(facilities.next()){
			String facilityId = facilities.getString("id");
			Integer structure = facilities.getInt("structure");
			Integer zoneId = facilities.getInt("nodeZoneId");
			switch(structure){
			case 311: case 321: case 331: case 345: case 366: case 470: case 500: case 510: case 511: case 512: case 513: case 514: case 515: case 516: case 520: case 525: case 531:
			case 532: case 533: case 534: case 530: 
				if(!secondaryFacilities.containsKey(zoneId)){
					secondaryFacilities.put(zoneId, new ArrayList<String>());
				}
				secondaryFacilities.get(zoneId).add(facilityId);
			}
		}

		/*Add home facilities to HashMap*/
		ResultSet homes = dba.executeQuery("SELECT * FROM hh_facilities_ext");
		while(homes.next()){
			hh_HomeFacilities.put(homes.getInt("synth_hh_id"), homes.getString("home"));
			hh_HomeZones.put(homes.getInt("synth_hh_id"), homes.getInt("nodeZoneId"));
		}	

		/*Find a zone for work location*/
		ResultSet housewives =   dba.executeQuery("SELECT * FROM sf_population WHERE (TRANWORK=0 OR TRANWORK=70) AND AGE>17;");
		while(housewives.next()){
			Integer hh_id = housewives.getInt("synth_hh_id");
			Integer per_no = housewives.getInt("PERNUM");
			String persond_id = hh_id.toString() + "_"+per_no.toString();
			Integer homeZone = hh_HomeZones.get(hh_id);
			
			/*Get sorted list of distances to other zone*/
			SortEntriesByValueDesc sortEntriesByValueDesc = new SortEntriesByValueDesc();
			List<Entry<Integer,Double>> sortedActivityZoneOptions = sortEntriesByValueDesc.entriesSortedByValues(nodeDistances.getDistanceMapForNode(homeZone.toString()));
			sortedActivityZoneOptions.remove(0);
			
			/*Create new set of random attractions*/
			for(Integer zone:secondaryFacilities.keySet()){
				if(placeAttractivities.get(zone)==null){
					placeAttractivities.put(zone, new ArrayList<Double>());
				}
				else{
					placeAttractivities.get(zone).clear();
				}

				for(Integer w=0;w<secondaryFacilities.get(zone).size();w++){
					placeAttractivities.get(zone).add(generator.nextDouble());
				}
			}
			
			/*Create sorted list of home zone work attractions for each zone*/
			HashMap<Integer,ArrayList<Double>> homeZonePlaceAttractions = new HashMap<Integer, ArrayList<Double>>();
			for(Integer z=1;z<25;z++){
				homeZonePlaceAttractions.put(z, new ArrayList<Double>());
				for(Double wp:placeAttractivities.get(z)){
					homeZonePlaceAttractions.get(z).add(wp);
				}	
				Collections.sort(homeZonePlaceAttractions.get(z));
				Collections.reverse(homeZonePlaceAttractions.get(z));
			}
			
			Boolean activityZoneFound = false;			
			Integer placeZone = 0;
			Boolean homeScoreAttractivityTooHigh = false;
			Double bestHomeZoneFacilityAttraction = 0.0;
			do{		
				/*Find maximal PlaceAttractivity in zone of residence*/
				if(homeScoreAttractivityTooHigh){
					homeZonePlaceAttractions.get(homeZone).remove(0);
				}

				if(!homeZonePlaceAttractions.get(homeZone).isEmpty()){
					bestHomeZoneFacilityAttraction = homeZonePlaceAttractions.get(homeZone).get(0);
				}
				else{
					//System.out.println("NO SECONDARY ACTIVITY PLACE FOUND: New, smaller random home zone work attractivity generated");
					bestHomeZoneFacilityAttraction = generator.nextDouble()*bestHomeZoneFacilityAttraction;
					homeZonePlaceAttractions.get(homeZone).add(bestHomeZoneFacilityAttraction);
				}

				/*Find closest Zone with higher PlaceAttractivity*/
				for(Entry<Integer, Double> activityOption:sortedActivityZoneOptions){
					//System.out.println("Searching in Zone: "+workOption.getKey()+" with distance "+workOption.getValue());
					for(Double att:placeAttractivities.get(activityOption.getKey())){
						if(att.doubleValue()>bestHomeZoneFacilityAttraction.doubleValue()){
							placeZone = activityOption.getKey();
							activityZoneFound = true;				
							break;
						}
					}
					if(activityZoneFound == true)
						break;
				}
				homeScoreAttractivityTooHigh=true; 
			}while(!activityZoneFound);

			
			String secondaryFacility = secondaryFacilities.get(placeZone).get(generator.nextInt(secondaryFacilities.get(placeZone).size()));
			secondaryActivities.put(persond_id, secondaryFacility); 
			person_HomeFacilities.put(persond_id, hh_HomeFacilities.get(hh_id));
			
			
		}

		bulkInsert(dba.getConnection(), secondaryActivities, person_HomeFacilities);
		
		dba.close();

		System.out.println("Done!");

	}

	public static void bulkInsert(java.sql.Connection connection, HashMap<String, String> hashOfValues, HashMap<String,String> person_HomeFacilities) throws SQLException {

		// First create a statement off the connection and turn off unique checks and key creation
		Statement statement = (com.mysql.jdbc.Statement)connection.createStatement();
		statement.execute("DROP TABLE IF EXISTS sf_home_secondary"); 
		statement.execute("CREATE TABLE sf_home_secondary(synth_person_id VARCHAR(10), synth_hh_id VARCHAR(10), secondaryFacility VARCHAR(10), homeFacility VARCHAR(10))"); 
		statement.execute("SET UNIQUE_CHECKS=0; ");
		statement.execute("ALTER TABLE sf_home_secondary DISABLE KEYS");

		// Define the query we are going to execute
		String statementText = "LOAD DATA LOCAL INFILE 'whatever.txt' "+
				"INTO TABLE sf_home_secondary";

		// Create StringBuilder to String that will become stream
		StringBuilder builder = new StringBuilder();

		// Iterate over map and create tab-text string
		for (Entry<String, String> entry : hashOfValues.entrySet()) {
			builder.append(entry.getKey());
			builder.append('\t');
			String[] synth_person_id_split = entry.getKey().split("_");
			builder.append( synth_person_id_split[0]);
			builder.append('\t');
			builder.append(entry.getValue());
			builder.append('\t');
			builder.append(person_HomeFacilities.get(entry.getKey()));
			builder.append('\n');
		}

		// Create stream from String Builder
		InputStream is = IOUtils.toInputStream(builder.toString());

		// Setup our input stream as the source for the local infile
		statement.setLocalInfileInputStream(is);

		// Execute the load infile
		statement.execute(statementText);

		// Turn the checks back on
		statement.execute("ALTER TABLE sf_home_secondary ENABLE KEYS");
		statement.execute("SET UNIQUE_CHECKS=1; ");
	}

}
