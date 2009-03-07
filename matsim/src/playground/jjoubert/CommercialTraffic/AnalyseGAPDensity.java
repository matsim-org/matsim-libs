package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class to 
 * @author johanwjoubert
 *
 */
public class AnalyseGAPDensity {
	// String value that must be set
	final static String PROVINCE = "Gauteng";
	// Mac
//	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
	final static String ROOT = "/home/jjoubert/";
	// Derived string values
	final static String GAP_SHAPEFILE = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "GAP_UTM35S.shp";
	final static String SHAPEFILE = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "_UTM35S.shp";
	final static String INPUT_MINOR = ROOT + PROVINCE + "/Vehicles/Activities/" + PROVINCE + "MinorLocations.txt";
	final static String INPUT_MAJOR = ROOT + PROVINCE + "/Vehicles/Activities/" + PROVINCE + "MajorLocations.txt";
	final static String OUTPUT_MINOR = ROOT + PROVINCE + "/Vehicles/Activities/" + PROVINCE + "MinorGapStats.txt";
	final static String OUTPUT_MAJOR = ROOT + PROVINCE + "/Vehicles/Activities/" + PROVINCE + "MajorGapStats.txt";

//	final static String INPUT_MINOR = ROOT + PROVINCE + "/Activities/20090225-2030/" + PROVINCE + "MinorLocations.txt";
//	final static String INPUT_MAJOR = ROOT + PROVINCE + "/Activities/20090225-2030/" + PROVINCE + "MajorLocations.txt";
//	final static String OUTPUT_MINOR = ROOT + PROVINCE + "/Activities/20090225-2030/" + PROVINCE + "MinorGapStats.txt";
//	final static String OUTPUT_MAJOR = ROOT + PROVINCE + "/Activities/20090225-2030/" + PROVINCE + "MajorGapStats.txt";

	public static final String DELIMITER = ",";
	final static int GAP_SEARCH_AREA = 20000; // in METERS
	final static int PROGRESS_INCREMENT = 100000;

