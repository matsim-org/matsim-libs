package playground.anhorni.choiceSetGeneration.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;


public class NelsonTripWriter {

public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + name +"_Trips.txt";
		
		try {		
			final String header="Id\tTTB (s)\t" +
					"Prior_X\tPrior_Y\tt_End_Prior (s after midnight)\t" +
					"Shop_X\tShop_Y\tt_Start_Shop (s after midnight)\tt_End_Shop (s after midnight)\t" +
					"Posterior_X\tPosterior_Y\ttStart_Posterior (s after midnight)";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				out.write(choiceSet.getId() + "\t" + 
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
