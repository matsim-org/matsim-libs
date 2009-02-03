package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public class ChoiceSetWriterSimple extends ChoiceSetWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriterSimple.class);
	
	public ChoiceSetWriterSimple() {
	}
	
	public void write(String outdir, String name,List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + name + "_ChoiceSets.txt";
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn(outfile +" not created");
			return;
		}
		
		try {		
			final String header="id\tTrip_nr\tTTB (s)\tShop_id\tLink_x\tLink_y\tExact_x\tExact_y\tTravel_Time (s)\tTravel_Distance (m)\tChosen";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				
				String location;
				Iterator<ZHFacility> fac_it = choiceSet.getFacilities().iterator();
				while (fac_it.hasNext()) {
					ZHFacility facility = fac_it.next();
					
					String chosen;
					if (facility.getId().compareTo(choiceSet.getTrip().getShoppingAct().getFacilityId()) == 0) {
						chosen = "1";
					}
					else {
						chosen = "0";
					}
										
					location = facility.getId() + "\t" + facility.getCenter().getX() +"\t" + 
						facility.getCenter().getY()+  "\t" + facility.getExactPosition().getX() + "\t" +
						facility.getExactPosition().getY() + "\t" +
						choiceSet.getTravelTime(facility) + "\t" +
						choiceSet.getTravelDistance(facility) + "\t" +
						chosen;
					
					out.write(choiceSet.getId() +"\t" + 
							choiceSet.getTrip().getTripNr() + "\t" + 
							choiceSet.getTravelTimeBudget() + "\t" +
							location);
					out.newLine();
				}					
				out.flush();
			}
			out.flush();
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}
}
