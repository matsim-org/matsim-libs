package playground.artemc.scenarioTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

import com.mysql.jdbc.Statement;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;
import playground.artemc.utils.SortEntriesByValueDesc;


public class WorkplaceAssignment {


	private final static String OD_MatrixPath = "C:/Work/Scenarios/SiouxFalls/OD-Table_export.csv";
	private final static String networkPath = "C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/Siouxfalls_network.xml";
	private final static String new_OD_MatrixPath = "C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/newODmatrix14112013.csv";
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
		
		Integer workingPopulation=0;

		HashMap<Integer,String> hh_HomeFacilities = new HashMap<Integer, String>();
		HashMap<Integer,ArrayList<String>> workFacilities = new HashMap<Integer, ArrayList<String>>();
		HashMap<String,Integer> facilityLocations = new HashMap<String, Integer>();
		HashMap<String,String> facilityLinks = new HashMap<String, String>();
		HashMap<Integer, Integer> workersInZone = new HashMap<Integer, Integer>(); 
		HashMap<Integer, Integer> workplacesInZone = new HashMap<Integer, Integer>();
		HashMap<String, Integer> workerHomezones = new HashMap<String, Integer>();
		HashMap<String, Integer> workerWorkzones = new HashMap<String, Integer>();
		HashMap<Integer,ArrayList<Double>> workplaceAttractivities = new HashMap<Integer, ArrayList<Double>>();
		HashMap<String, String> person_WorkFacilities = new HashMap<String, String>();
		

		NodeDistances nodeDistances = new NodeDistances(networkPath);

		//Add home facilities to HashMap
		ResultSet homes = dba.executeQuery("SELECT * FROM hh_facilities");
		while(homes.next()){
			hh_HomeFacilities.put(homes.getInt("synth_hh_id"), homes.getString("home"));
		}	

		//Add all facility locations to HashMap
		ResultSet facilities = dba.executeQuery("SELECT * FROM sf_facilities");
		while(facilities.next()){
			String facilityId = facilities.getString("id");
			Integer nodeZoneId = facilities.getInt("nodeZoneId");
			Integer structure = facilities.getInt("structure");
			String link_id = facilities.getString("link_id");
			
			facilityLocations.put(facilityId, nodeZoneId);	
			facilityLinks.put(facilityId, link_id);

			switch(structure){
			//Residential
			case 300: case 310: case 320: case 330: case 350: case 360: case 363: case 365: case 311: case 321: case 331: case 345: case 366: 
				case 470: case 340: case 430: case 431: case 432: case 433: case 434: case 435: case 436: case 450: case 451: case 452: case 453: 
					case 440: case 460: case 461: case 462: case 500: case 510: case 511: case 512: case 513: case 514: case 515: case 516: case 520:
						case 525: case 531: case 532: case 533: case 534: case 530: case 600: case 620: case 630: case 631: case 632: case 640: case 641:
							case 642: case 643:
				if(!workFacilities.containsKey(nodeZoneId))
					workFacilities.put(nodeZoneId, new ArrayList<String>());
				workFacilities.get(nodeZoneId).add(facilityId);
				break;
			default:
				break;
			}
		}


