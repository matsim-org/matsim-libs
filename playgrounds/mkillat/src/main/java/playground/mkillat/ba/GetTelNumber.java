package playground.mkillat.ba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetTelNumber {
	
	public static String  read(String filename){

		
		List <String> bla = new ArrayList<String>();
		String nummer = null;
		
		FileReader fr;
		BufferedReader br;
		try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
		if( line.startsWith("					<span>Name</span>")){
			String[] result = line.split("<a href");
//			for (int i = 0; i < result.length; i++) {
//				bla.add(result[i]);
//			}
			
			String temp1 = result[1];
			String[] result2 = temp1.split(" ");
			String temp2 = result2[0];
			String[] result3 = temp2.split("view/");
			nummer = result3[1];
			nummer = nummer.substring(0, nummer.length()-1);
//			nummer = nummer.replace("<br />", "");
//			nummer = nummer.substring(0, nummer.length()-2);
//			Datenschutz ist gesichert, letzten zwei Ziffern der Telefonnummer werden nicht übernommen jippy
			
			
			File file = new File("C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_temp.html");

	        
	        if(file.exists()){
	            file.delete();
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
		return nummer;
	}
	
}
