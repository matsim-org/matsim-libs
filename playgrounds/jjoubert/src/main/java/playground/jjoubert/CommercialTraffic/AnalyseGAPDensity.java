package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.Utilities.MyGapReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to read minor and major activity files for a given study area. The study area
 * is demarcated according to the Geospatial Analysis Platform (GAP) mesozones as 
 * developed by the CSIR Built Environment. The activities are then split on an hour-
 * by-hour basis using the start time of the activity. 
 * 
 * @author jwjoubert	
 *
 */
public class AnalyseGAPDensity {
	/* 
	 * String value that must be set. Allowed study areas are:
	 * 		- SouthAfrica
	 * 		- Gauteng
	 * 		- KZN
	 * 		- WesternCape
	 */
	static String studyAreaName = "KZN";

	// Set the home directory, depending on where the job is executed.
	//	 static String ROOT = "~/MATSim/workspace/MATSimData/"; 	// Mac
	//	 static String ROOT = "~/";									// IVT-Sim0
	static String ROOT = "~/data/";									// Satawal

	// Derived string values
	static String gapShapefileName = ROOT + "Shapefiles/" + studyAreaName + "/" + studyAreaName + "GAP_UTM35S.shp";
	static String areaShapefileName = ROOT + "Shapefiles/" + studyAreaName + "/" + studyAreaName + "_UTM35S.shp";
	static String inputFilenameMinor = ROOT + studyAreaName + "/Activities/" + studyAreaName + "MinorLocations.txt";
	static String inputFilenameMajor = ROOT + studyAreaName + "/Activities/" + studyAreaName + "MajorLocations.txt";
	static String outputFilenameMinor = ROOT + studyAreaName + "/Activities/" + studyAreaName + "MinorGapStats.txt";
	static String outputFilenameMajor = ROOT + studyAreaName + "/Activities/" + studyAreaName + "MajorGapStats.txt";

	private final static Logger log = Logger.getLogger(AnalyseGAPDensity.class);
	public final static String delimiter = ",";
	static int gapSearchRadius = 20000; // in METERS
	static int progressCounter = 0;
	static int progressMultiplier = 1;

	public static void main( String args[] ) {
		log.info("==========================================================================================");
		log.info("Performing detailed GAP analysis for: " + studyAreaName );
		log.info("==========================================================================================");
		log.info("");
		
		

		MyGapReader mgr = new MyGapReader(studyAreaName, gapShapefileName);
		List<SAZone> zoneList = mgr.getAllZones();
		QuadTree<SAZone> zoneTree = mgr.getGapQuadTree();

		assignActivityToZone(zoneList, zoneTree );

		writeZoneStatsToFile( zoneList );
		
		log.info("Process completed successfully!" );
	}

			
	private static void assignActivityToZone(List<SAZone> list, QuadTree<SAZone> tree ){
		log.info("Start assigning activity locations to GAP mesozones.");

		GeometryFactory gf = new GeometryFactory();

		Scanner inputMinor;
		try {
			inputMinor = new Scanner(new BufferedReader(new FileReader(new File( inputFilenameMinor ) ) ) );
			inputMinor.nextLine();

			try {
				while(inputMinor.hasNextLine() ){
					// Report progress
					if( progressCounter == progressMultiplier){
						log.info("... Minor activities processed: " + String.valueOf(progressCounter));
						progressMultiplier *= 2;
					}
					String[] thisLine = inputMinor.nextLine().split( delimiter );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );

						Point thisActivity = gf.createPoint(new Coordinate(x, y) );

						ArrayList<SAZone> shortlist = (ArrayList<SAZone>) tree.get(x, y, gapSearchRadius );
						SAZone minorZone = findZoneInArrayList(thisActivity, shortlist );
						if ( minorZone != null ){
							minorZone.incrementMinorActivityCountDetail( timeOfDay );
							minorZone.increaseMinorActivityDurationDetail( timeOfDay, duration );
						} 
//						else{
//							 log.info(Point is not is study area);		
//						}
					}
					progressCounter++;
				} 
			} finally {
				inputMinor.close();
			}		
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		log.info("... Minor activities processed: " + progressCounter);

