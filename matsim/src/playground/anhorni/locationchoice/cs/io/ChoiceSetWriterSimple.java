package playground.anhorni.locationchoice.cs.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.PersonAttributes;
import playground.anhorni.locationchoice.cs.helper.ZHFacilities;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public class ChoiceSetWriterSimple extends CSWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriterSimple.class);
	private ZHFacilities facilities;
	
	public ChoiceSetWriterSimple(ZHFacilities facilities) {
		this.facilities = facilities;
	}
	
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		
		this.writeNumberOfAlternatives(outdir, name, choiceSets);
	
		String outfile = outdir + name + "_ChoiceSets.txt";	
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn(outfile +" not created");
			return;
		}
		
		String header="Id\t" +
		"WP\tChoice\tAge\tGender\tIncome\tNumber_of_personsHH\tCivil_Status\tEducation\tTime_of_purchase\tstart_is_home\tTTB\t" ;

		for (int i = 0; i < this.facilities.getZhFacilities().size(); i++) {
			header += "SH" + i + "_Shop_id\t " +
					"SH" + i + "_AV\t" +
					"SH" + i + "_Mapped_x\t" + "SH" + i + "_Mapped_y\t" +
					"SH" + i + "_Exact_x\t" + "SH" + i + "_Exact_y\t" +
					"SH" + i + "_Travel_time_in_Net\t" + 
					"SH" + i + "_Travel_distance_in_net\t" +
					"SH" + i + "_Crow_fly_distance_exact\t" + "SH" + i + "_Crow_fly_distance_mapped\t" +
					"SH" + i + "RetailerID\t" +
					"SH" + i + "Size\t" +
					"SH" + i + "dHalt\t" +
					"SH" + i + "aAlt02\t" +
					"SH" + i + "aAlt10\t" +
					"SH" + i + "aAlt20\t" +
					"SH" + i + "HRS_WEEK\t";
		}
	
		try {								
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();		
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();

				String outLine = choiceSet.getId() +"\t" + choiceSet.getPersonAttributes().getWP() +"\t";
				
				// chosen facility is always 1st alternative in choice set
				String choice = "1\t";
				outLine += choice;
				
				PersonAttributes attributes = choiceSet.getPersonAttributes();
				outLine += attributes.getAge() +"\t"+ attributes.getGender() +"\t" + attributes.getIncomeHH() +"\t"+ 
				attributes.getNumberOfPersonsHH() +"\t" + attributes.getCivilStatus() +"\t" + attributes.getEducation() +"\t";
				
				outLine += choiceSet.getTrip().getShoppingAct().getStartTime() +"\t" + attributes.getStart_is_home() +"\t";
				outLine += choiceSet.getTravelTimeBudget() +"\t";
				
				// chosen facility: ------------------------------------------------------------
				outLine += this.printFacility(choiceSet.getChosenFacility().getFacility(), choiceSet);
								
				Iterator<ZHFacility> facilities_it = this.facilities.getZhFacilities().values().iterator();
				while (facilities_it.hasNext()) {
					ZHFacility facility = facilities_it.next();	
					outLine += this.printFacility(facility, choiceSet);
				}
				out.write(outLine);
				out.newLine();
				out.flush();
			}
			out.flush();			
			out.flush();
			out.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}
	
	
	private String printFacility(ZHFacility facility, ChoiceSet choiceSet) {
		
		String outLine = facility.getId().toString();
		
		//AV
		if (choiceSet.zhFacilityIsInChoiceSet(facility.getId())) {
			outLine += "1\t";
		}
		else {
			outLine += "0\t";
		}

		outLine += 
			facility.getMappedPosition().getX() + "\t" + 
			facility.getMappedPosition().getY()	+ "\t" + 
			facility.getExactPosition().getX() 	+ "\t" +
			facility.getExactPosition().getY()	+ "\t";
		
		double crowFlyDistanceMapped = choiceSet.calculateCrowFlyDistanceMapped(facility.getMappedPosition());
		double crowFlyDistanceExact = choiceSet.calculateCrowFlyDistanceExact(facility.getExactPosition());
		
		if (choiceSet.zhFacilityIsInChoiceSet(facility.getId())) {
			outLine += choiceSet.getTravelTimeStartShopEnd(facility.getId()) + "\t" +
				choiceSet.getTravelDistanceStartShopEnd(facility.getId()) +"\t" +
				crowFlyDistanceExact +"\t" +
				crowFlyDistanceMapped +"\t";
		}
		else {
			outLine += "-99\t-99\t-99\t-99\t";
		}
		
		outLine += facility.getRetailerID() + "\t" +
			facility.getSize_descr() +"\t" +
			facility.getDHalt() + "\t";	
		
		
		outLine += "aAlt02\taAlt10\taAlt20\t";
		return outLine;
	}
	
	
	private void writeNumberOfAlternatives(String outdir, String name,List<ChoiceSet> choiceSets)  {
		
		String outfile_alternatives = outdir + name + "_NumberOfAlternativesInclusive.txt";
		
		try {		
			final BufferedWriter out_alternatives = IOUtils.getBufferedWriter(outfile_alternatives);
			out_alternatives.write("Id\tNumber of alternatives (includes the chosen facility)");
			out_alternatives.newLine();			
			
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				out_alternatives.write(choiceSet.getId() + "\t" + choiceSet.getFacilities().size());
				out_alternatives.newLine();
				out_alternatives.flush();
			}
			out_alternatives.close();
						
		} catch (final IOException e) {
				Gbl.errorMsg(e);
		}	
	}	
}
