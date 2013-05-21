package playground.dziemke.potsdam.population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommuterReader {

	public static List <Commuter>  read(String filename){

		List <Commuter> commuter = new ArrayList <Commuter>();
		
		FileReader fr;
		BufferedReader br = null;
		try {
			fr = new FileReader(new File (filename));
			br = new BufferedReader(fr);
			String line = null;
			br.readLine(); //Erste Zeile (Kopfzeile) wird uebersprungen.
			while ((line = br.readLine()) != null) {
				String[] result = line.split(";");

				Commuter current = new Commuter(result[0],
												Integer.parseInt(result[1]),
												Integer.parseInt(result[2]),
												Integer.parseInt(result[3]),
												Integer.parseInt(result[4]),
												Integer.parseInt(result[5]));
				commuter.add(current);
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
		} finally {
			try {
                br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
		
		}
		return commuter;
		
	}
			
}
