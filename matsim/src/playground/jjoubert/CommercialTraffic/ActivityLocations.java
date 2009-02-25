package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class ActivityLocations {
	
	public final long STUDY_START = 1199145600; // 01 January 2008 00:00:00
	private final static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private final static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";

	final static String SOURCEFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Sorted/";
	final static String DESTFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Activities/";
	final static String VEH_FOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/XML/";
//	final static String SOURCEFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/Vehicles/";
//	final static String DESTFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/Activities/";
//	final static String VEH_FOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/XML/";
	final static String DELIMITER_IN = " "; // Could also be ',' or other;
	final static String DELIMITER_OUT = ","; // Could also be ' ';
	final static int[] statusStart = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,16,17,18,20};
	final static int[] statusStop = {15,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,145,147};
	final static int HOME_DURATION_THRESHOLD = 300; // 5 hours for home (expressed in MINUTES)
	final static int ACTIVITY_MIN_THRESHOLD = 8; // expressed in MINUTES
	final static int DISTANCE_THRESHOLD = Vehicle.DISTANCE_THRESHOLD;
	public static int progressDots;
	public static Polygon gauteng;
	public static int numberOfBoinkPoints = 0;

		
	public static void main( String args[] ) {
		long startTime = System.currentTimeMillis();
		final File inFolder = new File( SOURCEFOLDER );
		final File vehicles[] = inFolder.listFiles();
		int numberOfVehicles = 0;
		int totalVehicles = vehicles.length;
		int numberOfMajorActivities = 0;
		int numberOfMinorActivities = 0;
		
		MathTransform mt = getMathTransform(); // Prepare for geometric transformations
		
		System.out.println("Reading study area: Gauteng");
		gauteng = SelectGautengVehicles.readGautengPolygon();
		System.out.println("Done");
		System.out.println();
		
		System.out.println("Processing vehicle files in " + DESTFOLDER + "...");
		System.out.println();
		printProgressBar();
		
		try {
			File outFolder = new File( DESTFOLDER );
			outFolder.mkdir();
			File vehFolder = new File( VEH_FOLDER );
			vehFolder.mkdir();
			
			// Create all the output file writers 
			BufferedWriter vehicleStats = new BufferedWriter(new FileWriter(new File(DESTFOLDER + "vehicleStats.txt")));
			vehicleStats.write(	vehicleStatsHeaderString() );
			vehicleStats.newLine();			
			BufferedWriter majorLocations = new BufferedWriter(new FileWriter(new File(DESTFOLDER + "majorLocations.txt")));
			majorLocations.write( locationHeaderString() );
			majorLocations.newLine();
			BufferedWriter minorLocations = new BufferedWriter(new FileWriter(new File(DESTFOLDER + "minorLocations.txt")));
			minorLocations.write( locationHeaderString() );
			minorLocations.newLine();
			BufferedWriter hourOfDayInGauteng = new BufferedWriter(new FileWriter(new File(DESTFOLDER + "hourOfDayInGautengStats.txt")));
			hourOfDayInGauteng.write( gautengHeaderString() );
			hourOfDayInGauteng.newLine();
			
			for(int i = 0; i < vehicles.length; i++ ){
				File thisFile = vehicles[i];
				if(thisFile.isFile() && !(thisFile.getName().startsWith(".")) ){ // avoid .* file names on Mac
					Vehicle thisVehicle = createNewVehicle(thisFile);
					ArrayList<GPSPoint> log = readFileToArray(thisFile, mt);				
					processVehicleActivities(thisVehicle, thisFile, log);

					if(thisVehicle.getChains().size() > 0){
						// Write home locations to file
						for(Activity majorActivity: thisVehicle.getHomeLocation() ){
							majorLocations.write( locationString(majorActivity) );
							majorLocations.newLine();
							numberOfMajorActivities++;
						}
						
						// Write activity locations to file 
						for(Chain thisChain: thisVehicle.getChains() ){
							for(Activity minorActivity: thisChain.getActivities() ){
								minorLocations.write( locationString(minorActivity) );
								minorLocations.newLine();
								numberOfMinorActivities++;
							}
						}
						
						// Write vehicle statistics	to file					
						vehicleStats.write(	statsString(thisVehicle) );
						vehicleStats.newLine();
		
						saveVehicleStringToFile(thisVehicle.getVehID(), convertVehicleToXML(thisVehicle) );
					}
					
					// Write hour-of-day-in-Gauteng statistics
					if(thisVehicle.getGautengActivities().size() > 0){
						for(int j = 0; j < thisVehicle.getGautengActivities().size(); j++ ){
							hourOfDayInGauteng.write( gautengString(thisVehicle, j) );
							hourOfDayInGauteng.newLine();
						}
					}
					numberOfVehicles++;				
					updateProgress(numberOfVehicles, totalVehicles);
				}
			}
			vehicleStats.close();
			majorLocations.close();
			minorLocations.close();
			hourOfDayInGauteng.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println();
		System.out.println();
		System.out.print("Writing locations to file... " );
		System.out.println("Done" );
		System.out.println();
		
		long endTime = System.currentTimeMillis();
		
		System.out.println("-------------- SUMMARY  --------------");
		System.out.println("Number of vehicles processed: " + numberOfVehicles );
		System.out.println("Total home locations: " + numberOfMajorActivities );
		System.out.println("Total activity locations: " + numberOfMinorActivities );
		System.out.println("Total time (sec): " + ((int)(((double)(endTime - startTime)) / 1000) ) );
		System.out.println("Boink points: " + numberOfBoinkPoints );
		System.out.println("--------------------------------------");
		
	}


	private static MathTransform getMathTransform() {
		MathTransform mt = null;
		try{
			final CoordinateReferenceSystem sourceCRS = CRS.parseWKT( WGS84 );
			final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM35S );
			mt = CRS.findMathTransform(sourceCRS, targetCRS, true);
		} catch(Exception e){
			e.printStackTrace();
		}
		return mt;
	}


	private static String gautengHeaderString() {
		return  "Veh_ID" + DELIMITER_OUT + 
				"Long" + DELIMITER_OUT + 
				"Lat" + DELIMITER_OUT + 
				"Hour_of_Day_Start" + DELIMITER_OUT + 
				"Duration";
	}

	private static String gautengString(Vehicle thisVehicle, int i) {
		String outputString = String.valueOf( thisVehicle.getVehID() ) + DELIMITER_OUT +
							  String.valueOf(thisVehicle.getGautengActivities().get(i).getLocation().getCoordinate().x ) + DELIMITER_OUT +
							  String.valueOf( thisVehicle.getGautengActivities().get(i).getLocation().getCoordinate().y ) + DELIMITER_OUT +
							  String.valueOf( thisVehicle.getGautengActivities().get(i).getStartHour() ) + DELIMITER_OUT + 
							  String.valueOf( thisVehicle.getGautengActivities().get(i).getDuration() );
		return outputString;
	}

	private static String vehicleStatsHeaderString() {
		return "VehicleID" + DELIMITER_OUT + 
			   "Number_of_home_locations" + DELIMITER_OUT + 
			   "Number_of_chains" + DELIMITER_OUT + 
			   "Avg_chain_duration" + DELIMITER_OUT + 
			   "Avg_chain_distance" + DELIMITER_OUT +
			   "Avg_activities_per_chain" + DELIMITER_OUT +
			   "Total_activities" + DELIMITER_OUT + 
			   "Total_gauteng_activities" + DELIMITER_OUT +
			   "Percent_gauteng_activities" + DELIMITER_OUT +
			   "Gauteng_Chain_Distance";
	}

	private static String statsString(Vehicle thisVehicle) {
		return thisVehicle.getVehID() + DELIMITER_OUT + 
			   thisVehicle.getHomeLocation().size() + DELIMITER_OUT +
			   thisVehicle.getChains().size() + DELIMITER_OUT + 
			   thisVehicle.getAvgChainDuration() + DELIMITER_OUT + 
			   thisVehicle.getAvgChainDistance() + DELIMITER_OUT +
			   thisVehicle.getAvgActivitesPerChain() + DELIMITER_OUT +
			   thisVehicle.getTotalActivities() + DELIMITER_OUT +
			   thisVehicle.getNumberOfGautengActivities() + DELIMITER_OUT +
			   thisVehicle.getPercentGautengActivities() + DELIMITER_OUT +
			   thisVehicle.getGautengChainDistance();
	}
	
	private static void printProgressBar() {
		System.out.println("0%                 20%                 40%                 60%                 80%               100%");
		System.out.print("|");
		for(int i = 1; i <= 10; i++ ){
			for(int j = 1; j <= 9; j++ ){
				System.out.print("-");
			}
			System.out.print("|");
		}
		System.out.println();
		System.out.print("*");
	}

	private static void updateProgress(float vehicles, float totalVehicles) {
		int percentage = (int) (( vehicles / totalVehicles )*100);
		int dotsAdd = percentage - progressDots;
		for(int i = 1; i <= dotsAdd; i++ ){
			System.out.print("*");
		}
		progressDots += dotsAdd;
	}
	
	private static String locationHeaderString(){
		String s = "ID" + DELIMITER_OUT + 
				   "Long" + DELIMITER_OUT + 
				   "LAT" + DELIMITER_OUT + 
				   "Start" + DELIMITER_OUT + 
				   "Duration";
		return s;
	}
	
	private static String locationString(Activity activity){
		String hourSpace;
		String minSpace;
		String secSpace;

		if( activity.getStartTime().get(GregorianCalendar.HOUR_OF_DAY) < 10 ){
			hourSpace = "0";
		} else{
			hourSpace = "";
		}
		if( activity.getStartTime().get(GregorianCalendar.MINUTE) < 10 ){
			minSpace = "0";
		} else{
			minSpace = "";
		}
		if( activity.getStartTime().get(GregorianCalendar.SECOND) < 10 ){
			secSpace = "0";
		} else{
			secSpace = "";
		}
		
		String s = // vehicle
				   activity.getLocation().getVehID() + DELIMITER_OUT +
				   // coordinate
				   activity.getLocation().getCoordinate().x + DELIMITER_OUT + 
				   activity.getLocation().getCoordinate().y + DELIMITER_OUT +
				   // the day and time
				   "Day_" + 
				   activity.getStartTime().get(GregorianCalendar.DAY_OF_YEAR) + "_" +
				   hourSpace + activity.getStartTime().get(GregorianCalendar.HOUR_OF_DAY) + "H" +
				   minSpace + activity.getStartTime().get(GregorianCalendar.MINUTE) + ":" + 
				   secSpace + activity.getStartTime().get(GregorianCalendar.SECOND) + DELIMITER_OUT + 
				   // duration
				   activity.getDuration();	
		return s;
	}

	private static void processVehicleActivities( Vehicle thisVehicle, File file, ArrayList<GPSPoint> log) {
		
		findNextVehicleStop(log); // Clean all points until first start		
		ArrayList<Activity> activityList = extractActivities(file, log); // Find all the activities
		extractChains(thisVehicle, activityList);
		thisVehicle.updateVehicleStatistics(gauteng);		
	}

	private static ArrayList<Activity> extractActivities(File file,
			ArrayList<GPSPoint> log) {
		
		ArrayList<Activity> activityList = new ArrayList<Activity>(); // for the actual activities
		ArrayList<GPSPoint> locationList = new ArrayList<GPSPoint>(); // for the possible activity locations
		
		while(log.size() > 1){
			if( !startSignal(log.get(1).getStatus() ) ){
				locationList.add( log.get(1) );
				log.remove(1);
			} else{
				locationList.add( log.get(1) );
				// Get the gravity centroid of the possible locations.
				double x = 0;
				double y = 0;
				if( locationList.size() > 0 ){
					double xSum = 0;
					double ySum = 0;
					for( GPSPoint gps: locationList ){
						xSum += gps.getCoordinate().x;
						ySum += gps.getCoordinate().y;
					}
					x = xSum / locationList.size();
					y = ySum / locationList.size();
				} else{
					System.out.println("There does not seem to be a possible location for this activitiy?!");
				}
				log.get(0).setCoordinate(new Coordinate(x, y) );
				activityList.add(new Activity( log.get(0).getTime(), log.get(1).getTime(), log.get(0) ) );

				// Check if location where activity stops is within the limits from where activity started 
				int distance = (int) ( log.get(1).getCoordinate().distance( log.get(0).getCoordinate() ) );
				if(distance > DISTANCE_THRESHOLD){
//					System.out.println("Woops... this activity starts and ends at different positions.");
					numberOfBoinkPoints++;
				}
				
				log.remove(0);
				log.remove(0);
				locationList = new ArrayList<GPSPoint>();
				
				findNextVehicleStop(log);
				
			}
		}
		return activityList;
	}

	private static void extractChains(Vehicle thisVehicle,
			ArrayList<Activity> activityList) {
		Chain thisChain = new Chain();
		while( activityList.size() > 0){
			Activity thisActivity = activityList.get(0);
			if ( thisActivity.getDuration() < ACTIVITY_MIN_THRESHOLD ){ // too short for an activity
				activityList.remove(0);				
			} else if(thisActivity.getDuration() < HOME_DURATION_THRESHOLD ){ // then it is an activity)
				if( thisChain.getDayStart() == null ){
					thisChain.setDayStart(thisActivity.getStartTime() ); // set start time
				}
				thisChain.addActivity( thisActivity );
				activityList.remove(0);
			} else{ // else it is a major (home) location
				thisChain.addActivity( thisActivity ); // add home location to end of current chain
				
				thisVehicle.addChain( thisChain );
				
				thisChain = new Chain();
				thisChain.setDayStart( thisActivity.getEndTime() );
				thisChain.addActivity( thisActivity ); // add home location at start of new chain				
				activityList.remove(0);
			}
		}
		thisVehicle.cleanChains();
		thisVehicle.extractMajorLocations();
	}

	
	private static void findNextVehicleStop(ArrayList<GPSPoint> log) {
		boolean startFound = false;
		while( !startFound && log.size() > 0){
			if( !stopSignal(log.get(0).getStatus()) ){ // TODO Must rather work with enumeration
				log.remove(0);
			} else{
				startFound = true;
			}			
		}
		if( !startFound && log.size() > 1){
			System.out.println("Vehicle does not contain a single valid trip." );
			return; //TODO Check how elegant this is. Not a better way to do this?
		}
	}

	private static boolean startSignal(int status) {
		for (int i = 0; i < statusStart.length; i++) {
			if(statusStart[i] == status ){
				return true;
			}
		}
		return false;
	}

	private static boolean stopSignal(int status) {
		for (int i = 0; i < statusStop.length; i++) {
			if(statusStop[i] == status ){
				return true;
			}
		}
		return false;
	}

	public static ArrayList<GPSPoint> readFileToArray(File thisFile, MathTransform mt) { // I decided to not read in the speed... useless in DigiCore set
		int vehID;
		long time;
		double longitude;
		double latitude;
		int status;
		
		ArrayList<GPSPoint> log = new ArrayList<GPSPoint>();
		try {

			Scanner input = new Scanner(new BufferedReader(new FileReader(thisFile) ) );
			while( input.hasNextLine() ){
				String [] inputString = input.nextLine().split(DELIMITER_IN);
				if( inputString.length > 5){
					try{
						vehID = Integer.parseInt( inputString[0] );
						time =  Long.parseLong( inputString[1] );
						longitude = Double.parseDouble( inputString[2] );
						latitude = Double.parseDouble( inputString[3] );
						status = Integer.parseInt( inputString[4] );
						
						Coordinate c = new Coordinate( longitude, latitude );
						JTS.transform(c, c, mt);
						log.add( new GPSPoint(vehID, time, status, c) );
					} catch(NumberFormatException e){
						System.out.print("");
					} 
				}
			}
			input.close();						
		} catch (Exception e) {
			e.printStackTrace();
		}
		return log;
	}

	private static Vehicle createNewVehicle(File file) {
		Integer vehicleID = new Integer( (String) file.getName().subSequence(0, (file.getName().length() - 4) ) );
		Vehicle thisVehicle = new Vehicle( vehicleID );
		return thisVehicle;		
	}
	
	public static boolean saveVehicleStringToFile(int vehID, String saveString){
		boolean saved = false;
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter( new FileWriter(VEH_FOLDER + "/" + vehID + ".xml"));
			try{
				bw.write(saveString);
				saved = true;
			} finally{
				bw.close();
			}
		} catch(IOException ex){
			ex.printStackTrace();
		}
		
		return saved;
	}
	
	public static String readVehicleStringFromFile(int vehID){
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		String vehicleFile = VEH_FOLDER + "/" + vehID + ".xml";
		
		try{
			br = new BufferedReader( new FileReader(vehicleFile) );
			try{
				String s;
				while( (s = br.readLine() ) != null ){
					sb.append(s);
					sb.append("\n");
				}
			} finally {
				br.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return sb.toString();		
	}
	
	public static String convertVehicleToXML (Vehicle vehicle){
		XStream xstream = new XStream(new DomDriver());		
		return xstream.toXML(vehicle);
	}
	
	public static Vehicle convertVehicleFromXML (String XMLString){
		Vehicle vehicle = null;
		XStream xstream = new XStream(new DomDriver());
		Object obj = xstream.fromXML(XMLString);
		if(obj instanceof Vehicle){
			vehicle = (Vehicle) obj;
		}
		return vehicle;
	}
	
}
