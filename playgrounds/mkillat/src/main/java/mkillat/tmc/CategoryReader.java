package mkillat.tmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CategoryReader {

	public static List <EventCodeCategorys> read(String filename){

		List <EventCodeCategorys> output = new ArrayList <EventCodeCategorys>();
		
		FileReader fr;
		BufferedReader br;
		try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		String line = null;
		br.readLine(); //Erste Zeile (Kopfzeile) wird übersprungen.
		while ((line = br.readLine()) != null) {
		String[] result = line.split(";");
		
		EventCodeCategorys current = new EventCodeCategorys(Integer.parseInt(result[0]),
															result[1], 
															result[2], 
															result[3], 
															Double.parseDouble(result[4]));
		
			output.add(current);
		
		}
		

		
		
	} catch (FileNotFoundException e) {
		System.err.println("File not found...");
			e.printStackTrace();
	} catch (NumberFormatException e) {
		System.err.println("Wrong No. format...");
		e.printStackTrace();
	} catch (IOException e) {
		System.err.println("I/O error...");
		e.printStackTrace();
		}
		return output;
	}
	
}
