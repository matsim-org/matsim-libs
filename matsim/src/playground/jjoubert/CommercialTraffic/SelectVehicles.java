package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.ProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
/**
 * This class searches through the separated DigiCore vehicle files, identifies which
 * vehicles travel through a given study area, and moves the files into a separate 
 * folder as soon as a point is found within the study area.
 * 
 * @author johanwjoubert
 */

public class SelectVehicles {
	/* 
	 * String value that must be set. This can be one of the following values:
	 * 		- Gauteng
	 * 		- KZN
	 * 		- WesternCape
	 * 		- SouthAfrica
	 */
	final static String PROVINCE = "KZN";
	/*
	 * Set the home directory, depending on where the job is executed.
	 */
//	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; // Mac
//	final static String ROOT = "/home/jjoubert/";									// IVT-Sim0
	final static String ROOT = "/home/jjoubert/data/";								// Satawal
	
	// Derived string values
	final static String shapeFileSource = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "_UTM35S.shp";
	final static String vehicleSource = ROOT + "VehicleFiles/";
	final static String vehicleDestination = ROOT + PROVINCE + "/Unsorted/";
	
	// Other paramaters and variables
	private final static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private final static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";

	public static void main(String[] args) {
		System.out.println("===========================================================================================");
		System.out.println("Selecting vehicles from the DigiCore data set that are within: " + PROVINCE );
		System.out.println();
		System.out.println("Initializing... ");
		long startTime = System.currentTimeMillis();
		
		MyShapefileReader msr = new MyShapefileReader(shapeFileSource);
		MultiPolygon studyArea = msr.readMultiPolygon();
		
		final MathTransform mt = getMathTransform(); // Prepare for geometric transformations
		
		File originFolder = new File( vehicleSource );
		File destinationFolder = new File( vehicleDestination );
		destinationFolder.mkdirs();
		final int files = countVehicleFiles(originFolder);
		int inFiles = 0;
		int outFiles = 0;
		
		File vehicleFile[] = originFolder.listFiles();
		System.out.println("Done\n" );
		
		System.out.println("Processing vehicle files.");
		ProgressBar pb = new ProgressBar('*', files);
		pb.printProgressBar();
		
		for (int i = 0; i < vehicleFile.length; i++ ) {
			if( (vehicleFile[i].isFile()) && !(vehicleFile[i].getName().startsWith(".") ) ){
				if ( getVehicleStatus(studyArea, vehicleFile[i], mt ) ){
					copyVehicleFile(destinationFolder, vehicleFile[i] );
					inFiles++;
				} else{
					outFiles++;
				}			
			}
			// Report on progress
			if( i%100 == 0){
				pb.updateProgress(i);
			}
		}
		long endTime = System.currentTimeMillis();
		long seconds = ((long)(endTime - startTime)/1000);
		System.out.printf("\nDone\n\n");
		System.out.println("              Total number of files: " + (inFiles + outFiles) );
		System.out.println("Total number of files in study area: " + inFiles );
		
		System.out.printf ("\n                       Total time: %d (sec)", seconds);
	}

	private static boolean getVehicleStatus(MultiPolygon mp, File file, MathTransform mt) {
		boolean inStatus = false;		
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(file) ) );
			GeometryFactory gf = new GeometryFactory();
			
			while((input.hasNextLine() ) & !(inStatus) ){
				String [] inputString = input.nextLine().split(" ");
				if(inputString.length > 5){
					double x = Double.valueOf(inputString[2]).doubleValue();
					double y = Double.valueOf(inputString[3]).doubleValue();
					//TODO Create a more general "check" for point validity
					if( (x > 0) && (x < 90) && (y > -90) && (y < 0) ){
						Coordinate c = new Coordinate(x, y);
						try {
							JTS.transform(c, c, mt);
						} catch (Exception e) {
							// Points with coordinates outside the range (±90¼) are ignored, but is not removed.
//							e.printStackTrace();
							;
						}
						Point p = gf.createPoint(c);

						inStatus = testPolygon(mp, p);
					}
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

	public static boolean copyVehicleFile(File destinationFolder, File fromFile) {
		boolean result = false;
		String toFileName = destinationFolder.getAbsolutePath() + "/" + fromFile.getName();
		File toFile = new File(toFileName);

		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} catch(Exception e1){
			e1.printStackTrace();
		} finally {
			if (from != null){
				try {
					from.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				if (to != null){
					try {
						to.close();
						result = true;
					} catch (IOException e3) {
						e3.printStackTrace();
					}
				}
			}
		}
		return result;
	}

	public static boolean testPolygon(MultiPolygon mp, Point point) {
		boolean inPolygon = mp.contains(point);		
		return inPolygon;
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

	
}
