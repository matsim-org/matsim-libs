package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;

public class TripStats extends CSWriter{
		
	public TripStats(String mode) {
		super();
	}

	public void write(String outdir, String mode, List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + mode +"_TripStats.txt";
				
		try {

			final String header ="Id\tTravel distance in net (m)\tcrow fly distance exact (m)\tTravel time in net (s)\tTravel speed in net (km/h)\tTTB (s)\tUsed Buget (%)";
			
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				
				double travelDistance = choiceSet.getTravelDistanceStartShopEnd(choiceSet.getChosenFacilityId());
				double travelTime = choiceSet.getTravelTimeStartShopEnd(choiceSet.getChosenFacilityId());
				
				double travelSpeed = 3.6 * travelDistance / travelTime;
				double usedBudgetPercent = 100.0 * travelTime / choiceSet.getTravelTimeBudget();
				
				double crowFlyDistance = choiceSet.getTrip().getBeforeShoppingAct().getCoord().
					calcDistance(choiceSet.getChosenFacility().getFacility().getExactPosition()) +
					choiceSet.getTrip().getAfterShoppingAct().getCoord().
					calcDistance(choiceSet.getChosenFacility().getFacility().getExactPosition());
				
				String location = choiceSet.getId().toString();
				
				location += "\t" + travelDistance + "\t" + crowFlyDistance + "\t" + travelTime + "\t" + travelSpeed;
				location += "\t" + choiceSet.getTravelTimeBudget() + "\t" + usedBudgetPercent;
				
				out.write(location);
				out.newLine();
				out.flush();
			}					
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}

}
