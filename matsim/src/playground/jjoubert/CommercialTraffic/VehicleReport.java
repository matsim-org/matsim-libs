package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jfree.util.Log;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.vividsolutions.jts.geom.Coordinate;

public class VehicleReport {
	static String VEHICLE = "100869";
	static String PROVINCE = "Gauteng";
	
	static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	static String RAW_SOURCE = ROOT + PROVINCE + "/Sorted/" + VEHICLE + ".txt";
	static String XML_SOURCE = ROOT + PROVINCE + "/XML/" + VEHICLE + ".xml";
	
	static String OUPUT_FOLDER = ROOT + "Temp/" + VEHICLE + "/";

	private final static String WGS84 = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],PRIMEM[\"Greenwich\", 0.0],UNIT[\"degree\", 0.017453292519943295],AXIS[\"Lon\", EAST],AXIS[\"Lat\", NORTH]]";
	private final static String WGS84_UTM35S = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("============================================================");
		System.out.println("Preparing visualizable output for vehicle: " + VEHICLE );
		System.out.println("============================================================");
		System.out.println();

		// Prepare for geometric transformation
		MathTransform mt = null;
		try{
			final CoordinateReferenceSystem sourceCRS = CRS.parseWKT( WGS84 );
			final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM35S );
			mt = CRS.findMathTransform(sourceCRS, targetCRS, true);
		} catch(Exception e){
			e.printStackTrace();
		}

		// Ensure the output directory exist 
		File outputFolder = new File( OUPUT_FOLDER );
		boolean checkDirectory = outputFolder.mkdirs();
		if(!checkDirectory){
			Log.warn("Could not make " + outputFolder.toString() + ", or it already exists!");
		}

		// Convert the vehicle file to the essential, and WGS84_UMT35S, format
		convertRawInput(mt);
		
		// Convert the vehicle's XML file into activity chains
		convertXMLInput();
	}

	private static void convertXMLInput() {
		System.out.print("Preparing XML file to write chains to string... " );
		Vehicle vehicle = convertVehicleFromXML( readVehicleStringFromFile(Integer.parseInt( VEHICLE ) ) );
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File( OUPUT_FOLDER + VEHICLE + "_chains.txt") ) );
			try{
				output.write("Long,Lat,Major");
				output.newLine();
				
				for (Chain chain : vehicle.getChains() ) {
					for (int i = 0; i < chain.getActivities().size(); i++ ) {
						int type = 2;
						double x = chain.getActivities().get(i).getLocation().getCoordinate().x;
						double y = chain.getActivities().get(i).getLocation().getCoordinate().y;
						if( (i==0) || (i == chain.getActivities().size()-1 ) ){
							type = 0; // Major location
						} else{
							type = i; // Minor location
						}
						output.write(x + "," + y + "," + type );
						output.newLine();
					}
				}
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Done.\n\n" );
	}

	private static void convertRawInput(MathTransform mt) {
		System.out.print("Converting and reducing raw input... " );
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( RAW_SOURCE ) ) ) );
			BufferedWriter output = new BufferedWriter(new FileWriter(new File( OUPUT_FOLDER + VEHICLE + ".txt" ) ) );
			
			try{
				output.write( "Long,Lat" );
				output.newLine();

				while( input.hasNextLine() ){
					String [] nextLine = input.nextLine().split( " " );
					if( nextLine.length > 1){
						double x = Double.parseDouble( nextLine[2] );
						double y = Double.parseDouble( nextLine[3] );
						Coordinate c = new Coordinate( x, y );
						try {
							JTS.transform(c, c, mt);
							output.write( c.x + "," + c.y );
							output.newLine();
						} catch (TransformException e) {
							;
						}			
					}
				}
			} finally{
				output.close();
			}
			
		} catch (FileNotFoundException e) { // For Scanner
			e.printStackTrace();
		} catch (IOException e) { // For BufferedWriter
			e.printStackTrace();
		}
		System.out.printf("Done.\n\n");
	}
	
	private static String readVehicleStringFromFile(int vehID){
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		
		try{
			br = new BufferedReader( new FileReader( XML_SOURCE ) );
			try{
				String s;
				while( (s = br.readLine() ) != null ){
					sb.append(s);
					sb.append("\n");
				}
			} finally {
				br.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return sb.toString();		
	}
	
	private static Vehicle convertVehicleFromXML (String XMLString){
		Vehicle vehicle = null;
		XStream xstream = new XStream(new DomDriver());
		Object obj = xstream.fromXML(XMLString);
		if(obj instanceof Vehicle){
			vehicle = (Vehicle) obj;
		}
		return vehicle;
	}



}
