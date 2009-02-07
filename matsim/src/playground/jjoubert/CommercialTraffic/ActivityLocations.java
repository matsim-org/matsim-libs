package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class ActivityLocations {
	
	public static ArrayList<GPSPoint> homeLocations;
	public static ArrayList<GPSPoint> activityLocations;
	public final long STUDY_START = 1199145600; // 01 January 2008 00:00:00
	final static String SOURCEFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp";
	final static String DESTFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/TempActivity";
	final static String DELIMITER_IN = " "; // Could also be ',' or other;
	final static String DELIMITER_OUT = ","; // Could also be ' ';
	final static int[] statusStart = {18};
	final static int[] statusStop = {128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,145,146,147};
	final static int HOME_DURATION_THRESHOLD = 10000; // 5 hours for home (expressed in seconds)
	final static int ACTIVITY_MIN_THRESHOLD = 300; // Any 'activity' should be at least 5 minutes
	public static int progressDots;
	
		
	public static void main( String args[] ) {
		final File inFolder = new File( SOURCEFOLDER );
		final File vehicles[] = inFolder.listFiles();
		int numberOfVehicles = 0;
		int totalVehicles = vehicles.length - 1;
		
		homeLocations = new ArrayList<GPSPoint>();
		activityLocations = new ArrayList<GPSPoint>();
		
		System.out.println("Processing vehicle files in " + DESTFOLDER );
		System.out.println();
		printProgressBar();
		
		try {
			File outFolder = new File( DESTFOLDER );
			outFolder.mkdir();
			
			BufferedWriter vehicleStats = new BufferedWriter(new FileWriter(new File(DESTFOLDER + "/vehicleStats.txt")));
			vehicleStats.write(	"VehicleID" + DELIMITER_OUT + 
								"Number_of_home_locations" + DELIMITER_OUT + 
								"Number_of_chains" + DELIMITER_OUT + 
								"Average_chain_duration" + DELIMITER_OUT + 
								"Avg_activities_per_chain");
			vehicleStats.newLine();
			
			for(int i = 0; i < vehicles.length; i++ ){
				File thisFile = vehicles[i];
				if(thisFile.isFile() && !(thisFile.getName().startsWith(".")) ){ // avoid .* file names on Mac
					Vehicle thisVehicle = createNewVehicle(thisFile);
					
					ArrayList<GPSPoint> log = readFileToArray(thisFile);
					
					processVehicleActivities(thisVehicle, thisFile, log);
					
					addAllLocations(thisVehicle);
					
					vehicleStats.write(	thisVehicle.vehID + DELIMITER_OUT + 
										thisVehicle.homeLocation.size() + DELIMITER_OUT +
										thisVehicle.chains.size() + DELIMITER_OUT + 
										thisVehicle.avgChainDuration + DELIMITER_OUT + 
										thisVehicle.avgActivitesPerChain);
					vehicleStats.newLine();
					
					numberOfVehicles++;				
					updateProgress(numberOfVehicles, totalVehicles);
				}
			}
			vehicleStats.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println();
		System.out.println();
		System.out.print("Writing locations to file... " );
		writeLocationsToFile(homeLocations, "/homeLocations.txt" );
		writeLocationsToFile(activityLocations, "/activityLocations.txt" );
		System.out.println("Done" );
		System.out.println();
		
		System.out.println("-------------- SUMMARY  --------------");
		System.out.println("Number of vehicles processed: " + numberOfVehicles );
		System.out.println("Total home locations: " + homeLocations.size() );
		System.out.println("Total activity locations: " + activityLocations.size() );
		System.out.println("--------------------------------------");
		
	}


	private static void printProgressBar() {
		System.out.println("0%                      25%                      50%                      75%                    100%");
		System.out.print("|");
		for(int i = 1; i <= 4; i++ ){
			for(int j = 1; j <= 24; j++ ){
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

	private static void writeLocationsToFile(ArrayList<GPSPoint> list, String filename) {
		
		File writeFolder = new File(DESTFOLDER);
		writeFolder.mkdir();
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File (writeFolder.getPath() + filename) ) );
			output.write("LONG" + DELIMITER_OUT + "LAT");
			output.newLine();
			for (int i = 0; i < list.size() - 1; i++) {
				output.write(list.get(i).longitude + DELIMITER_OUT + list.get(i).latitude );
				output.newLine();
			}
			output.write(list.get(list.size() - 1).longitude + DELIMITER_OUT + list.get(list.size() - 1).latitude );	
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static void addAllLocations(Vehicle thisVehicle) {
		homeLocations.addAll(thisVehicle.homeLocation);
		for (Chain chain : thisVehicle.chains) {
			for (Activity activity : chain.activities) {
				activityLocations.add(activity.getLocation() );
			}
		}
	}

	private static void processVehicleActivities( Vehicle thisVehicle, File file, ArrayList<GPSPoint> log) {
		
		findNextVehicleStop(log); // Clean all points until first start		
		ArrayList<Activity> activityList = extractVehicleActivities(file, log); // Find all the activities
		
		// TODO Could be more sophisticated in identifying complete chains, starting and ending at home
		extractHomeAndChains(thisVehicle, activityList);
		
		thisVehicle.updateVehicleStatistics();		
	}

	private static ArrayList<Activity> extractVehicleActivities(File file,
			ArrayList<GPSPoint> log) {
		ArrayList<Activity> activityList = new ArrayList<Activity>();
		while(log.size() > 1){
			if( !startSignal(log.get(1).status) ){
				log.remove(1);
			} else{
				int duration = log.get(1).time - log.get(0).time;
				if( duration < ACTIVITY_MIN_THRESHOLD){
					log.remove(1);
				} else{
					// TODO I'll have to check whether the activity starts and ends at the same point.
					// TODO Will probably have to come up with a fancy comparator or something. 
					activityList.add(new Activity( log.get(0).time, duration, log.get(0) ) );
					log.remove(0);
					log.remove(0);
					findNextVehicleStop(log);
				}
			}
		}
		return activityList;
	}

	private static void extractHomeAndChains(Vehicle thisVehicle,
			ArrayList<Activity> activityList) {
		Chain thisChain = new Chain();
		while( activityList.size() > 0){
			Activity thisActivity = activityList.get(0);
			if ( thisActivity.getDuration() < ACTIVITY_MIN_THRESHOLD ){ // too short for an activity
				activityList.remove(0);				
			} else if(thisActivity.getDuration() < HOME_DURATION_THRESHOLD ){ // then it is an activity)
				if( thisChain.dayStart == 0 ){
					thisChain.dayStart = thisActivity.getStartTime(); // set start time
				}
				thisChain.activities.add( thisActivity );
				activityList.remove(0);
			} else{ // else it is a home location
				thisVehicle.homeLocation.add( thisActivity.getLocation() );
				activityList.remove(0);
				thisChain.calcDuration();
				thisVehicle.chains.add( thisChain );
				thisChain = new Chain();
			}
		}
		thisVehicle.cleanHomeLocations();
	}

	private static void findNextVehicleStop(ArrayList<GPSPoint> log) {
		boolean startFound = false;
		while( !startFound && log.size() > 0){
			if( !stopSignal(log.get(0).status) ){ // TODO Must rather work with enumeration
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

	public static ArrayList<GPSPoint> readFileToArray(File thisFile) {
		ArrayList<GPSPoint> log = new ArrayList<GPSPoint>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(thisFile) ) );
			while( input.hasNextLine() ){
				String [] inputString = input.nextLine().split(DELIMITER_IN);
				if( inputString.length > 0){
					GPSPoint point = new GPSPoint();
					point.setVehID( Integer.parseInt( inputString[0] ) );
					point.setTime( Integer.parseInt( inputString[1] ) );
					point.setLongitude( Double.parseDouble( inputString[2] ) );
					point.setLatitude( Double.parseDouble( inputString[3] ) );
					point.setStatus( Integer.parseInt( inputString[4] ) );
					point.setSpeed( Integer.parseInt( inputString[5] ) );
					log.add(point);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return log;
	}

	private static Vehicle createNewVehicle(File file) {
		Integer vehicleID = new Integer( (String) file.getName().subSequence(0, (file.getName().length() - 4) ) );
		Vehicle thisVehicle = new Vehicle( vehicleID );
		return thisVehicle;		
	}
	
	
}
