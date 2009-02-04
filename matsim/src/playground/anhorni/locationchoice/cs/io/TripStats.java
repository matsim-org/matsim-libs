package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;

public class TripStats extends CSWriter{
	
public void write(String outdir, String mode, List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + mode +"_TripStats.txt";
		
		try {		
			final String header="Id\tTravel distance (m)\tTravel time (s)\tTravel speed (km/h)\tTTB (s)\tUsed Buget (%)";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				
				double travelDistance = choiceSet.getTravelDistance(choiceSet.getChosenZHFacility());
				double travelTime = choiceSet.getTravelTime(choiceSet.getChosenZHFacility());
				
				double travelSpeed = 3.6 * travelDistance / travelTime;
				double usedBudgetPercent = 100.0 * travelTime / choiceSet.getTravelTimeBudget();
				
				out.write(choiceSet.getId() + "\t" + 
						travelDistance + "\t" + 
						travelTime + "\t" + 
						travelSpeed + "\t" + 
						choiceSet.getTravelTimeBudget() + "\t" +
						usedBudgetPercent);
				out.newLine();
				out.flush();
			}					
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}

}
