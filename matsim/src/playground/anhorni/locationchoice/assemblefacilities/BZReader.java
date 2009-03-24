package playground.anhorni.locationchoice.assemblefacilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordImpl;


public class BZReader {
	
	public BZReader() {
	}
	
	/*
	B015211A = 408;	Verbrauchermärkte (> 2500 m2) 
	B015211B = 409; Grosse Supermärkte (1000-2499 m2)
	B015211C = 410;	Kleine Supermärkte (400-999 m2)
	B015211D = 411;	Grosse Geschäfte (100-399 m2)
	
	B015211E = 412;	Kleine Geschäfte (< 100 m2)
	B015212A = 413; Warenhäuser
	
	*
	B015221A = 415; Detailhandel mit Obst und Gemüse
	B015222A = 416; Detailhandel mit Fleisch und Fleischwaren	
	B015223A = 417; Detailhandel mit Fisch und Meeresfrüchten	
	B015224A = 418; Detailhandel mit Brot, Back- und Süsswaren	
	B015225A = 419;	Detailhandel mit Getränken
	*
	B015227A = 421; Detailhandel mit Milcherzeugnissen und Eiern	
	B015227B = 422; Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getränken und Tabak a.n.g. (in Verkaufsräumen)

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
