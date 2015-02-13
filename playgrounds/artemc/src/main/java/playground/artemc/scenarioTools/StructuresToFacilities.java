package playground.artemc.scenarioTools;

import com.mysql.jdbc.Statement;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.*;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import playground.artemc.utils.DataBaseAdmin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;


public class StructuresToFacilities {

	private static final Logger log = Logger.getLogger(UpdateNodesFromShape.class);
	private static DataBaseAdmin dba;
	static java.sql.PreparedStatement dbFacility;
	static java.sql.PreparedStatement dbAcitivity;


	private static Double avgHouseholdSize = 2.0;
	//Home 00:00 - 30:00
	private static Tuple<Integer, Integer> homeOpeningTimes= new Tuple<Integer, Integer>(0, 108000);
	//Work 08:00 - 18:00
	private static Tuple<Integer, Integer> workOpeningTimes= new Tuple<Integer, Integer>(28800, 64800);
	//Secondary 08:00 - 20:00
	private static Tuple<Integer, Integer> secondaryOpeningTimes= new Tuple<Integer, Integer>(28800, 72000);
	//Educational 08:00 - 16:00
	private static Tuple<Integer, Integer> eduOpeningTimes= new Tuple<Integer, Integer>(27000, 57600);


	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		StringBuilder builder = new StringBuilder();
		String shapeFilePath = args[0];
		String facilitiesFilePath = args[1];
		String networkPath = args[2];

