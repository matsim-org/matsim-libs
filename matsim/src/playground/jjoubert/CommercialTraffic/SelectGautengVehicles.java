package playground.jjoubert.CommercialTraffic;
/*
 * This class searches through the separated DigiCore vehicle files, identifies which
 * vehicles travel through Gauteng, and moves the files into a separate folder as soon
 * as a point is found within the study area.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SelectGautengVehicles {
	final static String gautengShapefileSource = "/Users/johanwjoubert/MATSim/workspace/MATSimData/ShapeFiles/Gauteng/Gauteng_UTM35S.shp";


	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Polygon gauteng = readGautengPolygon();
		System.out.print("Initializing... ");
		
		File vehicleFolder = new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/VehicleFiles/");
		File gautengFolder = new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/VehicleFiles/GautengVehicles");
		gautengFolder.mkdir();
		final int files = countVehicleFiles(vehicleFolder);
		int inFiles = 0;
		int outFiles = 0;
		
		File vehicleFile[] = vehicleFolder.listFiles();
		System.out.println("Done\n" );
		
		System.out.println("Starting to process vehicle files.");
		for (int i = 0; i < vehicleFile.length; i++ ) {
			if( (vehicleFile[i].isFile()) && !(vehicleFile[i].getName().equalsIgnoreCase(".DS_Store"))){
				boolean gautengStatus = getVehicleStatus(gauteng, vehicleFile[i] );
				if (gautengStatus == true){
					moveVehicleFile(gautengFolder, vehicleFile[i] );
					inFiles++;
				} else{
					outFiles++;
				}
				
			}
			// Report on progress
			if( i%1000 == 0){
				float progress = ((float) i/(files-2)*100);
				System.out.printf("Progress: %3.0f %% \t%d of %d files checked; %d moved.\n", 
						progress, (inFiles + outFiles), (files-2), inFiles );		
			}
		}
		long endTime = System.currentTimeMillis();
		long seconds = ((long)(endTime - startTime)/1000);
		System.out.println("\nDone");
		System.out.println("           Total number of files: " + (inFiles + outFiles) );
		System.out.println("Total number of files in Gauteng: " + inFiles );
		
		System.out.printf ("\n                      Total time: %d", seconds);
	}

	private static boolean getVehicleStatus(Polygon polygon, File file) {
		boolean inStatus = false;		
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(file) ) );
			
			while((input.hasNextLine() ) & !(inStatus) ){
				String [] inputString = input.nextLine().split(" ");
				if(inputString.length > 5){
					double x = Double.valueOf(inputString[2]).doubleValue();
					double y = Double.valueOf(inputString[3]).doubleValue();
					Coordinate coord = new Coordinate(x,y);
					GeometryFactory gf = new GeometryFactory();
					Point p = gf.createPoint(coord);

					inStatus = testPolygon(polygon, p);
				}
			}		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
		return inStatus;
	}

	public static int countVehicleFiles(File folder) {
		int numberoffiles = folder.list().length;
		return numberoffiles;
	}

	public static boolean moveVehicleFile(File theFolder, File theFile) {
		String newFileName = theFolder.getAbsolutePath() + "/" + theFile.getName();
		File newFile = new File(newFileName);
		boolean moveResult = theFile.renameTo(newFile);
		return moveResult;
	}

	public static boolean testPolygon(Polygon polygon, Point point) {
		boolean inPolygon = polygon.contains(point);		
		return inPolygon;
	}
	
	public static Polygon readGautengPolygon() {
		
		FeatureSource fs = null;
		MultiPolygon mp = null;
		Polygon p = null;
		try {	
			fs = ShapeFileReader.readDataFile( gautengShapefileSource );
			for(Object o: fs.getFeatures() ){
				Geometry geo = ((Feature)o).getDefaultGeometry();
				if(geo instanceof MultiPolygon){
					mp = (MultiPolygon)geo;
				}
			}
			int numGeo = mp.getNumGeometries();
			if(numGeo > 1 ){
				System.out.println("There are multiple polygons...");
			} else{
				p = ((Polygon)mp.getGeometryN(0));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return p;
	}
	
}
