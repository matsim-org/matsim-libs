package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class MZReader {
	
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
				
				Coord coord = new CoordImpl(
						Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}	
				MZTrip mzTrip = new MZTrip(id, coord, startTime, endTime);
				String purpose = entries[45].trim();
				mzTrip.setPurpose(purpose);
				
				if (coord.getX() > 1000 && coord.getY() > 1000) {
					mzTrips.add(mzTrip);
				}
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return mzTrips;
	}
}