		//BufferedWriter incomeWriter = new BufferedWriter( new FileWriter("../roadpricingSingapore/scenarios/siouxFalls/hh_incomes.csv"));
		dba = new DataBaseAdmin(new File("./data/dataBases/SiouxFalls_DataBase.properties"));

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkPath); 
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Double x = 0.0;
		Double y = 0.0;

		Double capacity = 1.0;
		Integer nodeZoneId = 0;
		Integer tractId = 0;
		Integer structure = 0;
		Integer meanIncome = 0;
		Integer marginError = 0;
		Double units = 0.0;
		Integer generatedHouseholdIncome;


		Random generator = new Random();	

		//String shapeFile = "C:/Work/Roadpricing Scenarios/SiouxFalls/Network/SiouxFalls_nodes.shp";


		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("SiouxFalls Facilities");

		System.out.println(shapeFilePath);
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		Collection<SimpleFeature> fts = shapeFileReader.readFileAndInitialize(shapeFilePath); 		
		log.info("Shape file contains "+fts.size()+" features!");		


		for(SimpleFeature ft:fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coordinates = geo.getCoordinates();
			Collection<Property> properties = ft.getProperties();
			Collection<Property> propertiesStructure = ft.getProperties("STRUCTURE");
			Collection<Property> propertiesUnits = ft.getProperties("UNITS");
			Collection<Property> propertiesZoneId = ft.getProperties("NodeZoneId");
			Collection<Property> propertiesTractId = ft.getProperties("TractId");
			//Collection<Property> propertiesMeanIncome = ft.getProperties("DP3_HC01_V");
			//Collection<Property> propertiesMarginError = ft.getProperties("DP3_HC02_V");


			String facilityNumber = ft.getID().split("\\.")[1];

			System.out.println("Feature: "+ft.getID().split("\\.")[1]+","+ft.getIdentifier()+","+ft.getName().toString());

			for(int i=0;i<coordinates.length;i++){
				System.out.println(coordinates[i].x+","+coordinates[i].y+"   ");
				x = coordinates[i].x;
				y = coordinates[i].y;
			}		

			for (Property p :propertiesStructure) {
				System.out.println("Name: "+p.getName().toString());
				System.out.println("Value: "+p.getValue().toString());	
				System.out.println();		    	
				structure = Integer.valueOf(p.getValue().toString());
			}

			for (Property p :propertiesUnits) {
				System.out.println("Name: "+p.getName().toString());
				System.out.println("Value: "+p.getValue().toString());	
				System.out.println();		
				units = Double.valueOf(p.getValue().toString());
				capacity = units*avgHouseholdSize;
			}

			for (Property p :propertiesZoneId) {
				System.out.println("Name: "+p.getName().toString());
				System.out.println("Value: "+p.getValue().toString());	
				System.out.println();		
				nodeZoneId = Integer.valueOf(p.getValue().toString());
			}


			for (Property p :propertiesTractId) {
				System.out.println("Name: "+p.getName().toString());
				System.out.println("Value: "+p.getValue().toString());	
				System.out.println();		
				tractId = Integer.valueOf(p.getValue().toString());
			}

			//			for (Property p :propertiesMeanIncome) {
			//				System.out.println("Name: "+p.getName().toString());
			//				System.out.println("Value: "+p.getValue().toString());	
			//				System.out.println();		
			//				meanIncome = Integer.valueOf(p.getValue().toString());
			//			}
			//
			//			for (Property p :propertiesMarginError) {
			//				System.out.println("Name: "+p.getName().toString());
			//				System.out.println("Value: "+p.getValue().toString());	
			//				System.out.println();		
			//				marginError = Integer.valueOf(p.getValue().toString());
			//			}

			System.out.println();
			System.out.println();

			Id<ActivityFacility> facilityID = Id.create(facilityNumber+"_"+nodeZoneId, ActivityFacility.class);

			generatedHouseholdIncome = (int) Math.round(marginError*generator.nextGaussian() + meanIncome);

			
			System.out.println(nodeZoneId);
			if(nodeZoneId>0){	
				facilities.createAndAddFacility(facilityID, new CoordImpl(x,y));
				ActivityFacilityImpl facility = (ActivityFacilityImpl) facilities.getFacilities().get(facilityID);
				facility.setLinkId(NetworkUtils.getNearestLink(network, facility.getCoord()).getId());
				ArrayList<ActivityOptionImpl> activities = getActivity(structure, capacity);		
				String activityOptions = "";
				for(ActivityOption act:activities){
					facilities.getFacilities().get(facilityID).getActivityOptions().put(act.getType(), act);
					if(!activityOptions.equals("")){
						activityOptions = activityOptions + ","+act.getType();
					}
					else{
						activityOptions = act.getType();
					}
				}
				//facilities.getFacilities().get(facilityID).getCustomAttributes().put("household_income", generatedHouseholdIncome);
				//incomeWriter.write(facilityID.toString()+","+generatedHouseholdIncome+"\n");

				if(!activities.isEmpty()){
					appendToBuilder(builder, (ActivityFacilityImpl) facilities.getFacilities().get(facilityID), nodeZoneId, tractId, structure, units, activityOptions);
				}
				//writeToSQL((ActivityFacilityImpl) facilities.getFacilities().get(facilityID), nodeZoneId, tractId, structure, units, activityOptions);
			}
		}
		System.out.println("Total feautures: "+fts.size());
		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(facilitiesFilePath);
		//incomeWriter.close();
		System.out.println("  writing database table... ");
		bulkInsert(dba.getConnection(), builder);
		System.out.println("  done.");
	}

	private static ArrayList<ActivityOptionImpl> getActivity(Integer structure, Double capacity) {
		ArrayList<ActivityOptionImpl> activities = new ArrayList<ActivityOptionImpl>();
		switch(structure){
		//Residential
		case 100: case 110: case 120: case 121: case 122: case 130: case 140: case 150: case 151: case 152: case 153: case 200: case 210: case 220:
			activities.add(new ActivityOptionImpl("home"));
			activities.get(0).setCapacity(capacity);
			activities.get(0).addOpeningTime(new OpeningTimeImpl(homeOpeningTimes.getFirst(), homeOpeningTimes.getSecond()));
			break;
			//Office, Government
		case 300: case 310: case 320: case 330: case 350: case 360: case 363: case 365:
			activities.add(new ActivityOptionImpl("work"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			break;
			//Office-drive through, office/commercial, post, gym, 
		case 311: case 321: case 331: case 345: case 366: case 470:
			activities.add(new ActivityOptionImpl("work"));
			activities.add(new ActivityOptionImpl("secondary"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			activities.get(1).addOpeningTime(new OpeningTimeImpl(secondaryOpeningTimes.getFirst(), secondaryOpeningTimes.getSecond()));
			break;
			//Office,Residential	
		case 340: 
			activities.add(new ActivityOptionImpl("home"));
			activities.get(0).setCapacity(capacity/2);
			activities.get(0).addOpeningTime(new OpeningTimeImpl(homeOpeningTimes.getFirst(), homeOpeningTimes.getSecond()));
			activities.add(new ActivityOptionImpl("work"));
			activities.get(1).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			break;
			//Medical facility
		case 430: case 431: case 432: case 433: case 434: case 435: case 436:
			activities.add(new ActivityOptionImpl("work"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			break;
			//Educational facility
		case 450: case 451: case 452: case 453:
			activities.add(new ActivityOptionImpl("work"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			activities.add(new ActivityOptionImpl("edu"));
			activities.get(1).addOpeningTime(new OpeningTimeImpl(eduOpeningTimes.getFirst(), eduOpeningTimes.getSecond()));
			break;	
			//Educational and Cultural facility
		case 440: case 460: case 461:  case 462:
			activities.add(new ActivityOptionImpl("work"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			break;

			//Commercial
		case 500: case 510: case 511: case 512: case 513: case 514: case 515: case 516: case 520: case 525: case 531: case 532: case 533: case 534:
			activities.add(new ActivityOptionImpl("work"));
			activities.add(new ActivityOptionImpl("secondary"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			activities.get(1).addOpeningTime(new OpeningTimeImpl(secondaryOpeningTimes.getFirst(), secondaryOpeningTimes.getSecond()));
			break;
			//Commercial,Residential	
		case 530: 
			activities.add(new ActivityOptionImpl("home"));
			activities.get(0).setCapacity(capacity/2);
			activities.get(0).addOpeningTime(new OpeningTimeImpl(homeOpeningTimes.getFirst(), homeOpeningTimes.getSecond()));

			activities.add(new ActivityOptionImpl("work"));
			activities.get(1).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));

			activities.add(new ActivityOptionImpl("secondary"));
			activities.get(2).addOpeningTime(new OpeningTimeImpl(secondaryOpeningTimes.getFirst(), secondaryOpeningTimes.getSecond()));
			break;
			//Industrial
		case 600: case 620: case 630: case 631: case 632: case 640: case 641: case 642: case 643:
			activities.add(new ActivityOptionImpl("work"));
			activities.get(0).addOpeningTime(new OpeningTimeImpl(workOpeningTimes.getFirst(), workOpeningTimes.getSecond()));
			break;
		}
		return activities;
	}

	/*The method writes facility data to SQL table. The table has to predefined and already have right number of columns. 
	 * ATTENTION: The maximal number of activities per facility have to known and the table with corresponding number of columns has to be create beforehand. 
	 * Only one opening time for each activity is written into table (should be made more flexible in the future).  
	 */
//	private static void writeToSQL(ActivityFacilityImpl facility, Integer nodeZoneId, Integer tractZoneId, Integer structure, Double units, String activity) throws SQLException{
//		dbFacility = dba.getConnection().prepareStatement("INSERT INTO sf_facilities VALUES (?,?,?,?,?,?,?,?)");	
//		dbFacility.setString(1,facility.getId().toString());
//		dbFacility.setString(2,facility.getLinkId().toString());
//		dbFacility.setDouble(3,facility.getCoord().getX());
//		dbFacility.setDouble(4,facility.getCoord().getY());	
//		dbFacility.setInt(5,nodeZoneId);	
//		dbFacility.setInt(6,tractZoneId);	
//		dbFacility.setInt(7,structure);	
//		dbFacility.setDouble(8,units);	
//		dbFacility.setString(9,activity);	
//		dbFacility.executeUpdate();
//		dbFacility.close();
//
//		for(ActivityOption act:facility.getActivityOptions().values()){
//			dbAcitivity = dba.getConnection().prepareStatement("INSERT INTO sf_activities VALUES (?,?,?,?,?)");	
//			dbAcitivity.setString(1,act.getType());
//			dbAcitivity.setDouble(2,act.getCapacity());
//			for(OpeningTime t:((ActivityOptionImpl) act).getOpeningTimes()){
//				dbAcitivity.setDouble(3,t.getStartTime());
//				dbAcitivity.setDouble(4,t.getEndTime());
//				break;
//			}
//			dbAcitivity.setString(5,facility.getId().toString());
//			dbAcitivity.executeUpdate();
//			dbAcitivity.close();
//		}
//
//	}
	
	
	public static void appendToBuilder(StringBuilder builder, ActivityFacilityImpl facility, Integer nodeZoneId, Integer tractZoneId, Integer structure, Double units, String activity){
		// Create StringBuilder to String that will become stream
		builder.append(facility.getId().toString());
		builder.append('\t');
		builder.append(facility.getLinkId().toString());
		builder.append('\t');
		builder.append(facility.getCoord().getX());
		builder.append('\t');
		builder.append(facility.getCoord().getY());
		builder.append('\t');
		builder.append(nodeZoneId);
		builder.append('\t');
		builder.append(tractZoneId);
		builder.append('\t');
		builder.append(structure);
		builder.append('\t');
		builder.append(units);
		builder.append('\t');
		builder.append(activity);
		builder.append('\n');
	}
	
	public static void bulkInsert(java.sql.Connection connection, StringBuilder builder) throws SQLException {

		// First create a statement off the connection and turn off unique checks and key creation
		Statement statement = (com.mysql.jdbc.Statement)connection.createStatement();
		statement.execute("DROP TABLE IF EXISTS sf_facilities"); 
		statement.execute("CREATE TABLE sf_facilities(id VARCHAR(8), lind_id VARCHAR(8), x DOUBLE, y DOUBLE, nodeZoneId INT(8), tractZoneId INT(8), structure INT(8), units DOUBLE, activities VARCHAR(20))"); 
		statement.execute("SET UNIQUE_CHECKS=0; ");
		statement.execute("ALTER TABLE sf_facilities DISABLE KEYS");

		// Define the query we are going to execute
		String statementText = "LOAD DATA LOCAL INFILE 'whatever.txt' "+
				"INTO TABLE sf_facilities";

	
		
		// Create stream from String Builder
		InputStream is = IOUtils.toInputStream(builder.toString());

		// Setup our input stream as the source for the local infile
		statement.setLocalInfileInputStream(is);

		// Execute the load infile
		statement.execute(statementText);

		// Turn the checks back on
		statement.execute("ALTER TABLE sf_facilities ENABLE KEYS");
		statement.execute("SET UNIQUE_CHECKS=1; ");
	}
}		

