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

public class AnalyseGAPDensityFull {
	public static final String DELIMITER = ",";
	private static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	private static String activityFile = ROOT + "Temp/Activities/minorLocations.txt";
	private static boolean minorActivityRun = true;
	private static int maxNumberOfPoints = 1000;
	private static int numberOfPoints = 0;
	
	
	public static void main( String args[] ) {
		System.out.println( "Starting zone analysis." );
		//===============================================================================
		System.out.println( "Reading shapefiles..." );
		
		// Countries
		String countryShapefile = ROOT + "ShapeFiles/SA/SouthernAfrica_UTM35S.shp";
		final ArrayList<SAZone> countryList = readZoneShapefile( countryShapefile, 4, 0 );
					// Now manually change country order
					SAZone dummy = countryList.get(0);
					countryList.set(0, countryList.get(13) ); // South Africa first
					countryList.set(13, dummy);
					dummy = countryList.get(2);
					countryList.set(2, countryList.get(6) ); // Namibia third, after Botswana
					countryList.set(6, dummy);
		// Provinces
		String provinceShapefile = ROOT + "ShapeFiles/SA/SouthAfricaProvince_UTM35S.shp";
		final ArrayList<SAZone> provinceList = readZoneShapefile( provinceShapefile, 4, 1 );
					// Now manually put Gauteng first
					dummy = provinceList.get(0);
					provinceList.set(0, provinceList.get(2) );
					provinceList.set(2, dummy );				
		// Districts
		String districtShapefile = ROOT + "ShapeFiles/SA/SouthAfricaDistrict_UTM35S.shp";
		final ArrayList<SAZone> districtList = readZoneShapefile(districtShapefile, 3, 2);
		// Mesozone
		String mesozoneShapefile = ROOT + "ShapeFiles/SA/SouthAfricaGAP_UTM35S.shp";
		final ArrayList<SAZone> mesozoneList = readZoneShapefile(mesozoneShapefile, 2, 3);
		//===============================================================================
		
		// Building polygon-tree using array lists
		long timeStartArrayZone = System.currentTimeMillis();
		buildArrayTree(countryList, provinceList, districtList, mesozoneList);
		int durationArrayZone = (int)(System.currentTimeMillis() - timeStartArrayZone );
		
		// Read and process points with Array tree
		long timeStartArrayPoint = System.currentTimeMillis();
		int lostPoints1 = processPointsWithArrayTree(activityFile, countryList);
		int durationArrayPoint = (int)(System.currentTimeMillis() - timeStartArrayPoint);
		
		// Clear all list contents
		System.out.print("Clearing the zone lists between runs... ");
		for (SAZone zone : mesozoneList) {
			zone.clearSAZone();
		}
		for (SAZone zone : districtList) {
			zone.clearSAZone();
		}
		for (SAZone zone : provinceList) {
			zone.clearSAZone();
		}
		for (SAZone zone : countryList) {
			zone.clearSAZone();
		}
		System.out.printf("Done.\n\n");
		
		// Building polygon-tree using quad trees.
		long timeStartQuadtreeZone = System.currentTimeMillis();
		ArrayList<QuadTree<SAZone>> quadTrees = buildQuadTree(districtList, mesozoneList);
		int durationQuadtreeZone = (int)(System.currentTimeMillis() - timeStartQuadtreeZone );
		
		// Read and process points with quad trees
		long timeStartQuadtreePoint = System.currentTimeMillis();
		int lostPoints2 = processPointsWithQuadTree(activityFile, countryList, provinceList, quadTrees.get(0), quadTrees.get(1) );
		int durationQuadtreePoint = (int)(System.currentTimeMillis() - timeStartQuadtreePoint );
		System.out.printf("Done.\n\n");
		
		// Update all SAZone statistics
		System.out.print("Update zone statistics... ");
		for (SAZone zone : countryList) {
			zone.updateSAZoneCounts( minorActivityRun );
		}
		for (SAZone zone : provinceList) {
			zone.updateSAZoneCounts( minorActivityRun );
		}
		for (SAZone zone : districtList) {
			zone.updateSAZoneCounts( minorActivityRun );
		}
		for (SAZone zone : mesozoneList) {
			zone.updateSAZoneCounts( minorActivityRun );
		}	
		System.out.printf("Done.\n\n");		
		
		// Report final statistics
		System.out.println("Number of entities for which statistics are gathered:" );		
		System.out.println("Level 0 (Countries): " + countryList.size() );
		System.out.println("Level 1 (Province): " + provinceList.size() );
		System.out.println("Level 2 (District): " + districtList.size() );
		System.out.println("Level 3 (Mesozone): " + mesozoneList.size() );
		System.out.println();
		System.out.println("Time to build Array tree: " + durationArrayZone + " millisec" );
		System.out.println("Time to process " + Math.min(maxNumberOfPoints, numberOfPoints ) + " points with Array tree: " + durationArrayPoint + " millisec");
		System.out.println("Number of lost points using array tree: " + lostPoints1);
		System.out.println("Time to build quad trees: " + durationQuadtreeZone + " millisec" );
		System.out.println("Time to process " + Math.min(maxNumberOfPoints, numberOfPoints ) + " points with Array tree: " + durationQuadtreePoint + " millisec");
		System.out.println("Number of lost points using quad tree: " + lostPoints2);
		System.out.println();

		System.out.print("Writing zone statistics to file... ");
		String suffix = minorActivityRun ? "Minor": "Major";
		writeZoneStatsToFile(countryList, "Temp/Activities/countryStatsTimeOfDay" + suffix + ".txt" );
		writeZoneStatsToFile(provinceList, "Temp/Activities/provinceStatsTimeOfDay" + suffix + ".txt" );
		writeZoneStatsToFile(districtList, "Temp/Activities/districtStatsTimeOfDay" + suffix + ".txt" );
		writeZoneStatsToFile(mesozoneList, "Temp/Activities/mesozoneStatsTimeOfDay" + suffix + ".txt" );
		System.out.println("Done.");	
	}

