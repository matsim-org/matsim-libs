package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.population.Act;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.MZTrip;
import playground.anhorni.locationchoice.cs.helper.Trip;

/*	  0			1		2			3			4			5		6		7			8				9
 * ---------------------------------------------------------------------------------------------------------		
 *0	| ...
 * ---------------------------------------------------------------------------------------------------------
 */


public class NelsonTripReader {
	
	private List<ChoiceSet> choiceSets;
	private final static Logger log = Logger.getLogger(NelsonTripReader.class);
	private TreeMap<Id, MZTrip> mzTrips = null; 
		
	public NelsonTripReader() {
	}
	
	public List<ChoiceSet> readFiles(final String file0, final String file1, String mode)  {
		this.mzTrips = new TreeMap<Id, MZTrip>();
		this.choiceSets = new Vector<ChoiceSet>();
		
		read0(file0);
		read1(file1, mode);
		log.info("Number of " + mode + " trips : " + this.choiceSets.size());
		return this.choiceSets;
	}
				
	// read 810Trips
	private void read1(final String file, String mode) {
		
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {	
				String[] entries = curr_line.split("\t", -1);
				
				String m = entries[70].trim();
				
				if (m.endsWith("Fuss") && !mode.equals("walk")) continue;
				if (m.endsWith("Auto") && !mode.equals("car")) continue;
				
				String recordID = entries[0].trim();
				// TODO:
				// Passed last line. Why not captured in null test?
				if (recordID.length()==0) break;
				
				String trip3ID = entries[0].trim();	
				String HHNR = entries[3].trim();
				String ZIELPNR = entries[6].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				int tripNr = Integer.parseInt(entries[8].trim());
				
				// get the after shopping trip:	
				String key = HHNR + ZIELPNR + Integer.toString(tripNr+1);
				MZTrip mzTrip = this.mzTrips.get(new IdImpl(key));
				
				//----------------------------------------------------------------------------------------
				
				Coord beforeShoppingCoord = new CoordImpl(
						Double.parseDouble(entries[38].trim()), Double.parseDouble(entries[39].trim()));
				Act beforeShoppingAct = new Act("start", beforeShoppingCoord);
				// in seconds after midnight
				double endTimeBeforeShoppingAct = 60.0 * Double.parseDouble(entries[12].trim());
				beforeShoppingAct.setEndTime(endTimeBeforeShoppingAct);
				
				Coord shoppingCoord= new CoordImpl(
						Double.parseDouble(entries[50].trim()), Double.parseDouble(entries[51].trim()));
				Act shoppingAct = new Act("shop", shoppingCoord);
				
				double startTimeShoppingAct = 60.0 * Double.parseDouble(entries[15].trim());
				shoppingAct.setStartTime(startTimeShoppingAct);
				double endTimeShoppingAct = mzTrip.getStartTime();
				shoppingAct.setEndTime(endTimeShoppingAct);
						
				Coord afterShoppingCoord = mzTrip.getCoord();
				Act afterShoppingAct = new Act("end", afterShoppingCoord);
				
				double startTimeAfterShoppingAct = mzTrip.getEndTime(); 			
				afterShoppingAct.setStartTime(startTimeAfterShoppingAct);
								
				Trip trip = new Trip(tripNr, beforeShoppingAct, shoppingAct, afterShoppingAct);
				
				ChoiceSet choiceSet = new ChoiceSet(new IdImpl(trip3ID), trip);
				double travelTimeBudget = (startTimeAfterShoppingAct- endTimeBeforeShoppingAct) - (endTimeShoppingAct - startTimeShoppingAct);
				choiceSet.setTravelTimeBudget(travelTimeBudget);
				this.choiceSets.add(choiceSet);			
			}
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	// add F58, F514 for after shopping act (E_X and E_Y)
	private void read0(String file) {
				
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {
								
				String[] entries = curr_line.split("\t", -1);
				
				String mode = entries[53].trim();
				
				if (!(mode.endsWith("Fuss") || mode.equals("Auto"))) continue;
				
				String HHNR = entries[0].trim();
				String ZIELPNR = entries[1].trim();
				if (ZIELPNR.length() == 1) ZIELPNR = "0" + ZIELPNR; 
				String tripNr = entries[2].trim();
				
				Id id = new IdImpl(HHNR + ZIELPNR + tripNr);
				
				Coord coord = new CoordImpl(
						Double.parseDouble(entries[30].trim()), Double.parseDouble(entries[31].trim()));
				
				double startTime = 60* Double.parseDouble(entries[5].trim());
				
				double endTime = startTime;
				if (entries[41].trim().length() > 0) {
					endTime = 60* Double.parseDouble(entries[41].trim());
				}
				else {
					log.info("No end time found for " +id);
				}
				
				MZTrip mzTrip = new MZTrip(id, coord, startTime, endTime);
				this.mzTrips.put(id, mzTrip);
			}
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
	}

}
