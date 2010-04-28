package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.jjoubert.Utilities.ProgressBar;
import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.Utilities.FileSampler.MyFileSampler;

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
//	final static String ROOT = "~/";												// IVT-Sim0
	final static String ROOT = "~/data/";											// Satawal
	
	// Derived string values
	final static String shapeFileSource = ROOT + "ShapeFiles/" + PROVINCE + "/" + PROVINCE + "_UTM35S.shp";
	final static String vehicleSource = ROOT + "VehicleFiles/";
	final static String vehicleDestination = ROOT + PROVINCE + "/Unsorted/";
	
	// Other paramaters and variables
	private final static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private final static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";

	private final static Logger log = Logger.getLogger(SelectVehicles.class);
	
	public static void main(String[] args) {
		log.info("===========================================================================================");
		log.info("Selecting vehicles from the DigiCore data set that are within: " + PROVINCE );
		log.info("===========================================================================================");
		log.info("Initializing... ");
		long startTime = System.currentTimeMillis();
		
		MyShapefileReader msr = new MyShapefileReader(shapeFileSource);
		MultiPolygon studyArea = msr.readMultiPolygon();
		
		final MathTransform mt = getMathTransform(); // Prepare for geometric transformations
		
		File originFolder = new File( vehicleSource );
		File destinationFolder = new File( vehicleDestination );
		boolean checkCreate = destinationFolder.mkdirs();
		if(!checkCreate){
			log.warn("Could not create " + destinationFolder.toString() + ", or it already exists!");
		}
		final int files = countVehicleFiles(originFolder);
		int inFiles = 0;
		int outFiles = 0;
		
		MyFileFilter vehicleFilter = new MyFileFilter(".txt");
		
		File vehicleFile[] = originFolder.listFiles(vehicleFilter);
		log.info("Done" );
		
		log.info("Processing vehicle files.");
		ProgressBar pb = new ProgressBar('*', files);
		pb.printProgressBar();

		MyFileSampler sampler = new MyFileSampler(originFolder.getAbsolutePath(), destinationFolder.getAbsolutePath());
		for (int i = 0; i < vehicleFile.length; i++ ) {
			if ( getVehicleStatus(studyArea, vehicleFile[i], mt ) ){
				sampler.copyFile(destinationFolder, vehicleFile[i] );
				inFiles++;
			} else{
				outFiles++;
			}			
			// Report on progress
			if( i%100 == 0){
				pb.updateProgress(i);
			}
		}
		long endTime = System.currentTimeMillis();
		long seconds = ((long)(endTime - startTime)/1000);
		log.info("Done");
		log.info("              Total number of files: " + (inFiles + outFiles) );
		log.info("Total number of files in study area: " + inFiles );
		
		log.info("                       Total time: " + seconds + " (sec)");
	}

	private static boolean getVehicleStatus(MultiPolygon mp, File file, MathTransform mt) {
		boolean inStatus = false;		
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(file) ) );
			GeometryFactory gf = new GeometryFactory();
			
			while((input.hasNextLine() ) && !(inStatus) ){
				String [] inputString = input.nextLine().split(" ");
				if(inputString.length > 5){
					double x = Double.valueOf(inputString[2]).doubleValue();
					double y = Double.valueOf(inputString[3]).doubleValue();
					//TODO Create a more general "check" for point validity
					if( (x > 0) && (x < 90) && (y > -90) && (y < 0) ){
						Coordinate c = new Coordinate(x, y);
							try {
								JTS.transform(c, c, mt);
							} catch (TransformException e) {
								e.printStackTrace();
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
