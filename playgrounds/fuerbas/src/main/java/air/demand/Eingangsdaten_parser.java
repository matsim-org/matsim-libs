package air.demand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author treczka
 */		
public class Eingangsdaten_parser {

	private String Eingangsdaten = "./input/Eingangsdaten_September2010.txt";
			private FileReader fr;
			private BufferedReader br;
			private List<String> gelesenedaten = new ArrayList<String>();
			
			List<String> EingangAirport() throws IOException {
			
				try {
					fr = new FileReader(new File (Eingangsdaten));
					br = new BufferedReader(fr);
					String line = null;
					while ((line = br.readLine()) != null) {
						// Einlesen der Eingangsdaten und Runterbrechen auf Tagesebene der Verkehrszahlen
						String[] result = line.split(";");	// nimmt die Zeile an jedem ; auseinander
						gelesenedaten.add(result[0]);		// result[0] => Startflughafen; in Arraylist schreiben
						gelesenedaten.add(result[1]);		// result[1] => Zielflughafen; in Arraylist schreiben
						int Tageswert = (Integer.parseInt(result[2])/30);	// result[2] => monatl. Passagierzahl; geteilt durch 30 fuer Tageswert (Sep 2010 => 30Tage)
						String TageswertString = String.valueOf(Tageswert);	// Integer to String Conversion
						gelesenedaten.add(TageswertString);		// Tageswert in in Arraylist schreiben	
					}
				}
				catch (FileNotFoundException e) {
				System.err.println("File not Found...");
				e.printStackTrace();
				}
//				System.out.println(gelesenedaten);     //Visuelle Ausgabe der Daten in einer Zeile zur Prüfung
				return gelesenedaten;
				
			
			}
}
