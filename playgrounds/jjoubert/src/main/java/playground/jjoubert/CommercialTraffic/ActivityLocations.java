package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.MyXmlConverter;
import playground.jjoubert.Utilities.ProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
/**
 * This class extracts the activity locations from inputs vehicle files. The class should
 * be invoked on each study area separately.
 *
 * @author jwjoubert
 */
public class ActivityLocations {
	/*
	 * String value that must be set. Allowed study areas are:
	 * 		- SouthAfrica
	 * 		- Gauteng
	 * 		- KZN
	 * 		- WesternCape
	 */
	 static String studyAreaName = "WesternCape";

	// Set the home directory, depending on where the job is executed.
//	final static String ROOT = "~/MATSim/workspace/MATSimData/"; 	// Mac
//	final static String ROOT = "~/";								// IVT-Sim0
	final static String ROOT = "~/data/";							// Satawal

	// Derived string values.
	private static String sourceFolderName = ROOT + "DigiCore/SortedVehicles/";
	private static String destinationFolderName = ROOT + studyAreaName+ "/Activities/";
	private static String vehicleFolderName = ROOT + studyAreaName + "/XML/";
	private static String shapeFileSource = ROOT + "Shapefiles/" + studyAreaName + "/" + studyAreaName + "_UTM35S.shp";

	// Geographic transformation strings.
	private static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";

