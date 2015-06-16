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

public class CreateIKEANodesAndLinks {private static String dataFile = "./input/zufahrtsstrassen_nodes.txt";


public static void main(String[] args) {
	// TODO Auto-generated method stub
// Pseudo-Mercator to UTM_N31
	CoordinateTransformation ct = 
       TransformationFactory.getCoordinateTransformation("EPSG:3857","EPSG:32631");
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
	BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFile));
	// skip header
	String line = bufferedReader.readLine();
	
	// set indices
	int index_id = 0;
	int index_x = 4;
	int index_y = 5;
	
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");

		Coord coordinates = new CoordImpl(parts[index_x],parts[index_y]);
		Coord coordinatesTransformed = ct.transform(coordinates);
		
	writer.write("<node id=\"IKEA_node_" + parts[index_id] + "\" x=\"" +coordinatesTransformed.getX() + "\" y=\"" + coordinatesTransformed.getY() + "\" />");
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