		// Reset the progress counters
		progressCounter = 0;
		progressMultiplier = 1;
		try { // Major activities
			Scanner inputMajor = new Scanner(new BufferedReader(new FileReader(new File( inputFilenameMajor ) ) ) );
			inputMajor.nextLine();

			try {
				while(inputMajor.hasNextLine() ){
					// Report progress
					if( progressCounter == progressMultiplier){
						log.info("... Major activities processed: " + progressCounter);
						progressMultiplier *= 2;
					}
					String[] thisLine = inputMajor.nextLine().split( delimiter );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );

						Point thisActivity = gf.createPoint(new Coordinate(x, y) );
						
						ArrayList<SAZone> shortlist = (ArrayList<SAZone>) tree.get(x, y, gapSearchRadius );
						SAZone majorZone = findZoneInArrayList(thisActivity, shortlist );
						if ( majorZone != null ){
							majorZone.incrementMajorActivityCountDetail( timeOfDay );
							majorZone.increaseMajorActivityDurationDetail( timeOfDay, duration );
						} 
//						else{
							// Point is not is study area		
//						}
					}
					progressCounter++;
				} 
			} finally {
				inputMajor.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("... Major activities processed: " + progressCounter + " (Done)");
	}

	private static SAZone findZoneInArrayList(Point p, ArrayList<SAZone> list ) {
	SAZone zone = null;
	int i = 0;
	while( (i < list.size() ) && (zone == null) ){
		SAZone thisZone = list.get(i);
		if( thisZone.contains( p ) ){
			zone = thisZone;				
		} else{
			i++;
		}
	}
	return zone;
}

	private static void writeZoneStatsToFile(List<SAZone> zoneList) {
		log.info("Start writing mesozone statistics to file.");
		try {
			BufferedWriter outputMinor = new BufferedWriter(new FileWriter( new File ( outputFilenameMinor ) ) );
			BufferedWriter outputMajor = new BufferedWriter(new FileWriter( new File ( outputFilenameMajor ) ) );

			// Update zone activity counts.
			for (SAZone zone : zoneList) {
				zone.updateSAZoneCounts(true); // Update minor
				zone.updateSAZoneCounts(false); // Update major
			}

			// Write activities.
			try{
				// Write a header to each output file.
				writeHeaderString(outputMinor);
				writeHeaderString(outputMajor);

				// Write each zone's activities.
				for (SAZone zone : zoneList) {
					writeMinorStatisticsString(outputMinor, zone);
					writeMajorStatisticsString(outputMajor, zone);
				}
			} finally{
				outputMinor.close();
				outputMajor.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Completed writing to file.");
	}

	/**
	 * Writes a standard header string for the GAP statistics file.
	 */
	private static void writeHeaderString(BufferedWriter output) throws IOException{
		output.write("Name");
		output.write(delimiter); 
		output.write("Total_Count");
		output.write(delimiter);
		for(int i = 0; i < 24; i++){
			output.write("H");
			output.write(String.valueOf(i));
			output.write(delimiter);
		}
		for(int i = 0; i < 23; i++){
			output.write("D");
			output.write(String.valueOf(i));
			output.write(delimiter);
		}
		output.write("D23");
		output.newLine();
	}

	/**
	 * Method to create a statistics string for 'minor' activities. 
	 * @throws IOException 
	 */
	private static void writeMinorStatisticsString(BufferedWriter output, SAZone zone) throws IOException{
		output.write(zone.getName());
		output.write(delimiter);
		output.write(String.valueOf(zone.getMinorActivityCount()));
		output.write(delimiter);
		for(int i = 0; i < 24; i++){
			output.write(String.valueOf(zone.getMinorActivityCountDetail(i)));
			output.write(delimiter);
		}
		for(int i = 0; i < 23; i++){
			output.write(String.valueOf(zone.getMinorActivityDurationDetail(i)));
			output.write(delimiter);
		}
		output.write(String.valueOf(zone.getMinorActivityDurationDetail(23)));	
		output.newLine();
	}
	
	/**
	 * Method to create a statistics string for 'major' activities. 
	 * @throws IOException 
	 */
	private static void writeMajorStatisticsString(BufferedWriter output, SAZone zone) throws IOException{
		output.write(zone.getName());
		output.write(delimiter);
		output.write(String.valueOf(zone.getMajorActivityCount()));
		output.write(delimiter);
		for(int i = 0; i < 24; i++){
			output.write(String.valueOf(zone.getMajorActivityCountDetail(i)));
			output.write(delimiter);
		}
		for(int i = 0; i < 23; i++){
			output.write(String.valueOf(zone.getMajorActivityDurationDetail(i)));
			output.write(delimiter);
		}
		output.write(String.valueOf(zone.getMajorActivityDurationDetail(23)));		
	}

}
