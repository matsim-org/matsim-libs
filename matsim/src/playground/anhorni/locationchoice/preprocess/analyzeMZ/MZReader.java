package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class MZReader {
	
	private TreeMap<Id, PersonTrips> personTrips = new TreeMap<Id, PersonTrips>();
	
	public List<MZTrip> read(String file) {
		
		List<MZTrip> mzTrips = new Vector<MZTrip>();
				
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				String tripNr = entries[3].trim();
				
				Id id = new IdImpl(HHNR + ZIELPNR + tripNr);
				Id personId = new IdImpl(HHNR+ZIELPNR);
				
				CoordImpl coordEnd = new CoordImpl(
						Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				CoordImpl coordStart = new CoordImpl(
						Double.parseDouble(entries[18].trim()), Double.parseDouble(entries[19].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}	
				MZTrip mzTrip = new MZTrip(id, coordStart, coordEnd, startTime, endTime);
				
				mzTrip.setWmittel(entries[53].trim());
			
				if (!entries[45].trim().equals("-99")) {
					mzTrip.setPurposeCode(entries[45].trim());
					mzTrip.setShopOrLeisure("shop");
				}
				else if (!entries[44].trim().equals("-99")) {
					mzTrip.setPurposeCode(entries[44].trim());
					mzTrip.setShopOrLeisure("leisure");
				}
				else {
					mzTrip.setPurposeCode("-99");
					mzTrip.setShopOrLeisure("null");	
				}
				
				if (!personTrips.containsKey(personId)) {					
					personTrips.put(personId, new PersonTrips(personId, new ArrayList<MZTrip>()));
				}
				personTrips.get(personId).addMZTrip(mzTrip);
				mzTrips.add(mzTrip);	
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return mzTrips;
	}

	public TreeMap<Id, PersonTrips> getPersonTrips() {
		return personTrips;
	}
}