	public static void main( String args[] ) {
		System.out.println("==========================================================================================");
		System.out.println("Performing detailed GAP analysis for: " + PROVINCE );
		System.out.println();
		
		ArrayList<SAZone> zoneList = readGAPShapeFile( GAP_SHAPEFILE );
	
		QuadTree<SAZone> zoneTree = buildQuadTree( zoneList );

		assignActivityToZone(zoneList, zoneTree );
		
		writeZoneStatsToFile( zoneList );
	}
	
	
	@SuppressWarnings("unchecked")
	private static ArrayList<SAZone> readGAPShapeFile ( String fileString ){
		System.out.println("Reading GAP mesozone shapefile... " );
		
		ArrayList<SAZone> zoneList = new ArrayList<SAZone>();
		FeatureSource fs = null;
		MultiPolygon mp = null;
		GeometryFactory gf = new GeometryFactory();
		try {	
			fs = ShapeFileReader.readDataFile( fileString );
			ArrayList<Object> objectArray = (ArrayList<Object>) fs.getFeatures().getAttribute(0);
			for (int i = 0; i < objectArray.size(); i++) {
				Object thisZone = objectArray.get(i);
				// For GAP files, field [1] contains the GAP_ID
				String name = String.valueOf( ((Feature) thisZone).getAttribute( 1 ) ); 
				Geometry shape = ((Feature) thisZone).getDefaultGeometry();
				if( shape instanceof MultiPolygon ){
					mp = (MultiPolygon)shape;
					if( !mp.isSimple() ){
						System.out.println( "This polygon is NOT simple." );
					}
					Polygon polygonArray[] = new Polygon[mp.getNumGeometries()];
					for(int j = 0; j < mp.getNumGeometries(); j++ ){
						if(mp.getGeometryN(j) instanceof Polygon ){
							polygonArray[j] = (Polygon) mp.getGeometryN(j);							
						} else{
							System.out.println("Subset of multipolygon is NOT a polygon.");
						}
					}
					SAZone newZone = new SAZone(polygonArray, gf, name, 0);
					zoneList.add( newZone );
				} else{
					System.out.println( " This is not a multipolygon.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		System.out.printf("Done.\n\n" );
		return zoneList;
	}

	private static QuadTree<SAZone> buildQuadTree(ArrayList<SAZone> mesozoneList) {
		System.out.println("Establishing quad tree extent for " + PROVINCE + "... " );
		Polygon envelope = (Polygon) (ReadStudyAreaShapeFile.readStudyAreaPolygon( SHAPEFILE ) ).getEnvelope();
		Coordinate [] coords = envelope.getCoordinates();
		double minX = coords[0].x;
		double minY = coords[0].y;
		double maxX = coords[2].x;
		double maxY = coords[2].y;
		System.out.println("Done." );

		System.out.printf("Building quad tree from mesozones... " );

		QuadTree<SAZone> mesozoneQT = new QuadTree<SAZone>(minX, minY, maxX, maxY);
		for (SAZone zone : mesozoneList) {
			mesozoneQT.put(zone.getCentroid().getX(), zone.getCentroid().getY(), zone);
		}
		System.out.printf("Done.\n\n");
		return mesozoneQT;		
	}
		
	private static void assignActivityToZone(ArrayList<SAZone> list, QuadTree<SAZone> tree ){
		System.out.println("Assigning activity locations to GAP mesozones.");

		GeometryFactory gf = new GeometryFactory();
		
		try { // Minor activities
			Scanner inputMinor = new Scanner(new BufferedReader(new FileReader(new File( INPUT_MINOR ) ) ) );
			@SuppressWarnings("unused")
			String header = inputMinor.nextLine();

			try {
				int numberOfPoints = 0;
				while(inputMinor.hasNextLine() ){
					// Report progress
					if( numberOfPoints%PROGRESS_INCREMENT == 0){
						System.out.printf("     ...minor activities %8d\n", numberOfPoints );
					}
					String[] thisLine = inputMinor.nextLine().split( DELIMITER );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );

						Point thisActivity = gf.createPoint(new Coordinate(x, y) );

						ArrayList<SAZone> shortlist = (ArrayList<SAZone>) tree.get(x, y, GAP_SEARCH_AREA );
						SAZone minorZone = findZoneInArrayList(thisActivity, shortlist );
						if ( minorZone != null ){
							minorZone.incrementMinorActivityCountDetail( timeOfDay );
							minorZone.increaseMinorActivityDurationDetail( timeOfDay, duration );
						} else{
							// Point is not is study area		
						}
					}
					numberOfPoints++;
				} 
			} finally {
				inputMinor.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}

		try { // Major activities
			Scanner inputMajor = new Scanner(new BufferedReader(new FileReader(new File( INPUT_MAJOR ) ) ) );
			@SuppressWarnings("unused")
			String header = inputMajor.nextLine();

			try {
				int numberOfPoints = 0;
				while(inputMajor.hasNextLine() ){
					// Report progress
					if( numberOfPoints%PROGRESS_INCREMENT == 0){
						System.out.printf("     ...major activities %8d\n", numberOfPoints );
					}
					String[] thisLine = inputMajor.nextLine().split( DELIMITER );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );

						Point thisActivity = gf.createPoint(new Coordinate(x, y) );
						
						ArrayList<SAZone> shortlist = (ArrayList<SAZone>) tree.get(x, y, GAP_SEARCH_AREA );
						SAZone majorZone = findZoneInArrayList(thisActivity, shortlist );
						if ( majorZone != null ){
							majorZone.incrementMajorActivityCountDetail( timeOfDay );
							majorZone.increaseMajorActivityDurationDetail( timeOfDay, duration );
						} else{
							// Point is not is study area		
						}
					}
					numberOfPoints++;
				} 
			} finally {
				inputMajor.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.printf("Done.\n\n" );
	}

	private static SAZone findZoneInArrayList(Point p, ArrayList<SAZone> list ) {
	SAZone zone = null;
	int i = 0;
	while( (i < list.size() ) & (zone == null) ){
		SAZone thisZone = list.get(i);
		if( thisZone.contains( p ) ){
			zone = thisZone;				
		} else{
			i++;
		}
	}
	return zone;
}
	
	private static void writeZoneStatsToFile(ArrayList<SAZone> zoneList) {
		System.out.print("Writing mesozone statistics to file... ");
		try{
			BufferedWriter outputMinor = new BufferedWriter(new FileWriter( new File ( OUTPUT_MINOR ) ) );
			BufferedWriter outputMajor = new BufferedWriter(new FileWriter( new File ( OUTPUT_MAJOR ) ) );
			
			String header = createHeaderString();
			
			// Update zone activity counts
			for (SAZone zone : zoneList) {
				zone.updateSAZoneCounts(true); // Update minor
				zone.updateSAZoneCounts(false); // Update major
			}

			// Write minor activities
			try{
				outputMinor.write( header );
				outputMinor.newLine();
				for(int j = 0; j < zoneList.size()-1; j++ ){
					outputMinor.write( createStatsString( zoneList.get(j) ).get(0) );
					outputMinor.newLine();
				}
				outputMinor.write( createStatsString( zoneList.get( zoneList.size()-1 ) ).get(0) );
			} finally{
				outputMinor.close();
			}

			// Write major activities
			try{
				outputMajor.write( header );
				outputMajor.newLine();
				for(int j = 0; j < zoneList.size()-1; j++ ){
					outputMajor.write( createStatsString( zoneList.get(j) ).get(0) );
					outputMajor.newLine();
				}
				outputMajor.write( createStatsString( zoneList.get( zoneList.size()-1 ) ).get(0) );
			} finally{
				outputMajor.close();
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}
		System.out.print("Done.\n\n");
	}
		
	/*
	 * Returns a standard header string for the GAP statistics file.
	 */
	private static String createHeaderString(){
		String headerString = "Name" + DELIMITER + 
							  "Total_Count" + DELIMITER;
		for(int i = 0; i < 24; i++){
			headerString += "H" + i + DELIMITER;
		}
		for(int i = 0; i < 23; i++){
			headerString += "D" + i + DELIMITER;
		}
		headerString += "D23";
		return headerString;
	}

	/*
	 * Method to create statistics strings. The first string in the array
	 * relates to 'minor' activities, and the second string to 'major' 
	 * activities.
	 */
	private static ArrayList<String> createStatsString(SAZone zone){
		
		ArrayList<String> statsString = new ArrayList<String>();
		
		// Minor activity string
		String statsStringMinor = zone.getName() + DELIMITER;
		statsStringMinor += zone.getMinorActivityCount() + DELIMITER;
		for(int i = 0; i < 24; i++){
			statsStringMinor += zone.getMinorActivityCountDetail(i) + DELIMITER;
		}
		for(int i = 0; i < 23; i++){
			statsStringMinor += zone.getMinorActivityDurationDetail(i) + DELIMITER;
		}
		statsStringMinor += zone.getMinorActivityDurationDetail(23);

		// Major activity string
		String statsStringMajor = zone.getName() + DELIMITER;
		statsStringMajor += zone.getMajorActivityCount() + DELIMITER;
		for(int i = 0; i < 24; i++){
			statsStringMajor += zone.getMajorActivityCountDetail(i) + DELIMITER;
		}
		for(int i = 0; i < 23; i++){
			statsStringMajor += zone.getMajorActivityDurationDetail(i) + DELIMITER;
		}
		statsStringMajor += zone.getMajorActivityDurationDetail(23);
		
		statsString.add( statsStringMinor );
		statsString.add( statsStringMajor );
		
		return statsString;
	}



}
