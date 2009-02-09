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

public class ChoiceSetWriterSimple extends CSWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriterSimple.class);
	private String mode;
	private String crowFly;
	
	public ChoiceSetWriterSimple(String mode, String crowFly) {
		this.mode = mode;
		this.crowFly = crowFly;
	}
	
	public void write(String outdir, String name,List<ChoiceSet> choiceSets)  {
		
		String outfile = outdir + name + "_ChoiceSets.txt";
		String outfile_alternatives = outdir + name + "_NumberOfAlternativesInclusive.txt";
		
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn(outfile +" not created");
			return;
		}
		
		try {		
			final String header="Id\tTTB (s)\tShop_id\tLink_x\tLink_y\tExact_x\tExact_y\tTravel_Time (s) in net\tTravel_Distance in net (m)\tCrow fly distance (m) exact\tCrow fly distance (m) mapped\tChosen";
						
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			final BufferedWriter out_alternatives = IOUtils.getBufferedWriter(outfile_alternatives);
			out.write(header);
			out.newLine();
			out_alternatives.write("Id\tNumber of alternatives (includes the chosen facility)");
			out_alternatives.newLine();			
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				
				boolean oneIsChosen = false;
				Iterator<ZHFacility> fac_it = choiceSet.getFacilities().iterator();
				while (fac_it.hasNext()) {
					ZHFacility facility = fac_it.next();
					
					String location;
					String chosen;
					if (facility.getId().compareTo(choiceSet.getChosenZHFacility().getId()) == 0) {
						chosen = "1";
						oneIsChosen = true;
					}
					else {
						chosen = "0";
					}
					
					double crowFlyDistanceMapped = choiceSet.getTrip().getBeforeShoppingAct().getCoord().
						calcDistance(facility.getMappedposition()) +
						choiceSet.getTrip().getAfterShoppingAct().getCoord().calcDistance(facility.getMappedposition());
					
					double crowFlyDistanceExact = choiceSet.getTrip().getBeforeShoppingAct().getCoord().
						calcDistance(facility.getExactPosition()) +
						choiceSet.getTrip().getAfterShoppingAct().getCoord().calcDistance(facility.getMappedposition());
					
					location = facility.getId() + "\t" + facility.getMappedposition().getX() +"\t" + 
						facility.getMappedposition().getY()+  "\t" + facility.getExactPosition().getX() + "\t" +
						facility.getExactPosition().getY();
					
					if (this.crowFly.equals("true") && this.mode.equals("walk")) {
						location += "\t" + "-" + "\t" + "-";
					}
					else {
						location += "\t" + choiceSet.getTravelTime(facility) + "\t" +
							choiceSet.getTravelDistance(facility);
					}
					location +=	"\t" + crowFlyDistanceExact + "\t" +
						crowFlyDistanceMapped + "\t" +
						chosen;
					
					out.write(choiceSet.getId() +"\t" + 
							choiceSet.getTravelTimeBudget() + "\t" +
							location);
					out.newLine();
					
					
				}
				out.flush();
				out_alternatives.write(choiceSet.getId() + "\t" + choiceSet.getFacilities().size());
				out_alternatives.newLine();
				out_alternatives.flush();
				
				if (!oneIsChosen) {
					log.error("Problem with choice set " + choiceSet.getId());
				}
			}
			out.flush();
			out.close();
			out_alternatives.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}
}
