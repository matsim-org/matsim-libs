package playground.jjoubert.CommercialTraffic.ActivityAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.jjoubert.CommercialTraffic.SAZone;
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
 */
public class GapDensityAnalyser {
	
	private final Logger log;
	private String delimiter;
	private int gapSearchRadius;
	private int progressCounter;
	private int progressMultiplier;
	
	private String studyAreaName;
	private String root;
	private ArrayList<SAZone> zoneList;
	private QuadTree<SAZone> zoneTree;
	
	private String inputFilenameMinor;
	private String inputFilenameMajor;
	private String outputFilenameMinor;
	private String outputFilenameMajor;
	
	
	public GapDensityAnalyser(String studyAreaName, String version, String threshold, String sample, String root){
		// Basic assignments
		log = Logger.getLogger(GapDensityAnalyser.class);
		delimiter = ",";
		gapSearchRadius = 20000; // Meters
		
		this.studyAreaName = studyAreaName;
		this.root = root;
		
		// Read the study area files.
		MyActivityAnalysisStringBuilder sb = new MyActivityAnalysisStringBuilder(this.root, version, threshold, sample, studyAreaName);
//		MyStringBuilder msb = new MyStringBuilder(this.root);
		String gapShapefileName = sb.getGapShapefilename();
		MyGapReader mgr = new MyGapReader(this.studyAreaName, gapShapefileName);
		this.zoneList = mgr.getAllZones();
		this.zoneTree = mgr.getGapQuadTree();
		
		inputFilenameMinor = sb.getGapInputMinor();
		inputFilenameMajor = sb.getGapInputMajor();
		outputFilenameMinor = sb.getGapOutputMinor();
		outputFilenameMajor = sb.getGapOutputMajor();
		
	}

	public void analyseGapDensity() {
		log.info("==========================================================================================");
		log.info("Performing detailed GAP analysis for: " + studyAreaName );
		log.info("==========================================================================================");

		assignActivityToZone(zoneList, zoneTree );
		writeZoneStatsToFile( zoneList );
		
		log.info("Process completed successfully!" );
	}

			
	private void assignActivityToZone(ArrayList<SAZone> list, QuadTree<SAZone> tree ){
		log.info("Start assigning activity locations to GAP mesozones.");

		progressCounter = 0;
		progressMultiplier = 1;
		
		GeometryFactory gf = new GeometryFactory();

		Scanner inputMinor;
		try {
			inputMinor = new Scanner(new BufferedReader(new FileReader(new File( inputFilenameMinor ) ) ) );
			inputMinor.nextLine();

			try {
				while(inputMinor.hasNextLine() ){
					String[] thisLine = inputMinor.nextLine().split( delimiter );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int timeOfDay = Integer.parseInt( thisLine[3] );
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
						// Report progress
						if( ++progressCounter == progressMultiplier){
							log.info("... Minor activities processed: " + progressCounter);
							progressMultiplier *= 2;
						}
					}
				} 
			} finally {
				inputMinor.close();
			}		
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		log.info("... Minor activities processed: " + progressCounter + " (Done)");

		// Reset the progress counters
		progressCounter = 0;
		progressMultiplier = 1;
		try { // Major activities
			Scanner inputMajor = new Scanner(new BufferedReader(new FileReader(new File( inputFilenameMajor ) ) ) );
			inputMajor.nextLine();

			try {
				while(inputMajor.hasNextLine() ){
					String[] thisLine = inputMajor.nextLine().split( delimiter );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int timeOfDay = Integer.parseInt( thisLine[3] );
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
						// Report progress
						if( ++progressCounter == progressMultiplier){
							log.info("... Major activities processed: " + progressCounter);
							progressMultiplier *= 2;
						}
					}
				} 
			} finally {
				inputMajor.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("... Major activities processed: " + progressCounter + " (Done)");
	}

	private SAZone findZoneInArrayList(Point p, ArrayList<SAZone> list ) {
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

	private void writeZoneStatsToFile(ArrayList<SAZone> zoneList) {
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
	private void writeHeaderString(BufferedWriter output) throws IOException{
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
	private void writeMinorStatisticsString(BufferedWriter output, SAZone zone) throws IOException{
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
	private void writeMajorStatisticsString(BufferedWriter output, SAZone zone) throws IOException{
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
		output.newLine();
	}

}
