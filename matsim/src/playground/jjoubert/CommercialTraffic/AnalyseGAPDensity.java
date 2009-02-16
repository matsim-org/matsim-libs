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
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class AnalyseGAPDensity {
	public static final String DELIMITER = ",";

	
	public static void main( String args[] ) {
		System.out.println("Starting GAP mesozone analysis.");
		
		System.out.println("Reading GAP mesozone shapefile...");
		String gapShapefile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/ShapeFiles/Gauteng/GautengGAP.shp";
		ArrayList<GapZone> zoneList = readGapShapefile(gapShapefile);
		System.out.println("Done.");
		
		GeometryFactory gf = new GeometryFactory();
		
		System.out.print("Processing activity list... " );
		String activityFolder = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Activities";
		String activityFile = "/20090214-2230/hourOfDayInGautengStats.txt";
		String outputFile = "/20090214-2230/GapStats.txt";
		
		File inFile = new File( activityFolder + activityFile );
		File outFile = new File( activityFolder + outputFile );
		
		System.out.print("");

		try {
			Scanner input = new Scanner( new BufferedReader(new FileReader(inFile ) ) );
			@SuppressWarnings("unused")
			String header = input.nextLine();

			while( input.hasNextLine() ){
				String[] thisLine = input.nextLine().split( DELIMITER );
				if( thisLine.length > 3 ){
					double x = Double.parseDouble( thisLine[1] );
					double y = Double.parseDouble( thisLine[2] );
					int hour = Integer.parseInt( thisLine[3] );
					int duration = Integer.parseInt( thisLine[4] );
					Point p = gf.createPoint(new Coordinate(x, y) );
					boolean found = false;
					int i = 0; 
					while( (i < zoneList.size() ) && !found ) {
						if( zoneList.get(i).getGapPolygon().contains(p) ){
							zoneList.get(i).incrementActivityCount( hour );
							zoneList.get(i).increaseActivityDuration(hour, duration );
							found = true;
						}
						i++;
					}	
					System.out.print("");
				}
			}
			// Update the average duration for each hour
			for (GapZone gapZone : zoneList) {
				gapZone.update();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
		System.out.println("Total number of GAP mesozones: " + zoneList.size() );
		System.out.print("Writing GAP mesozone statistics to file... ");

		writeGapZoneStatsToFile(zoneList, outFile);
		System.out.println("Done.");	
	}

	private static void writeGapZoneStatsToFile(ArrayList<GapZone> zoneList,
			File outFile) {
		try{
			BufferedWriter output = new BufferedWriter(new FileWriter( outFile) );
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
		String headerString = "GAP_ID" + DELIMITER;
		for(int i = 0; i < 24; i++){
			headerString += "H" + i + DELIMITER;
		}
		for(int i = 0; i < 23; i++){
			headerString += "D" + i + DELIMITER;
		}
		headerString += "D23";
		return headerString;
	}
	
	private static String createStatsString(GapZone gapZone){
		String statsString = gapZone.getGapID() + DELIMITER;
		for(int i = 0; i < 24; i++){
			statsString += gapZone.getActivityCount(i) + DELIMITER;
		}
		for(int i = 0; i < 23; i++){
			statsString += gapZone.getActivityDuration(i) + DELIMITER;
		}
		statsString += gapZone.getActivityDuration(23);
		return statsString;
	}

	private static ArrayList<GapZone> readGapShapefile(String gapShapefile) {
		ArrayList<GapZone> zones = new ArrayList<GapZone>();

		FeatureSource fs = null;
		MultiPolygon mp = null;
		try {	
			fs = ShapeFileReader.readDataFile( gapShapefile );
			
			ArrayList<Object> gapArray = (ArrayList<Object>) fs.getFeatures().getAttribute(0);
			for (int i = 0; i < gapArray.size(); i++) {
				Object thisGAP = gapArray.get(i);
				int j = 0;
				j++;
				Integer gapID = (Integer) ((Feature) thisGAP).getAttribute(1);
				Geometry gapGeo = ((Feature) thisGAP).getDefaultGeometry();
				if( gapGeo instanceof MultiPolygon ){
					mp = (MultiPolygon)gapGeo;
					if( !mp.isSimple() ){
						System.out.println( "This polygon is NOT simple." );
					} 
				}
				GapZone newZone = new GapZone(gapID, mp);
				zones.add( newZone );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return zones;
	}
}
