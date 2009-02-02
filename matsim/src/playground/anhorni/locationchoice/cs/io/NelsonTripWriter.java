package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;


public class NelsonTripWriter {

public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + name +"_Trips.txt";
		
		try {		
			final String header="Id\tTrip_nr\tTTB (s)\tS_X\tS_Y\tEnd_S\tZ_X\tZ_Y\tStart_Z\tEnd_Z\tE_X\tE_Y\tStart_E";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				out.write(choiceSet.getId() + "\t" + 
						choiceSet.getTrip().getTripNr() + "\t" + 
						choiceSet.getTravelTimeBudget() + "\t" +
						choiceSet.getTrip().getBeforeShoppingAct().getCoord().getX() + "\t" +
						choiceSet.getTrip().getBeforeShoppingAct().getCoord().getY() + "\t" +
						choiceSet.getTrip().getBeforeShoppingAct().getEndTime() + "\t" +
						choiceSet.getTrip().getShoppingAct().getCoord().getX() + "\t" +
						choiceSet.getTrip().getShoppingAct().getCoord().getY() + "\t" +
						choiceSet.getTrip().getShoppingAct().getStartTime() + "\t" +
						choiceSet.getTrip().getShoppingAct().getEndTime() + "\t" +
						choiceSet.getTrip().getAfterShoppingAct().getCoord().getX() + "\t" +
						choiceSet.getTrip().getAfterShoppingAct().getCoord().getY() + "\t" +
						choiceSet.getTrip().getAfterShoppingAct().getStartTime()				);
				out.newLine();
				out.flush();
			}					
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}
	
}