	private static int processPointsWithQuadTree(String activityFile,
												 ArrayList<SAZone> countryList,
												 ArrayList<SAZone> provinceList, 
												 QuadTree<SAZone> districtTree,
												 QuadTree<SAZone> mesozoneTree) {

		System.out.println( "Reading and processing activity file " + activityFile + " using quad trees... " );
		GeometryFactory gf = new GeometryFactory();
		int lostPoints = 0;
		numberOfPoints = 0;
		
		SAZone SouthAfrica = countryList.get(0);

		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(activityFile) ) ) );
			@SuppressWarnings("unused")
			String header = input.nextLine();

			try {
				while(input.hasNextLine() && (numberOfPoints <= maxNumberOfPoints ) ){
					// Report progress
					if( numberOfPoints%100 == 0){
						System.out.printf("...%8d\n", numberOfPoints );
					}
					String[] thisLine = input.nextLine().split( DELIMITER );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );
						
						Point thisActivity = gf.createPoint(new Coordinate(x, y) );
						
						boolean found1 = false;
						boolean found2 = false;
						boolean found3 = false;
						boolean found4 = false;
						if( SouthAfrica.contains(thisActivity ) ){
							// Check mesozone
							found1 = findZoneInArrayList(thisActivity, (ArrayList<SAZone>) mesozoneTree.get(x, y, 10000), timeOfDay, duration );
							// Check districts
							found2 = findZoneInArrayList(thisActivity, (ArrayList<SAZone>) districtTree.get(x, y, 375000), timeOfDay, duration );						
							// Check provinces
							found3  = findZoneInArrayList(thisActivity, provinceList, timeOfDay, duration );
							
						} else{ 
							// Check Southern African country
							found4 = findZoneInArrayList(thisActivity, countryList, timeOfDay, duration);
						}	
						if( (found4) || (found1 && found2 && found3) ){
							// the point is placed
						} else{
							lostPoints++;
						}
					}
					numberOfPoints++;
				} 
			} finally {
				input.close();
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lostPoints;
	}

	private static boolean findZoneInArrayList(Point thisActivity,	ArrayList<SAZone> zoneList, int timeOfDay, int duration) {
		boolean found = false;
		int i = 0;
		while( (i < zoneList.size() ) && !found ){
			SAZone thisZone = zoneList.get(i);
			if( thisZone.contains( thisActivity ) ){
				found = true;				
				if( minorActivityRun ){
					thisZone.incrementMinorActivityCountDetail( timeOfDay );
					thisZone.increaseMinorActivityDurationDetail( timeOfDay, duration );
				} else{
					thisZone.incrementMajorActivityCountDetail( timeOfDay );
					thisZone.increaseMajorActivityDurationDetail( timeOfDay, duration );
				}
			} else{
				i++;
			}
		}
		return found;
	}

	private static ArrayList<QuadTree<SAZone>> buildQuadTree(ArrayList<SAZone> districtList,
									  						 ArrayList<SAZone> mesozoneList) {
		ArrayList<QuadTree<SAZone>> trees = new ArrayList<QuadTree<SAZone>>();
		
		System.out.println();
		System.out.println("Building quad-trees...");

		System.out.print("Districts...");
		QuadTree<SAZone> districtQT = new QuadTree<SAZone>(-560000, 6100000, 1100000, 7600000);
		for (SAZone zone : districtList) {
			districtQT.put(zone.getCentroid().getX(), zone.getCentroid().getY(), zone);
		}
		System.out.println("Done");
		
		System.out.print("Mesozones...");
		QuadTree<SAZone> mesozoneQT = new QuadTree<SAZone>(-560000, 6100000, 1100000, 7600000);
		for (SAZone zone : mesozoneList) {
			mesozoneQT.put(zone.getCentroid().getX(), zone.getCentroid().getY(), zone);
		}
		trees.add(districtQT);
		trees.add(mesozoneQT);
		System.out.println("Done");
		
		return trees;		
	}

	private static int processPointsWithArrayTree(String activityFile, ArrayList<SAZone> countryList) {

		System.out.println( "Reading and processing activity file " + activityFile + " using array lists... " );
		GeometryFactory gf = new GeometryFactory();
		int lostPoints = 0;
		numberOfPoints = 0;

		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(activityFile) ) ) );
			@SuppressWarnings("unused")
			String header = input.nextLine();

			try {
				while(input.hasNextLine() && (numberOfPoints <= maxNumberOfPoints ) ){
					// Report progress
					if( numberOfPoints%100 == 0){
						System.out.printf("...%8d\n", numberOfPoints );
					}
					String[] thisLine = input.nextLine().split( DELIMITER );
					if( thisLine.length > 3 ){
						double x = Double.parseDouble( thisLine[1] );
						double y = Double.parseDouble( thisLine[2] );
						int hourPosition = thisLine[3].indexOf("H");
						int timeOfDay = Integer.parseInt( thisLine[3].substring(hourPosition-2, hourPosition ) );
						int duration = Integer.parseInt( thisLine[4] );
						Point p = gf.createPoint(new Coordinate(x, y) );
						boolean found = drillDownPoint(countryList, p, timeOfDay, duration );
						if( !found ){
							lostPoints++;
						}
					}
					numberOfPoints++;
				} 
			} finally {
				input.close();
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lostPoints;
	}

	private static void buildArrayTree(ArrayList<SAZone> countryList,
									   ArrayList<SAZone> provinceList, 
									   ArrayList<SAZone> districtList,
									   ArrayList<SAZone> mesozoneList) {
		System.out.println();
		System.out.println("Building polygon-tree...");
		System.out.print("Provinces...");
		setParentPolygonContainers(countryList, provinceList);
		System.out.println("Done");
		System.out.print("Districts...");
		setParentPolygonContainers(countryList, districtList);
		System.out.println("Done");
		System.out.print("Mesozones...");
		setParentPolygonContainers(countryList, mesozoneList);
		System.out.println("Done");
		System.out.println();
	}

	private static SAZone drillDownZones( ArrayList<SAZone> list, int listLevel, Point point, int pointLevel){
		SAZone result = null;
		SAZone thisParent = null;
		int i = 0;
		boolean found = false;
		while( !found && (i < list.size() ) ){
			thisParent = list.get(i);
			if( thisParent.contains( point ) ){
				if( (listLevel + 1) == (pointLevel) ){ // then it is a direct parent
					found = true;
					result = thisParent;
				} else{
					ArrayList<SAZone> newList = thisParent.getPolygonContainer();
					int newListLevel = newList.get(0).getLevel();
					result = drillDownZones(newList, newListLevel, point, pointLevel);
					if( result != null ){
						found = true;
					}
				}
			} else{
				i++;
			}
		}	
		return result;
	}
	
	private static boolean drillDownPoint( ArrayList<SAZone> list, Point point, int timeOfDay, int duration ){
		SAZone thisParent = null;
		int i = 0;
		boolean found = false;
		while( !found && (i < list.size() ) ){
			thisParent = list.get(i);
			if( thisParent.contains( point ) ){
				if( thisParent.getPolygonContainer().size() == 0 ){ // then it is a direct parent
					found = true;
					// Increase activity counters
					if( minorActivityRun ){
						thisParent.incrementMinorActivityCountDetail( timeOfDay );
						thisParent.increaseMinorActivityDurationDetail( timeOfDay, duration );
					} else{
						thisParent.incrementMajorActivityCountDetail( timeOfDay );
						thisParent.increaseMajorActivityDurationDetail( timeOfDay, duration );
					}
				} else{
					ArrayList<SAZone> newList = thisParent.getPolygonContainer();
					found = drillDownPoint(newList, point, timeOfDay, duration );
				}
				return found;
			} else{
				i++;
			}
		}	
		return found;
	}

	private static void setParentPolygonContainers(
			ArrayList<SAZone> parentList, ArrayList<SAZone> childList) {
		
		int parentLevel = parentList.get(0).getLevel();
		for (SAZone child : childList) {
			Point childCentroid = child.getCentroid();
			int childLevel = child.getLevel();
			SAZone theZone = drillDownZones(parentList, parentLevel, childCentroid, childLevel);
			theZone.addToPolygonContainer(child);
		}
	}

	private static void writeZoneStatsToFile(ArrayList<SAZone> zoneList, String outFile) {
		try{
			BufferedWriter output = new BufferedWriter(new FileWriter( new File (ROOT + outFile) ) );
			try{
				output.write( createHeaderString() );
				output.newLine();
				for(int j = 0; j < zoneList.size()-1; j++ ){
					output.write( createStatsString( zoneList.get(j) ) );
					output.newLine();
				}
				output.write( createStatsString( zoneList.get( zoneList.size()-1 ) ) );
			} finally{
				output.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
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

	private static String createStatsString(SAZone zone){
		String statsString = zone.getName() + DELIMITER;
		if( minorActivityRun ){
			statsString += zone.getMinorActivityCount() + DELIMITER;
			for(int i = 0; i < 24; i++){
				statsString += zone.getMinorActivityCountDetail(i) + DELIMITER;
			}
			for(int i = 0; i < 23; i++){
				statsString += zone.getMinorActivityDurationDetail(i) + DELIMITER;
			}
			statsString += zone.getMinorActivityDurationDetail(23);
		} else{
			statsString += zone.getMajorActivityCount() + DELIMITER;
			for(int i = 0; i < 24; i++){
				statsString += zone.getMajorActivityCountDetail(i) + DELIMITER;
			}
			for(int i = 0; i < 23; i++){
				statsString += zone.getMajorActivityDurationDetail(i) + DELIMITER;
			}
			statsString += zone.getMajorActivityDurationDetail(23);
		}
		return statsString;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<SAZone> readZoneShapefile(String zoneShapefile, int nameAttributeIndex, int level) {
		ArrayList<SAZone> zones = new ArrayList<SAZone>();

		FeatureSource fs = null;
		MultiPolygon mp = null;
		GeometryFactory gf = new GeometryFactory();
		try {	
			fs = ShapeFileReader.readDataFile( zoneShapefile );
			ArrayList<Object> objectArray = (ArrayList<Object>) fs.getFeatures().getAttribute(0);
			for (int i = 0; i < objectArray.size(); i++) {
				Object thisZone = objectArray.get(i);
				int k = 0;
				k++;
				k++;
				String name = String.valueOf( ((Feature) thisZone).getAttribute( nameAttributeIndex ) );
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
					SAZone newZone = new SAZone(polygonArray, gf, name, level);
					zones.add( newZone );
				} else{
					System.out.println( " This is not a multipolygon.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return zones;
	}
}
