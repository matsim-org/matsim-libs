package playground.anhorni.locationchoice.facilities;

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
	B015211A = 408;
	B015211B = 409;
	B015211C = 410;	
	B015211D = 411;	
	B015211E = 412;	
	B015212A = 413;
	
	B015221A = 415;
	B015222A = 416;	
	B015223A = 417;	
	B015224A = 418;	
	B015225A = 419;	
	
	B015227A = 421;
	B015227B = 422;
	*/
	
		
	public List<Hectare> readBZ(String file) {
		
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
