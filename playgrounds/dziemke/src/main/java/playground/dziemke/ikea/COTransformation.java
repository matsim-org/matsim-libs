package playground.dziemke.ikea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class COTransformation {
	
	private static String dataFile = "./input/ikea_coord.txt";


	public static void main(String[] args) {
		// TODO Auto-generated method stub

		CoordinateTransformation ct = 
	       TransformationFactory.getCoordinateTransformation("EPSG:4326","EPSG:3857");
		BufferedWriter writer = null;
		
		//create a temporary file
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File logFile = new File(timeLog);

        // This will output the full path where the file will be written to...
        try {
			System.out.println(logFile.getCanonicalPath());
	        writer = new BufferedWriter(new FileWriter(logFile));

        } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    
        
		try{
			// load data-file
		@SuppressWarnings("resource")
		BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFile));
		// skip header
		String line = bufferedReader.readLine();
		
		// set indices
		//!!!!!!!!!!!!!!!!  WARN MGC:162 Assuming that coordinates are in longitude first notation, i.e. (longitude, latitude).
		int index_id = 0;
		int index_x = 2;
		int index_y = 1;
		
		while((line=bufferedReader.readLine()) != null){
			String parts[] = line.split(";");

			Coord coordinates = new CoordImpl(parts[index_x],parts[index_y]);
			Coord coordinatesTransformed = ct.transform(coordinates);
			
		writer.write(parts[index_id]+";"+coordinatesTransformed.getX()+";"+coordinatesTransformed.getY());
		writer.newLine();
		
		System.out.println("Alt: " + coordinates + "Neu: " + coordinatesTransformed);

		}
		
	}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		 try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

}
	}
