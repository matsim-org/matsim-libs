package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

public class IncreaseNumberOfCarsAtStation {
	public void run (String[] args) throws IOException {
		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/Stations_GreaterZurich_run24_iter2_100x.txt")));
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
	    String s = reader.readLine();
	    output.write(s);
	    output.newLine();
	    s = reader.readLine();
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
	    	for (int i = 0; i < 6; i++  )
	    		output.write(arr[i] + "\t");
	    	
	    	output.write((Integer.toString(Integer.parseInt(arr[6]) * 100)));
	    	
	    	output.newLine();
			
	    	s = reader.readLine();
	    	
	    }	 
	 
	    
	    output.flush();
	    output.close();
		
	}
	public static void main(String[] args) throws IOException {
	
		IncreaseNumberOfCarsAtStation ff = new IncreaseNumberOfCarsAtStation();
		
		ff.run(args);
		
		
	}
	

}
