package playground.mkillat.ba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class ReadTheStringFromHTML {

	public static List <String>  read(String filename){

		
		List <String> bla = new ArrayList<String>();
		
		FileReader fr;
		BufferedReader br;
		try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
		if( line.startsWith("    </tr><tr class=\"odd link_hover\"><td class=\"column-1\">")){
			String[] result = line.split("<td");
			for (int i = 0; i < result.length; i++) {
				bla.add(result[i]);
			}
		}
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
		return bla;
	}
	
}
