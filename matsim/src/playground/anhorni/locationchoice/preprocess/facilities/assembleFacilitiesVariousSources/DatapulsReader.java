package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class DatapulsReader {
	
	private final static Logger log = Logger.getLogger(DatapulsReader.class);
	
	String [] types2add = {"Carrefour", "Manor", "Jelmoli Z�rich", "Globus", "Confiserie",
			"Jumbo Markt AG", "Discounthaus", "Maxi-Supermarkt" };
	
	
	public DatapulsReader() {
	}
	
	/*
	 * 	Datapuls:
	 * 	0				1				2		3	4		5		6	7	8	9		10		11
		Hauptkategorie	Unterkategorie	Nummer	ID	NAME	STRASSE	HNR	PLZ	ORT	X_CH	Y_CH	Beschreibung	
	*/
	
		
	public List<ZHFacilityComposed> readDatapuls(String file) {
		
		
		
		List<ZHFacilityComposed> zhfacilities = new Vector<ZHFacilityComposed>();
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);
				
				if (!entries[0].trim().equals("6")) {
					continue;
				}
				
				String retailerCategory = entries[1].trim();
				String id = entries[3].trim();
				String name = entries[4].trim();
				String streetItem = entries[5].trim();
				
				String street = "-1";
				if (streetItem.length() > 1) {
					street = streetItem.toUpperCase();
				}
				String HNR = entries[6].trim();
				String PLZ = entries[7].trim();
				String city = entries[8].trim();
				
				String xs = entries[9].trim();
				String ys = entries[10].trim();
				
				if (xs.length() == 0 || ys.length() == 0) {
					log.info("No coordinates for " + entries[4]);
					xs = "-1";
					ys = "-1";
				}
				double x = Double.parseDouble(xs);
				double y = Double.parseDouble(ys);
				String desc = entries[11].trim();
				
				ZHFacilityComposed zhfacility = new ZHFacilityComposed(id, retailerCategory, name, street, HNR, PLZ, city, x, y, "-1", desc);
				if (retailerCategory.equals("8") || retailerCategory.equals("9")) {
					boolean set = false;
					for (int i = 0; i < types2add.length; i++) {
						if (name.contains(types2add[i])) {
							set = true;
							continue;
						}
					}	
					if (set) {
						zhfacilities.add(zhfacility);
					}
				}
				else {
					zhfacilities.add(zhfacility);
				}
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return zhfacilities;
	}
}