		/* Importing ODMatrix*/
		Integer[][] ODmatrix = new Integer[24][24];;
		try {
			ODmatrix = readODMatrix(OD_MatrixPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*New OD-Matrix*/
		Integer[][] newODmatrix = new Integer[24][24];;
		for(Integer row=0;row<24;row++){
			for(Integer column=0;column<24;column++){
				newODmatrix[row][column] =0;
			}
		}

		Integer totalTripsOD = 0;
		Integer row = 0;
		HashMap<Integer, Integer> tripsFromZone_OD = new HashMap<Integer, Integer>();

		for(Integer[] zoneTrips:ODmatrix){
			row++;
			tripsFromZone_OD.put(row, 0);
			for(Integer trips:zoneTrips){
				tripsFromZone_OD.put(row, (tripsFromZone_OD.get(row)+trips));			
				totalTripsOD = totalTripsOD + trips;
			}			
		}

		/*Get employed population*/
		ResultSet population = dba.executeQuery("SELECT * FROM sf_population WHERE TRANWORK!=0 AND TRANWORK<70");	
		while(population.next()){
			Integer synth_hh_id = population.getInt("synth_hh_id");
			Integer synth_person_no = population.getInt("PERNUM");
			/*Get home facility for persons household*/ 
			String hh_home_facility = hh_HomeFacilities.get(synth_hh_id);
			Integer homeZone = facilityLocations.get(hh_home_facility);

			/*Add worker id and location to HashMap*/
			String synth_person_id = synth_hh_id.toString() + "_" + synth_person_no.toString();
			workerHomezones.put(synth_person_id, homeZone);
			
			/*Count workers per zone*/
			if(workersInZone.containsKey(homeZone)){
				workersInZone.put(homeZone, (workersInZone.get(homeZone) + 1));
			}else{
				workersInZone.put(homeZone, 1);
			}
			workingPopulation++;
		}

		/*Generate scaled number of work places for each zone*/
		Double sumOfAllWorkplaceProportions = 0.0;
		for(Integer zone:workersInZone.keySet()){
			Double tentativeWorkplaceProportion = 2*((double) tripsFromZone_OD.get(zone)/ (double) totalTripsOD) - (double) workersInZone.get(zone)/ (double) workingPopulation;
			if(tentativeWorkplaceProportion>0){
				sumOfAllWorkplaceProportions = sumOfAllWorkplaceProportions + tentativeWorkplaceProportion;
			}
		}

		for(Integer zone:workersInZone.keySet()){
			Double workplaceProportion = (2*((double) tripsFromZone_OD.get(zone)/ (double) totalTripsOD) - (double) workersInZone.get(zone)/ (double) workingPopulation) / sumOfAllWorkplaceProportions;
			if(workplaceProportion>0){
				workplacesInZone.put(zone, (int) Math.round(workplaceProportion * workingPopulation));
			}
			else{
				workplacesInZone.put(zone, 0);
			}
			System.out.println(zone+"\t"+workplacesInZone.get(zone));
		}

		/*Find a zone for work location*/
		for(String workerId:workerHomezones.keySet()){
			Integer workersHomeZone = workerHomezones.get(workerId);
			/*Get sorted list of distances to other zone*/
			SortEntriesByValueDesc sortEntriesByValueDesc = new SortEntriesByValueDesc();
			List<Entry<Integer,Double>> sortedWorkZoneOptions = sortEntriesByValueDesc.entriesSortedByValues(nodeDistances.getDistanceMapForNode(workersHomeZone.toString()));
			sortedWorkZoneOptions.remove(0);
			
			/*Create new set of random attractions*/
			for(Integer zone:workplacesInZone.keySet()){
				if(workplaceAttractivities.get(zone)==null){
					workplaceAttractivities.put(zone, new ArrayList<Double>());
				}
				else{
					workplaceAttractivities.get(zone).clear();
				}
				
				for(Integer w=0;w<workplacesInZone.get(zone);w++){
					workplaceAttractivities.get(zone).add(generator.nextDouble());
				}
			}
			
			/*Create sorted list of home zone work attractions for each zone*/
			HashMap<Integer,ArrayList<Double>> homeZoneWorkAttractions = new HashMap<Integer, ArrayList<Double>>();
			for(Integer z=1;z<25;z++){
				homeZoneWorkAttractions.put(z, new ArrayList<Double>());
				for(Double wp:workplaceAttractivities.get(z)){
					homeZoneWorkAttractions.get(z).add(wp);
				}	
				Collections.sort(homeZoneWorkAttractions.get(z));
				Collections.reverse(homeZoneWorkAttractions.get(z));
			}
			
			
			Boolean workZoneFound = false;			
			Integer workZone = 0;
			Boolean homeScoreAttractivityTooHigh = false;
			Double bestHomeZoneWorkFacilityAttraction = 0.0;
			do{		
				/*Find maximal WorkPlaceAttractivity in zone of residence*/
				if(homeScoreAttractivityTooHigh){
					homeZoneWorkAttractions.get(workersHomeZone).remove(0);
				}
				
				if(!homeZoneWorkAttractions.get(workersHomeZone).isEmpty()){
					bestHomeZoneWorkFacilityAttraction = homeZoneWorkAttractions.get(workersHomeZone).get(0);
				}
				else{
					//System.out.println("NO WORKPLACE FOUND: New, smaller random home zone work attractivity generated");
					bestHomeZoneWorkFacilityAttraction = generator.nextDouble()*bestHomeZoneWorkFacilityAttraction;
					homeZoneWorkAttractions.get(workersHomeZone).add(bestHomeZoneWorkFacilityAttraction);
				}
				
				/*Find closest Zone with higher WorkPlaceAttractivity*/
				for(Entry<Integer, Double> workOption:sortedWorkZoneOptions){
					//System.out.println("Searching in Zone: "+workOption.getKey()+" with distance "+workOption.getValue());
					for(Double att:workplaceAttractivities.get(workOption.getKey())){
						if(att.doubleValue()>bestHomeZoneWorkFacilityAttraction.doubleValue()){
							workZone = workOption.getKey();
							workZoneFound = true;				
							break;
						}
					}
					if(workZoneFound == true)
						break;
				}
				homeScoreAttractivityTooHigh=true; 
			}while(!workZoneFound);
			
			workerWorkzones.put(workerId, workZone);
		//	System.out.println("   Work zone for "+workersHomeZone+" found: "+workZone);
			
			
			/*Create new OD MAtrix and write it out as CSV*/
			newODmatrix[workersHomeZone-1][workZone-1]++;
			newODmatrix[workZone-1][workersHomeZone-1]++;
			BufferedWriter writerODmatrix = new BufferedWriter( new FileWriter(new_OD_MatrixPath));
			for(Integer row1=0;row1<24;row1++){
				for(Integer column1=0;column1<24;column1++){
					writerODmatrix.write(newODmatrix[row1][column1]+",");
				}
				writerODmatrix.write("\n");
			}
			writerODmatrix.close();
			
			/*Assign workfacility inside the chosen workzone*/			
			boolean workAtSameLinkFromHome=true;
			String workFacilityId = null ;
			while(workAtSameLinkFromHome){
				workFacilityId = workFacilities.get(workZone).get(generator.nextInt((int) workFacilities.get(workZone).size()));
				if(facilityLinks.get(workFacilityId) == facilityLinks.get(hh_HomeFacilities.get(Integer.valueOf(workerId.split("_")[0])))){
					System.out.println("   Work and Home on the same link! Looking for new facility...");
					System.out.println("   Home: "+hh_HomeFacilities.get(Integer.valueOf(workerId.split("_")[0]))+","+facilityLinks.get(hh_HomeFacilities.get(Integer.valueOf(workerId.split("_")[0])))+"   Work: "+workFacilityId.toString()+","+facilityLinks.get(workFacilityId));
				}
				else{
					workAtSameLinkFromHome=false;
				}		
				
			}	
			person_WorkFacilities.put(workerId, workFacilityId);
			
		}
		
		bulkInsert(dba.getConnection(), person_WorkFacilities);
		dba.close();

		System.out.println("Done!");
	}

	public static Integer[][] readODMatrix(String filepath) throws IOException{

		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		Integer row = -1;
		Integer[][] matrix = new Integer[24][24];
		try {
			while (true) {
				row++;
				String line = reader.readLine();
				if (line == null) break;
				String[] fields = line.split(",");
				Integer column=-1;
				for(String trips:fields){
					column++;
					matrix[row][column] = Integer.valueOf(trips);
				}
			}
		} finally {
			reader.close();
		}
		return matrix;
	}
	
	public static void bulkInsert(java.sql.Connection connection, HashMap<String, String> hashOfValues) throws SQLException {

		// First create a statement off the connection and turn off unique checks and key creation
		Statement statement = (com.mysql.jdbc.Statement)connection.createStatement();
		statement.execute("DROP TABLE IF EXISTS sf_workplaces"); 
		statement.execute("CREATE TABLE sf_workplaces(synth_person_id VARCHAR(10), synth_hh_id VARCHAR(10), workFacility VARCHAR(10))"); 
		statement.execute("SET UNIQUE_CHECKS=0; ");
		statement.execute("ALTER TABLE sf_workplaces DISABLE KEYS");

		// Define the query we are going to execute
		String statementText = "LOAD DATA LOCAL INFILE 'whatever.txt' "+
				"INTO TABLE sf_workplaces";

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
			builder.append('\n');
		}

		// Create stream from String Builder
		InputStream is = IOUtils.toInputStream(builder.toString());

		// Setup our input stream as the source for the local infile
		statement.setLocalInfileInputStream(is);

		// Execute the load infile
		statement.execute(statementText);

		// Turn the checks back on
		statement.execute("ALTER TABLE sf_workplaces ENABLE KEYS");
		statement.execute("SET UNIQUE_CHECKS=1; ");
	}

}


