package playground.artemc.scenarioTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import com.mysql.jdbc.Statement;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;



public class HomeAssignement {

	static java.sql.PreparedStatement dbHomeLocation;
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

		HashMap<Integer,Integer> zoneUnits = new HashMap<Integer, Integer>();  
		HashMap<Integer,ArrayList<String>> zoneUnitArrays = new HashMap<Integer, ArrayList<String>>(); 
		HashMap<Integer,String> hh_homes = new HashMap<Integer, String>();  
		ResultSet facilities = dba.executeQuery("SELECT * FROM sf_facilities");

		while(facilities.next()){
			String id = facilities.getString("id");
			Integer tractZoneId = facilities.getInt("tractZoneId");	
			Integer structure = facilities.getInt("structure");	
			Integer units = facilities.getInt("units");	

			switch(structure){
			//Residential
			case 100: case 110: case 120: case 121: case 122: case 130: case 140: case 150: case 151: case 152: case 153: case 200: case 210: case 220: case 340: case 530:
				if(zoneUnits.containsKey(tractZoneId)){
					zoneUnits.put(tractZoneId, (zoneUnits.get(tractZoneId) +  units));
				}
				else{
					zoneUnits.put(tractZoneId, units);
					zoneUnitArrays.put(tractZoneId, new ArrayList<String>());
				}
				break;
			default:
				break;
			}

			//Add the structure id to the list as many time as there are units in the structure
			for(int i=0;i<units;i++)
				zoneUnitArrays.get(tractZoneId).add(id);
		}

		//Create copy oh HashMap with zoneUnitArrays
		HashMap<Integer,ArrayList<String>> zoneUnitArraysCopy = new HashMap<Integer, ArrayList<String>>(); 
		for(Integer tractZoneId:zoneUnitArrays.keySet()){
			zoneUnitArraysCopy.put(tractZoneId, new ArrayList<String>());
			for(String facilityId:zoneUnitArrays.get(tractZoneId)){
				zoneUnitArraysCopy.get(tractZoneId).add(facilityId);
			}
		}	

		for(Integer tractZone:zoneUnits.keySet()){
			System.out.println(tractZone+"\t"+zoneUnits.get(tractZone));
		}

		ResultSet households  = dba.executeQuery("SELECT * FROM synthpop_households");
		System.out.println("Assigning home locations to households...");
		int facilityInList = 0;
		String homeFacilityId = null;
		while(households.next()){
			Integer hh_id = households.getInt("synth_hh_id");
			Integer tractZone = households.getInt("zone");
			
			if(zoneUnitArrays.get(tractZone).size()>0){
				facilityInList = (int) (zoneUnitArrays.get(tractZone).size() * generator.nextDouble());
				homeFacilityId = zoneUnitArrays.get(tractZone).get(facilityInList);
				zoneUnitArrays.get(tractZone).remove(facilityInList);

			}
			else{
				System.out.println("Zone "+tractZone+" ran out of units...");
				facilityInList = (int) (zoneUnitArraysCopy.get(tractZone).size() * generator.nextDouble());
				homeFacilityId = zoneUnitArraysCopy.get(tractZone).get(facilityInList);
			}
			hh_homes.put(hh_id, homeFacilityId);

		}

		//bulkInsert(dba.getConnection(), hh_homes);
		dba.close();

		System.out.println("Done!");
	}

	public static void bulkInsert(java.sql.Connection connection, HashMap<Integer, String> hashOfValues) throws SQLException {

		// First create a statement off the connection and turn off unique checks and key creation
		Statement statement = (com.mysql.jdbc.Statement)connection.createStatement();
		statement.execute("DROP TABLE IF EXISTS hh_facilities"); 
		statement.execute("CREATE TABLE hh_facilities(synth_hh_id INT(8), home VARCHAR(10))"); 
		statement.execute("SET UNIQUE_CHECKS=0; ");
		statement.execute("ALTER TABLE hh_facilities DISABLE KEYS");

		// Define the query we are going to execute
		String statementText = "LOAD DATA LOCAL INFILE 'whatever.txt' "+
				"INTO TABLE hh_facilities";

		// Create StringBuilder to String that will become stream
		StringBuilder builder = new StringBuilder();

		// Iterate over map and create tab-text string
		for (Entry<Integer, String> entry : hashOfValues.entrySet()) {
			builder.append(entry.getKey());
			builder.append('\t');
			builder.append(entry.getValue());
			builder.append('\n');
		}

		// Create stream from String Builder
		InputStream is = IOUtils.toInputStream(builder.toString());

		// Setup our input stream as the source for the local infile
		statement.setLocalInfileInputStream(is);

		// Execute the load infile
		statement.execute(statementText);

		// Turn the checks back on
		statement.execute("ALTER TABLE hh_facilities ENABLE KEYS");
		statement.execute("SET UNIQUE_CHECKS=1; ");
	}

}