package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.population.Act;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.Trip;


/*	  0			1		2			3			4			5		6		7			8				9
 * ---------------------------------------------------------------------------------------------------------		
 *0	| ...
 * ---------------------------------------------------------------------------------------------------------
 */


public class NelsonTripReader {
	
	private List<ChoiceSet> choiceSets = new Vector<ChoiceSet>();
	private final static Logger log = Logger.getLogger(NelsonTripReader.class);
		
	public NelsonTripReader() {
	}
	
	public List<ChoiceSet> readFile(final String file, String mode)  {
				
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
						
			while ((curr_line = bufferedReader.readLine()) != null) {	
				String[] entries = curr_line.split("\t", -1);
				
				String recordID = entries[0].trim();
				// TODO:
				// Passed last line. Why not captured in null test?
				if (recordID.length()==0) break;
				
				Id personID = new IdImpl(recordID.substring(0, recordID.length()-2));
				
				int tripNr = Integer.parseInt(entries[12].trim());
				
				Coord beforeShoppingCoord = new CoordImpl(
						Double.parseDouble(entries[4].trim()), Double.parseDouble(entries[5].trim()));
				Act beforeShoppingAct = new Act("h", beforeShoppingCoord);
				
				beforeShoppingAct.setEndTime(60.0 * Double.parseDouble(entries[19].trim()));
				
				Coord shoppingCoord= new CoordImpl(
						Double.parseDouble(entries[6].trim()), Double.parseDouble(entries[7].trim()));
				Act shoppingAct = new Act("s", shoppingCoord);
				shoppingAct.setStartTime(60.0 * Double.parseDouble(entries[20].trim()));
				
				Coord afterShoppingCoord = new CoordImpl(
						Double.parseDouble(entries[15].trim()), Double.parseDouble(entries[16].trim()));
				Act afterShoppingAct = new Act("w", afterShoppingCoord);
								
				Trip trip = new Trip(tripNr, beforeShoppingAct, shoppingAct, afterShoppingAct);
				
				ChoiceSet choiceSet = new ChoiceSet(personID, trip);
				Double travelTimeBudget = 60.0 * Double.parseDouble(entries[3].trim());
				choiceSet.setTravelTimeBudget(travelTimeBudget);
				this.choiceSets.add(choiceSet);			
			}
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info("Number of " + mode + " trips : " + this.choiceSets.size());
		return this.choiceSets;
	}

}
