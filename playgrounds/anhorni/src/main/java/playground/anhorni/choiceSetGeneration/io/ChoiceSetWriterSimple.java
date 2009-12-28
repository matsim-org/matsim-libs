package playground.anhorni.choiceSetGeneration.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.PersonAttributes;
import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

public class ChoiceSetWriterSimple extends CSWriter {

	private final static Logger log = Logger.getLogger(ChoiceSetWriterSimple.class);
	private ZHFacilities facilities;
	
	public ChoiceSetWriterSimple(ZHFacilities facilities) {
		this.facilities = facilities;
	}
		
	@Override
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {	
		
		this.writeNumberOfAlternatives(outdir, name, choiceSets);
		
		outdir += "/choicesets/";
 		
		this.writeRoundTripIndetermediateStop(outdir, name, choiceSets, 0, false);
		this.writeRoundTripIndetermediateStop(outdir, name +"_RoundTrip", choiceSets, 1, false);
		this.writeRoundTripIndetermediateStop(outdir, name +"_IntermediateStop", choiceSets, 2, false);
		
		this.writeRoundTripIndetermediateStop(outdir, name, choiceSets, 0, true);
		this.writeRoundTripIndetermediateStop(outdir, name +"_RoundTrip", choiceSets, 1, true);
		this.writeRoundTripIndetermediateStop(outdir, name +"_IntermediateStop", choiceSets, 2, true);
	}
	
	/*
	 * tripKind: 	0: round trip or intermediate stop
	 * 				1: round trip
	 * 				2: intermediate stop
	 */
	private void writeRoundTripIndetermediateStop(String outdir, String name, List<ChoiceSet> choiceSets, 
			int tripKind, boolean imputed) {
				
		String outfile;
		if (imputed) {
			outfile = outdir + name + "_Imputed_ChoiceSets.txt";	
		}
		else {
			outfile = outdir + name + "_ChoiceSets.txt";
		}
		
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn("Choice sets not written");
			return;
		}
		
		String header = this.getHeader();
	
		try {								
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(header);
			out.newLine();	
						
			Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
			while (choiceSet_it.hasNext()) {
				ChoiceSet choiceSet = choiceSet_it.next();
				
				if (!(tripKind == 0)) {
					if (tripKind == 1 && !choiceSet.isRoundTrip()) continue;
					if (tripKind == 2 &&  choiceSet.isRoundTrip()) continue;
				}
				
				String id_WP = choiceSet.getId() +"\t" + choiceSet.getPersonAttributes().getWP() + "\t";
				String choice = null;
				
				PersonAttributes attributes = choiceSet.getPersonAttributes();
				String outLine = attributes.getAge() + "\t" + attributes.getGender() + "\t";
				
				if (attributes.getIncomeHH() < 0.0 && imputed) {
					outLine += "3.78\t";
				}
				else {
					outLine += attributes.getIncomeHH() + "\t";
				}
				
				if (attributes.getNumberOfPersonsHH() < 0 && imputed) {
					
					int age = attributes.getAge();					
					if (age >= 0 && age <=18) {
						outLine += "4.14\t";
					}
					else if (age >= 19 && age <= 30) {
 						outLine += "2.74\t";
					}
					else if (age >= 31 && age <= 49){
						outLine += "2.89\t";
					}
					else if (age >= 50) {
						outLine += "1.96\t";
					}
					else {
						outLine += "2.64\t";
					}
				}
				else {
					outLine += attributes.getNumberOfPersonsHH() + "\t";
				}
				
				outLine += choiceSet.getTrip().getShoppingAct().getStartTime() +"\t" + attributes.getStart_is_home() +"\t";
				outLine += choiceSet.getTravelTimeBudget() +"\t";
								
				int index = 0;
				Iterator<ZHFacility> facilities_it = this.facilities.getZhFacilities().values().iterator();
				while (facilities_it.hasNext()) {
					ZHFacility facility = facilities_it.next();	
					outLine += this.printFacility(facility, choiceSet);				
					if (facility.getId().compareTo(choiceSet.getChosenFacilityId()) == 0 ) {
						choice = Integer.toString(index) + "\t";
					}
					index++;
				}
				
				out.write(id_WP + choice + outLine);
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
		
		String outLine = facility.getId().toString()+"\t";
		
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
			
			outLine += choiceSet.getFacilities().get(facility.getId()).getAdditionalTime() +"\t";
			outLine += choiceSet.getFacilities().get(facility.getId()).getAdditionalDistance() +"\t";			
		}
		else {
			outLine += "-1\t-1\t-1\t-1\t-1\t-1\t";
		}
		
		outLine += facility.getRetailerID() + "\t" +
			facility.getSize_descr() +"\t" +
			facility.getDHalt() + "\t";	
			
		outLine += facility.getAccessibility02()+ "\t" + facility.getAccessibility10()+ "\t" + facility.getAccessibility20()+"\t";
		outLine += facility.getHrs_week() +"\t";
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
	
	private String getHeader() {
		String header="Id\t" +
		"WP\tChoice\tAge\tGender\tIncome\tNbrPersHH\tTpurchase\tstart_is_home\tTTB\t" ;

		for (int i = 0; i < this.facilities.getZhFacilities().size(); i++) {
			header += "SH" + i + "_Shop_id\t" +
					"SH" + i + "_AV\t" +
					"SH" + i + "_Mapped_x\t" + "SH" + i + "_Mapped_y\t" +
					"SH" + i + "_Exact_x\t" + "SH" + i + "_Exact_y\t" +
					"SH" + i + "_TTnet\t" + 
					"SH" + i + "_TDnet\t" +
					"SH" + i + "_CFD_exact\t" + "SH" + i + "_CFD_mapped\t" +
					"SH" + i + "_addTime\t" +"SH" + i + "_addDistance\t" +
					"SH" + i + "_RetailerID\t" +
					"SH" + i + "_Size\t" +
					"SH" + i + "_dHalt\t" +
					"SH" + i + "_acc02\t" +
					"SH" + i + "_acc10\t" +
					"SH" + i + "_acc20\t" +
					"SH" + i + "_HRS_WEEK\t";
		}	
		return header;
	}
}