	// Study-specific parameters that must be set.
	private static String delimiter = ",";
	private static int[] statusStart = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,16,17,18,20};
	private static int[] statusStop = {15,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,145,147};
	private static int majorActivityMinimumDuration = 300; // 5 hours for home (expressed in MINUTES)
	private static int minorActivityMinimumDuration = 8; // expressed in MINUTES
	private static int distanceThreshold = Vehicle.DISTANCE_THRESHOLD;
	private static MultiPolygon studyArea;

	// To determine the duration histogram
	private static int bucketSize = 30; // Expressed in MINUTES
	private static int bucketRange = 10080; /// Expressed in MINUTES (Here, a week)
	static int numBuckets = (bucketRange / bucketSize) + 1;
	static ArrayList<Integer> durationBuckets;
	// a 'BoinkPoint' is an activity that starts and ends at different locations. Currently I don't do anything with them.
	//TODO Sort out how to handle these 'BoinkPoint's.
	protected static int numberOfBoinkPoints = 0;
	private final static Logger log = Logger.getLogger(ActivityLocations.class);

	/**
	 * The main method calls various other methods, both within the class and from other
	 * classes.
	 * <h4>Requires:</h4>
	 * <ul>
	 * <li> A folder with vehicle files containing <b><i>sorted</i></b> GPS logs.
	 * 		Filenames should be <code>VehicleID.txt</code>, where <code>VehicleId</code>
	 * 		is the unique integer identifying the vehicle. </li>
	 * <li> A shapefile of the study area. In the case of South Africa, the <i>UTM35S</i>
	 * 		 coordinate system should be used. </li>
	 * </ul>
	 * <h4>Produces:</h4>
	 * In all the output files, the phrase <code>Area</code> refers to the study area:
	 * <ul>
	 * <li> A folder called <code>/XML/</code> with xml vehicle files of all vehicles
	 * 		performing activities in the given study area. Each <code>VehicleId.xml</code>
	 * 		file contains the complete vehicle with all its chains, activities, and
	 * 		statistics related to the study area. An <code>xml</code> file can be read
	 * 		in with <code>playground.jjoubert.Utilities.MyXmlConverter.java</code> to
	 * 		reproduce the <code>Vehicle</code> object. </li>
	 * <li> A text file <code>AreaActivityDurations.txt</code>. All activity durations,
	 * 		both <i>minor</i> and <i>major</i>, are aggregated (binned) into 30-minute
	 * 		intervals. </li>
	 * <li> A text file <code>AreaVehicleStats.txt</code> with statistics of each vehicle.
	 *		the statistics include the number of <i>major</i> locations; number of chains;
	 *		average chain distance and duration; average number of <i>minor</i> activities
	 *		per chain; total number of activities; percentage activities in the study area;
	 *		and the total distance of all chains that passes through the study are. </li>
	 * <li> A text file called <code>AreaMinorLocations.txt</code> listing each <i>minor</i>
	 * 		activity found in the study area, along with its latitude and longitude
	 * 		coordinates (expressed as decimal degrees in the <i>UTM35S</i> coordinate
	 * 		system), the activity's start time, and the duration (in minutes). </li>
	 * <li> A text file called <code>AreaMajorLocations.txt</code> listing each <i>major</i>
	 * 		activity found in the study area, along with its latitude and longitude
	 * 		coordinates (expressed as decimal degrees in the <i>UTM35S</i> coordinate
	 * 		system), the activity's start time, and the duration (in minutes). </li>
	 * </ul>
	 *
	 * @param args
	 */
	public static void main( String args[] ) {
		log.info("==========================================================================================");
		log.info("Identifying vehicle activity locations for vehicles travelling through: " + studyAreaName );
		log.info("==========================================================================================");
		long startTime = System.currentTimeMillis();
		final File inFolder = new File( sourceFolderName );
		final File vehicles[] = inFolder.listFiles();
		int numberOfVehicles = 0;
		int totalVehicles = vehicles.length;
		int numberOfMajorActivities = 0;
		int numberOfMinorActivities = 0;

		// Create an empty time ArrayList
		durationBuckets = new ArrayList<Integer>();
		for(int a = 1; a <= numBuckets; a++ ){
			durationBuckets.add(0);
		}

		MathTransform mt = getMathTransform(); // Prepare for geometric transformations

		log.info("Reading study area: " + studyAreaName );
		MyShapefileReader msr = new MyShapefileReader(shapeFileSource);
		studyArea = msr.readMultiPolygon();
		studyArea.getCentroid();
		log.info("Done rreading study area.");
		log.info("Processing vehicle files in " + destinationFolderName + "...");

		ProgressBar pb = new ProgressBar('*', totalVehicles);
		pb.printProgressBar();

		try {
			File outFolder = new File( destinationFolderName );
			outFolder.mkdirs();
			File vehFolder = new File( vehicleFolderName );
			boolean checkCreate = vehFolder.mkdirs();
			if(!checkCreate){
				log.warn("Could not create " + vehicleFolderName + ", or it already exists!");
			}

			// Create all the output file writers, and write each's header.
			BufferedWriter vehicleStats = new BufferedWriter(new FileWriter(new File(destinationFolderName + studyAreaName + "VehicleStats.txt")));
			writeVehicleStatsHeader(vehicleStats);
			BufferedWriter majorLocations = new BufferedWriter(new FileWriter(new File(destinationFolderName + studyAreaName + "MajorLocations.txt")));
			writeLocationHeader(majorLocations);
			BufferedWriter minorLocations = new BufferedWriter(new FileWriter(new File(destinationFolderName + studyAreaName + "MinorLocations.txt")));
			writeLocationHeader(minorLocations);
			BufferedWriter durationOutput = new BufferedWriter(new FileWriter(new File(destinationFolderName + studyAreaName + "ActivityDurations.txt")));
			durationOutput.write("Bin,Number_of_Activities");
			durationOutput.newLine();

			try{
				for(int i = 0; i < vehicles.length; i++ ){
					File thisFile = vehicles[i];
					if(thisFile.isFile() && !(thisFile.getName().startsWith(".")) ){ // avoid .* file names on Mac
						Vehicle thisVehicle = createNewVehicle(thisFile);
						ArrayList<GPSPoint> log = readFileToArray(thisFile, mt);
						processVehicleActivities(thisVehicle, thisFile, log);

						if(thisVehicle.getChains().size() > 0){
							// Write major locations to file
							for(Activity majorActivity: thisVehicle.getHomeLocation() ){
								writeLocationLine(majorLocations, majorActivity);
								numberOfMajorActivities++;
							}

							// Write minor locations to file
							for(Chain thisChain: thisVehicle.getChains() ){
								// Do NOT consider the major locations at the end-points of each chain
								for(int j = 1; j < thisChain.getActivities().size() - 1; j++){
									writeLocationLine(minorLocations, thisChain.getActivities().get(j));
									numberOfMinorActivities++;
								}
							}

							// Write vehicle statistics	to file
							writeVehicleStatsLine(vehicleStats, thisVehicle);

							// Writing the vehicle as an XML file
							MyXmlConverter mxc = new MyXmlConverter();
							String vehicleFilenameXml = vehicleFolderName + thisVehicle.getVehID() + ".xml";
							mxc.writeObjectToFile(thisVehicle, vehicleFilenameXml);
						}
						numberOfVehicles++;
						pb.updateProgress(numberOfVehicles);
					}
				}
				for (int i = 0; i < durationBuckets.size(); i++) {
					durationOutput.write(String.valueOf((i+1)*10));
					durationOutput.write(delimiter);
					durationOutput.write(String.valueOf(durationBuckets.get(i)));
					durationOutput.newLine();
				}

			}finally{
				vehicleStats.close();
				majorLocations.close();
				minorLocations.close();
				durationOutput.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		long endTime = System.currentTimeMillis();

		log.info("--------------------------------------");
		log.info("Summary for: " + studyAreaName );
		log.info("--------------------------------------");
		log.info("Number of vehicles processed: " + numberOfVehicles );
		log.info("Total home locations: " + numberOfMajorActivities );
		log.info("Total activity locations: " + numberOfMinorActivities );
		log.info("Total time (sec): " + ((int)(((double)(endTime - startTime)) / 1000) ) );
		log.info("Boink points: " + numberOfBoinkPoints );
		log.info("--------------------------------------");

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

	private static void writeVehicleStatsHeader(BufferedWriter output) throws IOException {
		output.write("VehicleID");
		output.write(delimiter);
		output.write("Number_of_home_locations");
		output.write(delimiter);
		output.write("Number_of_chains");
		output.write(delimiter);
		output.write("Avg_chain_duration");
		output.write(delimiter);
		output.write("Avg_chain_distance");
		output.write(delimiter);
		output.write("Avg_activities_per_chain");
		output.write(delimiter);
		output.write("Total_activities");
		output.write(delimiter);
		output.write("Total_studyArea_activities");
		output.write(delimiter);
		output.write("Percent_studyArea_activities");
		output.write(delimiter);
		output.write("StudyArea_Chain_Distance");
		output.newLine();
	}

	/**
	 * Method introduced just to clean up the main method. It simply writes out the given
	 * vehicle's statistics to file.
	 * @param output the <code>BufferedWriter</code> to which output is written;
	 * @param thisVehicle the vehicle whose statistics are written;
	 * @throws IOException if the file is not found. This should never be the case, since
	 * 	       the file is opened in the main method.
	 */
	private static void writeVehicleStatsLine(BufferedWriter output, Vehicle thisVehicle) throws IOException {
		output.write(String.valueOf(thisVehicle.getVehID()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getHomeLocation().size()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getChains().size()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getAvgChainDuration()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getAvgChainDistance()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getAvgActivitesPerChain()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getTotalActivities()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getNumberOfStudyAreaActivities()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getPercentStudyAreaActivities()));
		output.write(delimiter);
		output.write(String.valueOf(thisVehicle.getStudyAreaChainDistance()));
		output.newLine();
	}

	private static void writeLocationHeader(BufferedWriter output) throws IOException{
		output.write("ID");
		output.write(delimiter);
		output.write("Long");
		output.write(delimiter);
		output.write("Lat");
		output.write(delimiter);
		output.write("Start");
		output.write(delimiter);
		output.write("Duration");
		output.newLine();
	}

	private static void writeLocationLine(BufferedWriter output, Activity activity) throws IOException{
		output.write(String.valueOf(activity.getLocation().getVehID()));			// vehicle Id
		output.write(delimiter);
		output.write(String.valueOf(activity.getLocation().getCoordinate().x));	// Longitude
		output.write(delimiter);
		output.write(String.valueOf(activity.getLocation().getCoordinate().y)); // Latitude
		output.write(delimiter);

		/*
		 * Create a nice way of reporting the day and time.
		 */
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
		output.write("Day_");
		output.write(String.valueOf(activity.getStartTime().get(GregorianCalendar.DAY_OF_YEAR)));
		output.write("_");
		output.write(hourSpace);
		output.write(String.valueOf(activity.getStartTime().get(GregorianCalendar.HOUR_OF_DAY)));
		output.write("H");
		output.write(minSpace);
		output.write(String.valueOf(activity.getStartTime().get(GregorianCalendar.MINUTE)));
		output.write(":");
		output.write(secSpace);
		output.write(String.valueOf(activity.getStartTime().get(GregorianCalendar.SECOND)));
		output.write(delimiter);

		output.write(String.valueOf(activity.getDuration()));						// duration

		output.newLine();

	}

	private static void processVehicleActivities( Vehicle thisVehicle, File file, ArrayList<GPSPoint> log) {

		findNextVehicleStop(log); // Clean all points until first start
		ArrayList<Activity> activityList = extractActivities(log); // Find all the activities
		extractChains(thisVehicle, activityList);
		thisVehicle.updateVehicleStatistics(studyArea);
	}

	private static ArrayList<Activity> extractActivities(ArrayList<GPSPoint> log) {

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
					System.err.println("There does not seem to be a possible location for this activity?!");
				}
				log.get(0).setCoordinate(new Coordinate(x, y) );
				// Using the start position as the location of the activity
				// TODO When sorting out 'BoinkPoints' I need to address this as well.
				activityList.add(new Activity( log.get(0).getTime(), log.get(1).getTime(), log.get(0) ) );
				// Add the activity's duration to the duration bucket
				int lastActivityDuration = activityList.get(activityList.size()-1).getDuration();
				int position = Math.min(numBuckets - 1, (lastActivityDuration / bucketSize) );
				int dummy = durationBuckets.get(position);
				durationBuckets.set(position, dummy + 1);

				// Check if location where activity stops is within the limits from where activity started
				int distance = (int) ( log.get(1).getCoordinate().distance( log.get(0).getCoordinate() ) );
				if(distance > distanceThreshold){
//					System.err.println("Woops... this activity starts and ends at different positions.");
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
			if ( thisActivity.getDuration() < minorActivityMinimumDuration ){ // too short for an activity
				activityList.remove(0);
			} else if(thisActivity.getDuration() < majorActivityMinimumDuration ){ // then it is an activity)
				if( thisChain.getDayStart() == null ){
					thisChain.setDayStart(thisActivity.getStartTime() ); // set start time
				}
				thisChain.getActivities().add(thisActivity);
				activityList.remove(0);
			} else{ // else it is a major (home) location
				thisChain.getActivities().add(thisActivity); // add home location to end of current chain

				thisVehicle.getChains().add( thisChain );

				thisChain = new Chain();
				thisChain.setDayStart( thisActivity.getEndTime() );
				thisChain.getActivities().add(thisActivity); // add home location at start of new chain
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
		Scanner input;
		try {
			input = new Scanner(new BufferedReader(new FileReader(thisFile) ) );
			try{
				while( input.hasNextLine() ){
					String [] inputString = input.nextLine().split(delimiter);
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
						} catch(NumberFormatException e2){
							System.out.print("");
						} catch(Exception e3){
							// Points with coordinates outside the range (±90º) are ignored.
							System.out.print("");
						}
					}
				}
			} finally{
				input.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return log;
	}

	private static Vehicle createNewVehicle(File file) {
		int position = file.getName().indexOf(".");
		Integer vehicleID = Integer.parseInt(file.getName().substring(0, position));
		Vehicle thisVehicle = new Vehicle( vehicleID );
		return thisVehicle;
	}

	public static int getMajorActivityMinimumDuration() {
		return majorActivityMinimumDuration;
	}


}
