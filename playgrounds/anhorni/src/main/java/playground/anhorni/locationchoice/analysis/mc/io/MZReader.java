package playground.anhorni.locationchoice.analysis.mc.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.analysis.mc.MZTrip;

public class MZReader {
	private List<MZTrip> mzTrips = new Vector<MZTrip>();

	public MZReader(List<MZTrip> mzTrips) {
		this.mzTrips = mzTrips;
	}
	
	public List<MZTrip> read(String file) {
						
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);							
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				//String tripNr = entries[3].trim();
				//Id id = new IdImpl(HHNR + ZIELPNR + tripNr);
				Id personId = new IdImpl(HHNR+ZIELPNR);
				
				// filter inplausible persons
				// 6177302: unbezahlte Arbeit
				//if (personId.compareTo(new IdImpl("6177302")) == 0) continue; 
				
				CoordImpl coordEnd = new CoordImpl(
						Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				CoordImpl coordStart = new CoordImpl(
						Double.parseDouble(entries[18].trim()), Double.parseDouble(entries[19].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}	
				MZTrip mzTrip = new MZTrip(personId, coordStart, coordEnd, startTime, endTime);
				
				CoordImpl coordHome = new CoordImpl(
						Double.parseDouble(entries[6].trim()), Double.parseDouble(entries[7].trim()));
				
				mzTrip.setHome(coordHome);

				mzTrip.setWmittel(entries[52].trim());
				mzTrip.setWzweck2(entries[54].trim());
			
				if (entries[55].trim().equals("4")) {
					mzTrip.setPurposeCode(entries[45].trim());
					mzTrip.setPurpose("shop");
				}
				else if (entries[55].trim().equals("8")) {
					mzTrip.setPurposeCode(entries[44].trim());
					mzTrip.setPurpose("leisure");
				}
				else if (entries[55].trim().equals("2")) {
					mzTrip.setPurpose("work");
					if (mzTrip.getWzweck2().equals("1")) {
						mzTrip.setPurposeCode("1");
					}
					else {
						mzTrip.setPurposeCode("-99");
					}
				}
				// education
				else if (entries[55].trim().equals("3")) {
					mzTrip.setPurpose("education");
					if (mzTrip.getWzweck2().equals("1")) {
						mzTrip.setPurposeCode("1");
					}
					else {
						mzTrip.setPurposeCode("-99");
					}
				}
				else {
					mzTrip.setPurposeCode(entries[55].trim());
					mzTrip.setPurpose("null");	
				}
				this.mzTrips.add(mzTrip);	
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return this.mzTrips;
	}
	
	public List<MZTrip> getMzTrips() {
		return mzTrips;
	}
	public void setMzTrips(List<MZTrip> mzTrips) {
		this.mzTrips = mzTrips;
	}
}
