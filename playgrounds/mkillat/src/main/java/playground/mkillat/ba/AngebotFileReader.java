package playground.mkillat.ba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import playground.mkillat.tmc.Message;

public class AngebotFileReader {

	public static List <Angebot>  read(String filename){

		List <Angebot> angebote = new ArrayList <Angebot>();
		
		FileReader fr;
		BufferedReader br;
		try {
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		String line = null;
		br.readLine(); //Erste Zeile (Kopfzeile) wird übersprungen.
		while ((line = br.readLine()) != null && br.readLine()!="# datum; zeit; preis; plaetze; id") {
		String[] result = line.split(";");
		
		
		Angebot current = new Angebot(	result[0], 
										result[1],
										result[2],
										result [3],
										result [4],
										result[5]      );
		
			angebote.add(current);
		
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
		return angebote;
	}
	
}
