package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;


public class BZReader {
	
	public BZReader() {
	}
	
	/*
	B015211A = 408;	Verbraucherm�rkte (> 2500 m2) 
	B015211B = 409; Grosse Superm�rkte (1000-2499 m2)
	B015211C = 410;	Kleine Superm�rkte (400-999 m2)
	B015211D = 411;	Grosse Gesch�fte (100-399 m2)
	
	B015211E = 412;	Kleine Gesch�fte (< 100 m2)
	B015212A = 413; Warenh�user
	
	*
	B015221A = 415; Detailhandel mit Obst und Gem�se
	B015222A = 416; Detailhandel mit Fleisch und Fleischwaren	
	B015223A = 417; Detailhandel mit Fisch und Meeresfr�chten	
	B015224A = 418; Detailhandel mit Brot, Back- und S�sswaren	
	B015225A = 419;	Detailhandel mit Getr�nken
	*
	B015227A = 421; Detailhandel mit Milcherzeugnissen und Eiern	
	B015227B = 422; Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getr�nken und Tabak a.n.g. (in Verkaufsr�umen)

	*/
	
		
	public List<Hectare> readBZGrocery(String file) {
		
		List<Hectare> hectares = new Vector<Hectare>();
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split(";", -1);				
				String x = entries[1].trim();
				String y = entries[2].trim();
				Hectare hectare = new Hectare(new CoordImpl(x,y));
				
				for (int i = 3; i < entries.length; i++) {					
					if (i >= 408 && i <= 422 && i != 414 && i != 420 && Double.parseDouble(entries[i].trim()) > 0.0) {
						hectare.addShop(i);
					}
				}
				if (hectare.getShops().size() > 0) {
					hectares.add(hectare);
				}
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return hectares;
	}
}
