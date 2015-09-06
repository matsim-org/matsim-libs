package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class ChangeStationsLocations {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		
		
		
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
	    String line;
	    BufferedReader reader1 = IOUtils.getBufferedReader(args[1]);
	    String line1 = reader1.readLine();
	    
	   
	    
		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/CarSharing/CS_Stations_relocated.txt")));
	    output.write(line1);
	    output.newLine();
		HashMap<String, Coord> mapa = new HashMap<String, Coord>();

		while ((line = reader.readLine()) != null) {
		      String[] parts = StringUtils.explode(line, '\t');
			Coord coord = new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
		      mapa.put(parts[0], coord);	    	
	    	
	    }
		
		while ((line1 = reader1.readLine()) != null) {
		      String[] arr1 = StringUtils.explode(line1, '\t');
		      arr1[2] = Double.toString((mapa.get(arr1[0])).getX());
		      arr1[3] = Double.toString((mapa.get(arr1[0])).getY());

		      output.write(arr1[0] + "\t" + arr1[1] + "\t" +arr1[2] + "\t" +arr1[3] + "\t" +arr1[4] + "\t" +arr1[5] + "\t" + arr1[6]);
				
		      output.newLine();
		      
		      
	    	
	    }

		output.flush();
		output.close();
	}

}
